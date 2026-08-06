import 'dart:convert';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/storage/token_storage.dart';
import '../models/user.dart';

/// Backend calls for the auth domain:
///   POST /v1/auth/register   (public)
///   POST /v1/auth/login      (public)
///   POST /v1/auth/refresh    (internal, handled by ApiClient)
///   POST /v1/password/forgot (public)
///   POST /v1/password/reset  (public)
///   POST /v1/email/verify    (public, ?token=)
///   POST /v1/pin             (JWT)
///   POST /v1/users/me/change-password (JWT)
class AuthRepository {
  const AuthRepository(this._api, this._storage);

  final ApiClient _api;
  final TokenStorage _storage;

  /// Register a new user. Backend creates the account and returns an
  /// `AuthResponse` (access + refresh). Response is wrapped by ApiClient.
  Future<AuthResponse> register({
    required String firstName,
    required String lastName,
    required String email,
    required String phone,
    required String password,
  }) async {
    final auth = await _api.post<AuthResponse>(
      '/v1/auth/register',
      body: {
        'firstName': firstName,
        'lastName': lastName,
        'email': email,
        'phone': phone,
        'password': password,
      },
      parser: (json) => AuthResponse.fromJson(json as Map<String, dynamic>),
    );
    if (auth == null) throw const ApiException(message: 'Registration failed.');
    await _persist(auth);
    return auth;
  }

  Future<AuthResponse> login({
    required String email,
    required String password,
  }) async {
    final auth = await _api.post<AuthResponse>(
      '/v1/auth/login',
      body: {'email': email, 'password': password},
      parser: (json) => AuthResponse.fromJson(json as Map<String, dynamic>),
    );
    if (auth == null) throw const ApiException(message: 'Login failed.');
    await _persist(auth);
    return auth;
  }

  /// Sends a password-reset email (backend always returns 200).
  Future<void> forgotPassword(String email) => _api.post<Never>(
        '/v1/password/forgot',
        body: {'email': email},
      );

  Future<void> resetPassword({
    required String token,
    required String newPassword,
  }) =>
      _api.post<Never>(
        '/v1/password/reset',
        body: {'token': token, 'newPassword': newPassword},
      );

  /// Changes the authenticated user's password (requires current password).
  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
  }) =>
      _api.post<Never>(
        '/v1/users/me/change-password',
        body: {'currentPassword': currentPassword, 'newPassword': newPassword},
      );

  Future<void> verifyEmail(String token) => _api.post<Never>(
        '/v1/email/verify',
        query: {'token': token},
      );

  /// Sets the transaction PIN (4-6 digits, required before transacting).
  Future<void> setPin(String pin) => _api.post<Never>(
        '/v1/pin',
        body: {'pin': pin},
      );

  Future<void> logout() async {
    await _storage.clear();
  }

  /// Hydrate a session from secure storage (hot app start).
  Future<User?> restoreSession() async {
    final userJson = await _storage.userJson;
    if (userJson == null || userJson.isEmpty) return null;
    try {
      return User.fromJson(jsonDecode(userJson) as Map<String, dynamic>);
    } on FormatException {
      await _storage.clear();
      return null;
    }
  }

  Future<bool> hasPin() async {
    final raw = await _storage.pinSetRaw;
    return raw == 'true';
  }

  /// Records that the user has configured a transaction PIN locally.
  /// The backend flag is set via `setPin`; this keeps the client in sync.
  Future<void> markPinSet() => _storage.setPinSet(true);

  Future<void> _persist(AuthResponse auth) async {
    await _storage.saveSession(
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
      userJson: auth.user == null ? null : jsonEncode(auth.user!.toJson()),
    );
  }
}