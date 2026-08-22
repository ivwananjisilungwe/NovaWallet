import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/providers.dart';
import '../../../core/theme/nova_colors.dart';
import '../providers/auth_provider.dart';
import '../providers/biometric_provider.dart';

/// Biometric unlock gate (shown when the feature flag is ON and a saved
/// session exists).
///
/// Flow:
///  1. Auto-triggers the platform biometric prompt on first frame.
///  2. SUCCESS  -> marks the cycle unlocked; the router redirect then lets
///     the restored session through to `/wallet` (or `/pin/set`).
///  3. CANCEL / FAILURE / UNAVAILABLE -> falls back to the normal login flow
///     by clearing the local session (`logout`); the router sends the user to
///     `/login`. No biometric data is ever touched — the OS owns it all.
class BiometricLockScreen extends ConsumerStatefulWidget {
  const BiometricLockScreen({super.key});

  @override
  ConsumerState<BiometricLockScreen> createState() =>
      _BiometricLockScreenState();
}

class _BiometricLockScreenState extends ConsumerState<BiometricLockScreen> {
  bool _prompted = false;

  /// True once the prompt failed or was cancelled — controls whether the
  /// retry UI is shown. On success the router navigates away while the
  /// progress indicator is still up.
  bool _failed = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _authenticate());
  }

  Future<void> _authenticate() async {
    if (_prompted || !mounted) return;
    _prompted = true;

    final bio = ref.read(biometricProvider);
    final auth = ref.read(authProvider);

    // Feature turned off, capabilities gone, or no restorable session:
    // nothing to unlock -> fall back immediately.
    if (!bio.state.enabled || !bio.state.usable || !auth.state.isAuthenticated) {
      await _fallbackToLogin();
      return;
    }

    // The gate only makes sense when there is a persisted session to restore.
    final token = await ref.read(tokenStorageProvider).accessToken;
    if (token == null || token.isEmpty) {
      await _fallbackToLogin();
      return;
    }

    final success = await ref.read(biometricProvider.notifier).unlock();
    if (!mounted) return;

    if (success) {
      // Router refreshListenable picks this up and proceeds authenticated.
      ref.read(biometricProvider.notifier).markUnlocked();
    } else {
      setState(() => _failed = true);
    }
  }

  /// Cancel/fail/unavailable path: clear the local session so the router's
  /// unauthenticated branch funnels the user to the normal login screen.
  Future<void> _fallbackToLogin() async {
    await ref.read(authProvider.notifier).logout();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NovaColors.background,
      body: SafeArea(
        child: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 96,
                height: 96,
                decoration: const BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [NovaColors.primary, NovaColors.primaryContainer],
                  ),
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.fingerprint,
                  size: 56,
                  color: NovaColors.onPrimary,
                ),
              ),
              const SizedBox(height: 24),
              const Text(
                'Unlock NovaWallet',
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w700,
                  color: NovaColors.onBackground,
                  letterSpacing: -0.02,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Use your fingerprint or face to continue',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 14,
                  color: NovaColors.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 32),
              if (!_failed)
                const SizedBox(
                  width: 28,
                  height: 28,
                  child: CircularProgressIndicator(strokeWidth: 3),
                )
              else ...[
                FilledButton.icon(
                  onPressed: () {
                    _prompted = false;
                    _failed = false;
                    setState(() {});
                    _authenticate();
                  },
                  icon: const Icon(Icons.fingerprint),
                  label: const Text('Try again'),
                ),
                const SizedBox(height: 12),
                TextButton(
                  onPressed: _fallbackToLogin,
                  child: const Text('Use password instead'),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
