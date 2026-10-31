import 'dart:convert';

import 'package:http/http.dart' as http;

import 'package:novawallet_mobile/features/auth/data/auth_service.dart';
import 'package:novawallet_mobile/features/auth/data/session_store.dart';

/// Small reusable helper for protected endpoints.
///
/// Attaches `Authorization: Bearer <accessToken>` from [sessionStore] and
/// parses the standard `ApiResponse<T>` / `ErrorResponse` envelopes.
class AuthenticatedHttpClient {
  AuthenticatedHttpClient({
    required this.sessionStore,
    http.Client? client,
    this.timeout = const Duration(seconds: 15),
  }) : _client = client ?? http.Client();

  final SessionStore sessionStore;
  final http.Client _client;
  final Duration timeout;

  /// Performs an authenticated GET and returns the parsed `data` map.
  ///
  /// Throws [AuthException] on missing session, 401, or other failures.
  Future<Map<String, dynamic>> getJson(Uri uri) async {
    final token = await sessionStore.readAccessToken();
    if (token == null || token.isEmpty) {
      throw const AuthException(
        type: AuthErrorType.sessionExpired,
        userMessage: 'Your session has expired. Please sign in again.',
      );
    }

    http.Response response;
    try {
      response = await _client
          .get(
            uri,
            headers: <String, String>{
              'Authorization': 'Bearer $token',
              'Accept': 'application/json',
            },
          )
          .timeout(timeout);
    } on http.ClientException catch (error, stackTrace) {
      throw AuthException.fromError(error, stackTrace);
    } on FormatException catch (error, stackTrace) {
      throw AuthException.fromError(error, stackTrace);
    } on Exception catch (error, stackTrace) {
      throw AuthException.fromError(error, stackTrace);
    }

    if (response.statusCode == 200) {
      try {
        final decoded =
            jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
        if (decoded['success'] == true &&
            decoded['data'] is Map<String, dynamic>) {
          return decoded['data'] as Map<String, dynamic>;
        }
      } catch (_) {
        // Fall through.
      }
      throw const AuthException(
        type: AuthErrorType.unexpected,
        userMessage: 'The server response could not be read. Please try again.',
      );
    }

    if (response.statusCode == 401) {
      throw const AuthException(
        type: AuthErrorType.sessionExpired,
        userMessage: 'Your session has expired. Please sign in again.',
      );
    }

    throw AuthException.fromResponse(response);
  }
}
