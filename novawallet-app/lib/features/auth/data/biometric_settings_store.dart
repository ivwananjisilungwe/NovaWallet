import '../../../core/cache/hive_boxes.dart';

/// Persists the biometric-unlock feature flag locally. Default is OFF.
abstract interface class BiometricSettingsStore {
  Future<bool> load();
  Future<void> save(bool enabled);
}

/// Hive-backed implementation using the shared `settings` box.
///
/// Booleans are stored as 'true'/'false' strings, mirroring the convention
/// used by [TokenStorage] for `auth_pin_set`.
class HiveBiometricSettingsStore implements BiometricSettingsStore {
  const HiveBiometricSettingsStore();

  static const _key = 'biometric_unlock_enabled';

  @override
  Future<bool> load() async {
    try {
      return HiveBoxes.settings.get(_key) == 'true';
    } catch (_) {
      // Box not open yet (or unavailable): fail safe to OFF.
      return false;
    }
  }

  @override
  Future<void> save(bool enabled) async {
    try {
      await HiveBoxes.settings.put(_key, enabled ? 'true' : 'false');
    } catch (_) {
      // Persistence failure must never crash the toggle; worst case the
      // flag reverts to its OFF default on next launch.
    }
  }
}
