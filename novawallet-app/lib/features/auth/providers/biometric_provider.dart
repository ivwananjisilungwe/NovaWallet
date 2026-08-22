import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/biometric_service.dart';
import '../data/biometric_settings_store.dart';

/// Immutable snapshot of the biometric-unlock feature state.
///
/// Read synchronously by the router's redirect gate (same contract as
/// [AuthState]) and by the Security screen toggle.
class BiometricState {
  const BiometricState({
    this.enabled = false,
    this.checked = false,
    this.deviceSupported = false,
    this.canCheckBiometrics = false,
    this.sessionUnlocked = false,
  });

  /// User preference, persisted locally. Default OFF.
  final bool enabled;

  /// True once the startup capability probe has completed.
  final bool checked;

  /// Hardware supports biometrics at all.
  final bool deviceSupported;

  /// Biometrics are enrolled and checkable right now.
  final bool canCheckBiometrics;

  /// True once the current app-run / resume-cycle has been unlocked.
  /// Reset when the app returns from background while the flag is ON.
  final bool sessionUnlocked;

  bool get usable => deviceSupported && canCheckBiometrics;

  BiometricState copyWith({
    bool? enabled,
    bool? checked,
    bool? deviceSupported,
    bool? canCheckBiometrics,
    bool? sessionUnlocked,
  }) {
    return BiometricState(
      enabled: enabled ?? this.enabled,
      checked: checked ?? this.checked,
      deviceSupported: deviceSupported ?? this.deviceSupported,
      canCheckBiometrics: canCheckBiometrics ?? this.canCheckBiometrics,
      sessionUnlocked: sessionUnlocked ?? this.sessionUnlocked,
    );
  }
}

/// Owns the biometric-unlock runtime state.
///
/// The platform prompt itself is delegated to [BiometricService]; this class
/// only tracks preference + capability + per-cycle unlock status.
class BiometricNotifier extends ChangeNotifier {
  BiometricNotifier(this._service, this._store);

  final BiometricService _service;
  final BiometricSettingsStore _store;

  BiometricState _state = const BiometricState();
  BiometricState get state => _state;

  bool _initializing = false;

  /// Loads the persisted flag and probes device capabilities once.
  /// Errors degrade to "unavailable" so the gate never blocks startup.
  Future<void> init() async {
    if (_state.checked || _initializing) return;
    _initializing = true;
    try {
      final enabled = await _store.load();
      final supported = await _service.isDeviceSupported();
      final canCheck = await _service.canCheckBiometrics();
      _state = _state.copyWith(
        enabled: enabled,
        checked: true,
        deviceSupported: supported,
        canCheckBiometrics: canCheck,
      );
    } finally {
      _initializing = false;
      notifyListeners();
    }
  }

  /// Settings toggle. Turning OFF immediately disarms the lock for this run.
  Future<void> setEnabled(bool value) async {
    await _store.save(value);
    _state = _state.copyWith(enabled: value);
    if (!value) {
      _state = _state.copyWith(sessionUnlocked: false);
    }
    notifyListeners();
  }

  /// Runs the platform biometric prompt. Returns true only on success.
  Future<bool> unlock() async {
    return _service.authenticate(reason: 'Unlock NovaWallet');
  }

  /// Marks the current cycle as unlocked (called by the lock screen after a
  /// successful prompt). The router then lets the user through.
  void markUnlocked() {
    if (!_state.sessionUnlocked) {
      _state = _state.copyWith(sessionUnlocked: true);
      notifyListeners();
    }
  }

  /// Re-arms the gate so the next redirect sends the user back to the lock
  /// screen. Called when the app resumes from background while the flag is ON
  /// and a session exists.
  void requireUnlock() {
    if (_state.enabled && _state.sessionUnlocked) {
      _state = _state.copyWith(sessionUnlocked: false);
      notifyListeners();
    }
  }
}

final biometricServiceProvider = Provider<BiometricService>((ref) {
  return BiometricService();
});

final biometricStoreProvider = Provider<BiometricSettingsStore>((ref) {
  return const HiveBiometricSettingsStore();
});

/// App-wide biometric state provider. Read synchronously in redirects.
final biometricProvider = ChangeNotifierProvider<BiometricNotifier>((ref) {
  final notifier = BiometricNotifier(
    ref.watch(biometricServiceProvider),
    ref.watch(biometricStoreProvider),
  );
  // Probe capabilities without blocking the first frame.
  Future.microtask(notifier.init);
  return notifier;
});
