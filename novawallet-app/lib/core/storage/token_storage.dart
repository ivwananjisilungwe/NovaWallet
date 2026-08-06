import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Wraps platform secure storage (Keychain / EncryptedSharedPreferences).
class TokenStorage {
  TokenStorage({FlutterSecureStorage? storage})
      : _storage = storage ?? const FlutterSecureStorage();

  static const _kAccessToken = 'auth_access_token';
  static const _kRefreshToken = 'auth_refresh_token';
  static const _kUser = 'auth_user_json';
  static const _kPinSet = 'auth_pin_set';

  final FlutterSecureStorage _storage;

  Future<String?> get accessToken => _storage.read(key: _kAccessToken);
  Future<String?> get refreshToken => _storage.read(key: _kRefreshToken);
  Future<String?> get userJson => _storage.read(key: _kUser);
  Future<String?> get pinSetRaw => _storage.read(key: _kPinSet);

  Future<void> saveSession({
    required String accessToken,
    required String refreshToken,
    String? userJson,
  }) async {
    await _storage.write(key: _kAccessToken, value: accessToken);
    await _storage.write(key: _kRefreshToken, value: refreshToken);
    if (userJson != null) {
      await _storage.write(key: _kUser, value: userJson);
    }
  }

  Future<void> saveUser(String userJson) =>
      _storage.write(key: _kUser, value: userJson);

  Future<void> setPinSet(bool value) =>
      _storage.write(key: _kPinSet, value: value ? 'true' : 'false');

  Future<void> clear() async {
    await _storage.delete(key: _kAccessToken);
    await _storage.delete(key: _kRefreshToken);
    await _storage.delete(key: _kUser);
    await _storage.delete(key: _kPinSet);
  }
}
