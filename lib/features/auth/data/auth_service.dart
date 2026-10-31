/*
 * NovaWallet authentication service.
 *
 * Calls the verified Spring Boot `novawallet-api` login contract:
 *   POST {baseUrl}/api/v1/auth/login
 *   Headers: Content-Type: application/json; charset=UTF-8
 *   Body: {"email": String, "password": String}
 *
 * Success (200): parses the `ApiResponse<AuthResponse>` envelope and returns
 * an [AuthSession] containing the access token, refresh token, and user
 * details.
 * Failures map to [AuthException] with a user-safe message that never
 * exposes backend internals:
 *   401 → invalid credentials
 *   429 → rate-limited / temporarily locked
 *   network failure → connection problem
 *   anything else → generic unexpected error
 *
 * Tokens and credentials are never logged. Sessions live in memory only;
 * secure token storage is pending (see TODO.md).
 */
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import 'package:novawallet_mobile/core/config/app_config.dart';

import 'auth_models.dart';

/// The category of an authentication failure, used to pick a user-safe message.
enum AuthErrorType {
  invalidCredentials,
  rateLimited,
  network,
  server,
  duplicateAccount,
  validation,
  sessionExpired,
  unexpected,
}

/// Thrown by [AuthService] implementations when login cannot complete.
///
/// Never construct this with raw backend payloads — use
/// [AuthException.fromResponse] so user-visible text stays safe.
class AuthException implements Exception {
  const AuthException({
    required this.type,
    required this.userMessage,
    this.statusCode,
  });

  /// What went wrong, for programmatic handling.
  final AuthErrorType type;

  /// Safe, user-facing explanation. Never contains tokens or backend details.
  final String userMessage;

  /// HTTP status code when the failure originated from an HTTP response.
  final int? statusCode;

  @override
  String toString() => 'AuthException($type): $userMessage';

  /// Builds an [AuthException] from an HTTP error response.
  factory AuthException.fromResponse(http.Response response) {
    final int status = response.statusCode;
    String body;
    try {
      body = utf8.decode(response.bodyBytes);
    } on FormatException {
      body = response.body;
    }

    String? serverMessage;
    Map<String, dynamic>? errorsMap;
    try {
      final decoded = jsonDecode(body) as Map<String, dynamic>?;
      serverMessage =
          (decoded?['message'] as String?) ?? decoded?['code'] as String?;
      if (decoded?['errors'] is Map<String, dynamic>) {
        errorsMap = decoded!['errors'] as Map<String, dynamic>;
      }
    } on FormatException {
      // Body isn't JSON — fall back to the generic message below.
    } catch (_) {
      // Ignore parsing failures; fall back to generic message.
    }

    switch (status) {
      case 401:
        return const AuthException(
          type: AuthErrorType.invalidCredentials,
          userMessage: 'Incorrect email or password. Please check your details and try again.',
          statusCode: 401,
        );
      case 429:
        return const AuthException(
          type: AuthErrorType.rateLimited,
          userMessage: 'Too many failed attempts. Your account is temporarily locked — try again in 15 minutes.',
          statusCode: 429,
        );
      case 409:
        return AuthException(
          type: AuthErrorType.duplicateAccount,
          userMessage: serverMessage != null && serverMessage.isNotEmpty
              ? serverMessage
              : 'An account with these details already exists.',
          statusCode: 409,
        );
      case 400:
        // Prefer field-level validation messages when the backend provides them.
        String validationMessage = 'Please check your details and try again.';
        if (errorsMap != null && errorsMap.isNotEmpty) {
          validationMessage = errorsMap.values
              .map((value) => '$value')
              .where((value) => value.isNotEmpty)
              .join(' ');
        } else if (serverMessage != null && serverMessage.isNotEmpty) {
          validationMessage = serverMessage;
        }
        return AuthException(
          type: AuthErrorType.validation,
          userMessage: validationMessage,
          statusCode: 400,
        );
      case >= 500:
        return AuthException(
          type: AuthErrorType.server,
          userMessage: 'NovaWallet servers are temporarily unavailable. Please try again later.',
          statusCode: status,
        );
      default:
        return AuthException(
          type: AuthErrorType.unexpected,
          userMessage: serverMessage != null && serverMessage.isNotEmpty
              ? '$serverMessage.'
              : 'Unable to sign in. Please try again.',
          statusCode: status,
        );
    }
  }

