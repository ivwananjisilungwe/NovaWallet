import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import 'package:novawallet_mobile/features/auth/data/auth_models.dart';

/// Abstraction for persisting the authenticated session.
///
/// Tokens are stored only through secure storage; never in plaintext prefs.
abstract class SessionStore {
  Future<void> saveSession(AuthSession session);
  Future<AuthSession?> readSession();
  Future<String?> readAccessToken();
  Future<void> clear();
}

/// Secure-storage-backed [SessionStore].
class SecureSessionStore implements SessionStore {
  SecureSessionStore({FlutterSecureStorage? storage})
    : _storage = storage ?? const FlutterSecureStorage();

  final FlutterSecureStorage _storage;

  static const _kAccessToken = 'novawallet_access_token';
  static const _kRefreshToken = 'novawallet_refresh_token';
  static const _kExpiresIn = 'novawallet_expires_in';
  static const _kUser = 'novawallet_user';

  @override
  Future<void> saveSession(AuthSession session) async {
    await _storage.write(key: _kAccessToken, value: session.accessToken);
    await _storage.write(key: _kRefreshToken, value: session.refreshToken);
    await _storage.write(key: _kExpiresIn, value: '${session.expiresIn}');
    await _storage.write(key: _kUser, value: jsonEncode(session.user.toJson()));
  }

  @override
  Future<AuthSession?> readSession() async {
    final accessToken = await _storage.read(key: _kAccessToken);
    final refreshToken = await _storage.read(key: _kRefreshToken);
    final expiresInRaw = await _storage.read(key: _kExpiresIn);
    final userRaw = await _storage.read(key: _kUser);
    if (accessToken == null ||
        accessToken.isEmpty ||
        refreshToken == null ||
        refreshToken.isEmpty) {
      return null;
    }
    AuthUser user;
    try {
      user = userRaw != null && userRaw.isNotEmpty
          ? AuthUser.fromJson(jsonDecode(userRaw) as Map<String, dynamic>)
          : const AuthUser(
              id: '',
              firstName: '',
              lastName: '',
              email: '',
              role: 'USER',
              emailVerified: false,
              pinSet: false,
            );
    } on Object {
      return null;
    }
    return AuthSession(
      accessToken: accessToken,
      refreshToken: refreshToken,
      expiresIn: int.tryParse(expiresInRaw ?? '') ?? 0,
      user: user,
    );
  }

  @override
  Future<String?> readAccessToken() async {
    final token = await _storage.read(key: _kAccessToken);
    if (token == null || token.isEmpty) return null;
    return token;
  }

  @override
  Future<void> clear() async {
    await _storage.delete(key: _kAccessToken);
    await _storage.delete(key: _kRefreshToken);
    await _storage.delete(key: _kExpiresIn);
    await _storage.delete(key: _kUser);
  }
}
