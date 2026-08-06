import 'dart:async';
import 'dart:convert';

import 'package:dio/dio.dart';

import '../config/app_config.dart';
import '../storage/token_storage.dart';
import 'api_exception.dart';

/// Central HTTP client for the NovaWallet backend.
///
/// Responsibilities:
///  * Base URL / timeouts from [AppConfig].
///  * Attaches the Bearer access token to every request.
///  * On 401: performs a **single-flight** refresh-token rotation and retries
///    the original request once. If refresh fails, clears the session so the
///    router can bounce the user to login.
///  * Attaches an `Idempotency-Key` header to mutating requests (POST/PUT/
///    PATCH/DELETE) so the backend can dedupe replays.
///  * Unwraps the backend `ApiResponse<T>` envelope and normalizes failures
///    into [ApiException].
///
/// Usage:
/// ```dart
/// final api = ref.read(apiClientProvider);
/// final data = await api.post<WalletResponse>('/v1/wallets/me');
/// ```
class ApiClient {
  /// Creates the client with the given [dio] transport and [storage] for
  /// token persistence.
  ApiClient({required Dio dio, required TokenStorage storage})
      : this._(dio, storage);

  ApiClient._(this._dio, this._storage) {
    _dio.options.baseUrl = AppConfig.apiBaseUrl;
    _dio.options
      ..connectTimeout = const Duration(seconds: 15)
      ..receiveTimeout = const Duration(seconds: 30)
      ..sendTimeout = const Duration(seconds: 30)
      ..headers['Accept'] = 'application/json';

    _dio.interceptors
      ..add(_AuthInterceptor(_storage, refresh))
      ..add(_ErrorInterceptor());
  }

  final Dio _dio;
  final TokenStorage _storage;

  /// Serializes concurrent 401-triggered refreshes into a single flight.
  Future<void>? _refreshInFlight;

  /// True while a refresh is running; guards against logout-during-refresh.
  bool _refreshing = false;

  // ---------------------------------------------------------------------------
  // Public API
  // ---------------------------------------------------------------------------

  /// GET with envelope unwrapping. [parser] converts `data` into a model.
  Future<T?> get<T>(
    String path, {
    Map<String, dynamic>? query,
    T Function(dynamic json)? parser,
  }) async {
    final res = await _guard(
      () => _dio.get<Map<String, dynamic>>(
        path,
        queryParameters: query,
      ),
    );
    return _unwrap(res.data, parser);
  }

  /// POST with envelope unwrapping and idempotency protection.
  Future<T?> post<T>(
    String path, {
    Object? body,
    Map<String, dynamic>? query,
    T Function(dynamic json)? parser,
    String? idempotencyKey,
  }) async {
    final res = await _guard(
      () => _dio.post<Map<String, dynamic>>(
        path,
        data: body,
        queryParameters: query,
        options: Options(headers: _idempotencyHeaders(idempotencyKey)),
      ),
    );
    return _unwrap(res.data, parser);
  }

  /// PUT with envelope unwrapping and idempotency protection.
  Future<T?> put<T>(
    String path, {
    Object? body,
    T Function(dynamic json)? parser,
    String? idempotencyKey,
  }) async {
    final res = await _guard(
      () => _dio.put<Map<String, dynamic>>(
        path,
        data: body,
        options: Options(headers: _idempotencyHeaders(idempotencyKey)),
      ),
    );
    return _unwrap(res.data, parser);
  }

  /// PATCH with envelope unwrapping and idempotency protection.
  Future<T?> patch<T>(
    String path, {
    Object? body,
    T Function(dynamic json)? parser,
    String? idempotencyKey,
  }) async {
    final res = await _guard(
      () => _dio.patch<Map<String, dynamic>>(
        path,
        data: body,
        options: Options(headers: _idempotencyHeaders(idempotencyKey)),
      ),
    );
    return _unwrap(res.data, parser);
  }

  /// DELETE with envelope unwrapping and idempotency protection.
  Future<T?> delete<T>(
    String path, {
    T Function(dynamic json)? parser,
    String? idempotencyKey,
  }) async {
    final res = await _guard(
      () => _dio.delete<Map<String, dynamic>>(
        path,
        options: Options(headers: _idempotencyHeaders(idempotencyKey)),
      ),
    );
    return _unwrap(res.data, parser);
  }

  /// Multipart upload (KYC documents, avatars). [fields] are form fields,
  /// [file] is the binary payload under [fileFieldName].
  Future<T?> postMultipart<T>(
    String path, {
    required MultipartFile file,
    required String fileFieldName,
    Map<String, dynamic>? fields,
    T Function(dynamic json)? parser,
  }) async {
    final form = FormData.fromMap({
      fileFieldName: file,
      ...?fields,
    });
    final res = await _guard(
      () => _dio.post<Map<String, dynamic>>(
        path,
        data: form,
        options: Options(headers: {'Content-Type': 'multipart/form-data'}),
      ),
    );
    return _unwrap(res.data, parser);
  }

  /// Downloads a raw byte payload (e.g. KYC document files).
  Future<List<int>> getBytes(String path) async {
    final res = await _guard(
      () => _dio.get<List<int>>(
        path,
        options: Options(responseType: ResponseType.bytes),
      ),
    );
    return res.data ?? const [];
  }

