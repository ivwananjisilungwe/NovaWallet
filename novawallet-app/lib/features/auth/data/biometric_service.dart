import 'package:local_auth/local_auth.dart';

/// Thin wrapper around the platform biometric APIs (`local_auth`).
///
/// SECURITY NOTE: this class never sees, stores, or transmits biometric data.
/// It only asks the OS to run its native authentication dialog (fingerprint /
/// Face ID) and relays the boolean outcome. The OS owns all biometric
/// enrollment and matching.
class BiometricService {
  BiometricService({LocalAuthentication? auth})
      : _auth = auth ?? LocalAuthentication();

  final LocalAuthentication _auth;

  /// Whether enrolled biometrics can be checked right now (hardware present
  /// AND at least one biometric enrolled).
  Future<bool> canCheckBiometrics() => _guard(() => _auth.canCheckBiometrics);

  /// Whether the device hardware supports biometric authentication at all,
  /// regardless of current enrollment.
  Future<bool> isDeviceSupported() => _guard(() => _auth.isDeviceSupported());

  /// Shows the platform biometric prompt. Returns true only when the user
  /// successfully authenticates; false on cancel, failure, or unavailability.
  ///
  /// `biometricOnly` keeps the prompt strictly fingerprint/Face ID so the
  /// outcome maps deterministically onto the app's own fallback (its normal
  /// login screen). `stickyAuth` lets an interrupted prompt (app backgrounded
  /// mid-dialog on Android) resume instead of failing silently.
  Future<bool> authenticate({required String reason}) => _guard(
        () => _auth.authenticate(
          localizedReason: reason,
          options: const AuthenticationOptions(
            biometricOnly: true,
            stickyAuth: true,
          ),
        ),
      );

  /// Platform-channel calls throw on unsupported platforms (desktop/web/unit
  /// tests) or plugin errors; degrade to "unavailable" instead of crashing.
  Future<bool> _guard(Future<bool> Function() operation) async {
    try {
      return await operation();
    } catch (_) {
      return false;
    }
  }
}