  /// Builds an [AuthException] from a network-level failure.
  factory AuthException.fromError(Object error, StackTrace stackTrace) {
    debugPrintStack(stackTrace: stackTrace, label: 'auth_service error');
    return const AuthException(
      type: AuthErrorType.network,
      userMessage: "Can't reach NovaWallet servers. Check your connection and try again.",
    );
  }
}

/// Contract for authenticating a user against NovaWallet's backend.
///
/// Prefer [HttpAuthService] in production. Inject a test double in widget
/// tests instead of pointing at a live server.
abstract class AuthService {
  /// Signs in with [email] and [password], returning the authenticated
  /// session on success.
  ///
  /// Throws [AuthException] on validation, network, or API failure.
  Future<AuthSession> login({required String email, required String password});

  /// Registers a new account with the given details, returning the
  /// authenticated session on success.
  ///
  /// Throws [AuthException] on validation, network, or API failure.
  Future<AuthSession> register({
    required String firstName,
    required String lastName,
    required String email,
    required String phone,
    required String password,
  });

  /// Verifies an email address using the token from the verification link.
  ///
  /// Throws [AuthException] on invalid/expired token, network, or API failure.
  Future<void> verifyEmail({required String token});

  /// Refreshes the session using a refresh token.
  ///
  /// Throws [AuthException] on invalid/expired token, network, or API failure.
  Future<AuthSession> refresh({required String refreshToken});
}

/// HTTP-backed [AuthService] backed by [AppConfig.authBaseUrl].
class HttpAuthService implements AuthService {
  /// Creates an [HttpAuthService] pointing at [baseUrl] (defaults to
  /// `AppConfig.authBaseUrl`). Inject [client] to make the service testable
  /// without a live server.
  HttpAuthService({String? baseUrl, String? emailBaseUrl, http.Client? client})
    : baseUrl = baseUrl ?? AppConfig.authBaseUrl,
      _emailBaseUrl = emailBaseUrl ?? AppConfig.emailBaseUrl,
      _client = client ?? http.Client();

  /// Backend base URL for auth endpoints.
  final String baseUrl;

  /// Backend base URL for email-verification endpoints.
  final String _emailBaseUrl;

  final http.Client _client;

  static const Duration _timeout = Duration(seconds: 15);
  static const String _loginPath = '/login';

  /// The most recently obtained session, held in memory only.
  ///
  /// Null until a successful login completes. Persistent, encrypted storage
  /// is not yet implemented — see TODO.md for the boundary.
  AuthSession? lastSession;

  @override
  Future<AuthSession> login({
    required String email,
    required String password,
  }) async {
    final Uri uri = Uri.parse('$baseUrl$_loginPath');

    http.Response response;
    try {
      response = await _client
          .post(
            uri,
            headers: <String, String>{
              'Content-Type': 'application/json; charset=UTF-8',
              'Accept': 'application/json',
            },
            body: jsonEncode(<String, String>{
              'email': email.trim(),
              'password': password,
            }),
          )
          .timeout(_timeout);
    } on http.ClientException catch (error, stackTrace) {
      throw AuthException.fromError(error, stackTrace);
    } on FormatException catch (error, stackTrace) {
      throw AuthException.fromError(error, stackTrace);
    } on Exception catch (error, stackTrace) {
      throw AuthException.fromError(error, stackTrace);
    }

    // 200: parse the ApiResponse<AuthResponse> envelope and confirm success.
    if (response.statusCode == 200) {
      try {
        final Map<String, dynamic> decoded =
            jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;

        if (decoded['success'] == true &&
            decoded['data'] is Map<String, dynamic>) {
          final session = AuthSession.fromJson(
            decoded['data'] as Map<String, dynamic>,
          );
          lastSession = session;
          return session;
        }
      } catch (_) {
        // Fall through to unexpected error below.
      }
      return Future.error(
        const AuthException(
          type: AuthErrorType.unexpected,
          userMessage: 'Login succeeded but the response could not be read. Please try again.',
        ),
      );
    }

    // Any other status is an API error — map it safely.
    throw AuthException.fromResponse(response);
  }