  /// Refresh-token rotation, single-flight. Used by the auth interceptor.
  ///
  /// Mirrors backend semantics:
  ///  * refresh token goes in `Authorization: Bearer <refreshToken>`
  ///  * response is `ApiResponse<AuthResponse>` with fresh access+refresh
  ///  * a rotated (reused) refresh token invalidates the whole family — so on
  ///    failure we always clear the session.
  Future<bool> refresh() async {
    if (_refreshing) {
      // Another call is already refreshing — wait for it.
      await _refreshInFlight;
      return true;
    }
    final token = await _storage.refreshToken;
    if (token == null) return false;

    final completer = Completer<bool>();
    _refreshInFlight = completer.future;
    _refreshing = true;
    try {
      final res = await _dio.post<Map<String, dynamic>>(
        '/v1/auth/refresh',
        options: Options(headers: {'Authorization': 'Bearer $token'}),
      );
      final data = res.data;
      final ok = data?['success'] == true;
      if (ok && data?['data'] != null) {
        final auth = data!['data'] as Map<String, dynamic>;
        await _storage.saveSession(
          accessToken: auth['accessToken'] as String? ?? '',
          refreshToken: auth['refreshToken'] as String? ?? '',
          userJson: auth['user'] == null ? null : jsonEncode(auth['user']),
        );
        completer.complete(true);
        return true;
      }
      await _storage.clear();
      completer.complete(false);
      return false;
    } on DioException catch (e) {
      // Network failure mid-refresh: keep tokens, let the caller retry later.
      if (e.type == DioExceptionType.connectionTimeout ||
          e.type == DioExceptionType.connectionError ||
          e.type == DioExceptionType.receiveTimeout) {
        completer.complete(false);
        return false;
      }
      // 401/403 from refresh = token family is dead.
      await _storage.clear();
      completer.complete(false);
      return false;
    } finally {
      _refreshing = false;
      _refreshInFlight = null;
    }
  }

  /// Removes any stored session (logout / hard failure).
  Future<void> clearSession() => _storage.clear();

  // ---------------------------------------------------------------------------
  // Internals
  // ---------------------------------------------------------------------------

  /// Unwraps `{"success", "message", "data"}` or throws [ApiException].
  T? _unwrap<T>(Map<String, dynamic>? body, T Function(dynamic json)? parser) {
    if (body == null) return null;
    if (body['success'] != true) {
      throw ApiException(
        message: body['message'] as String? ?? 'Request failed.',
        details: body['details'],
      );
    }
    final data = body['data'];
    return data == null || parser == null ? null : parser(data);
  }

  /// Runs a Dio call and re-throws the normalized [ApiException] that
  /// [_ErrorInterceptor] stashed on `DioException.error`, so the rest of the
  /// app can `on ApiException catch` directly.
  Future<T> _guard<T>(Future<T> Function() run) async {
    try {
      return await run();
    } on DioException catch (e) {
      final normalized = e.error;
      if (normalized is ApiException) throw normalized;
      rethrow;
    }
  }

  Map<String, String> _idempotencyHeaders(String? providedKey) {
    if (providedKey == null) return const {};
    return {'Idempotency-Key': providedKey};
  }
}

/// Adds `Authorization: Bearer <token>`; on 401 tries refresh-once-then-retry.
class _AuthInterceptor extends Interceptor {
  _AuthInterceptor(this._storage, this._refresh);

  final TokenStorage _storage;
  final Future<bool> Function() _refresh;

  /// Set of URLs that must never receive an auth header (login/refresh).
  static const _anonymousPaths = {'/v1/auth/login', '/v1/auth/refresh'};

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final path = options.path;
    if (_anonymousPaths.contains(path)) {
      handler.next(options);
      return;
    }
    final token = await _storage.accessToken;
    if (token != null && token.isNotEmpty) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    // Flag stored on RequestOptions.extra to detect retry loops.
    final alreadyRetried = err.requestOptions.extra['auth_retried'] == true;
    if (err.response?.statusCode == 401 && !alreadyRetried) {
      final ok = await _refresh();
      if (ok) {
        final options = err.requestOptions;
        options.extra['auth_retried'] = true; // prevent infinite retry loops
        final token = await _storage.accessToken;
        options.headers['Authorization'] = 'Bearer $token';
        try {
          final res = await Dio().fetch<dynamic>(options);
          handler.resolve(res);
          return;
        } on DioException catch (retryErr) {
          handler.next(retryErr);
          return;
        }
      }
    }
    handler.next(err);
  }
}

/// Maps raw [DioException]s onto [ApiException] for the rest of the app.
class _ErrorInterceptor extends Interceptor {
  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    final res = err.response;
    if (res == null) {
      handler.next(
        err.copyWith(
          error: ApiException(message: err.message ?? 'Network error'),
        ),
      );
      return;
    }
    // Backend envelope or plain message.
    final body = res.data;
    final message = body is Map<String, dynamic>
        ? (body['message'] as String? ?? 'Request failed.')
        : (body?.toString() ?? 'Request failed.');
    handler.next(
      err.copyWith(
        error: ApiException(
          message: message,
          statusCode: res.statusCode,
          details: body is Map<String, dynamic> ? body['details'] : null,
        ),
      ),
    );
  }
}