  static const String _registerPath = '/register';

  @override
  Future<AuthSession> register({
    required String firstName,
    required String lastName,
    required String email,
    required String phone,
    required String password,
  }) async {
    final Uri uri = Uri.parse('$baseUrl$_registerPath');

    http.Response response;
    try {
      response = await _client
          .post(
            uri,
            headers: <String, String>{
              'Content-Type': 'application/json; charset=UTF-8',
              'Accept': 'application/json',
            },
            body: jsonEncode(<String, String>{
              'firstName': firstName.trim(),
              'lastName': lastName.trim(),
              'email': email.trim(),
              'phone': phone.trim(),
              'password': password,
            }),
          )
          .timeout(_timeout);
    } on http.ClientException catch (error, stackTrace) {
      throw AuthException.fromError(error, stackTrace);
    } on FormatException catch (error, stackTrace) {
      throw AuthException.fromError(error, stackTrace);
    } on Exception catch (error, stackTrace) {
      throw AuthException.fromError(error, stackTrace);
    }

    if (response.statusCode == 200 || response.statusCode == 201) {
      try {
        final Map<String, dynamic> decoded =
            jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;

        if (decoded['success'] == true &&
            decoded['data'] is Map<String, dynamic>) {
          final session = AuthSession.fromJson(
            decoded['data'] as Map<String, dynamic>,
          );
          lastSession = session;
          return session;
        }
      } catch (_) {
        // Fall through to unexpected error below.
      }
      throw const AuthException(
        type: AuthErrorType.unexpected,
        userMessage: 'Registration succeeded but the response could not be read. Please try again.',
      );
    }

    throw AuthException.fromResponse(response);
  }

  @override
  Future<void> verifyEmail({required String token}) async {
    final Uri uri = Uri.parse('$_emailBaseUrl/verify')
        .replace(queryParameters: <String, String>{'token': token});

    http.Response response;
    try {
      response = await _client
          .post(uri, headers: <String, String>{'Accept': 'application/json'})
          .timeout(_timeout);
    } on http.ClientException catch (error, stackTrace) {
      throw AuthException.fromError(error, stackTrace);
    } on FormatException catch (error, stackTrace) {
      throw AuthException.fromError(error, stackTrace);
    } on Exception catch (error, stackTrace) {
      throw AuthException.fromError(error, stackTrace);
    }

    if (response.statusCode == 200) {
      return;
    }

    throw AuthException.fromResponse(response);
  }

  static const String _refreshPath = '/refresh';

  @override
  Future<AuthSession> refresh({required String refreshToken}) async {
    final Uri uri = Uri.parse('$baseUrl$_refreshPath');

    http.Response response;
    try {
      response = await _client
          .post(
            uri,
            headers: <String, String>{
              'Authorization': 'Bearer $refreshToken',
              'Accept': 'application/json',
            },
          )
          .timeout(_timeout);
    } on http.ClientException catch (error, stackTrace) {
      throw AuthException.fromError(error, stackTrace);
    } on FormatException catch (error, stackTrace) {
      throw AuthException.fromError(error, stackTrace);
    } on Exception catch (error, stackTrace) {
      throw AuthException.fromError(error, stackTrace);
    }

    if (response.statusCode == 200) {
      try {
        final Map<String, dynamic> decoded =
            jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
        if (decoded['success'] == true &&
            decoded['data'] is Map<String, dynamic>) {
          final session = AuthSession.fromJson(
            decoded['data'] as Map<String, dynamic>,
          );
          lastSession = session;
          return session;
        }
      } catch (_) {
        // Fall through.
      }
      throw const AuthException(
        type: AuthErrorType.unexpected,
        userMessage:
            'Token refresh succeeded but the response could not be read.',
      );
    }

    throw AuthException.fromResponse(response);
  }

  @override
  String toString() => 'HttpAuthService($baseUrl)';
}
