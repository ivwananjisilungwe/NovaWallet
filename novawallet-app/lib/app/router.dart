import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../features/auth/providers/auth_provider.dart';
import '../features/auth/providers/biometric_provider.dart';
import '../features/auth/routes.dart';
import 'main_shell.dart';

/// Aggregated navigation config.
///
/// Route ownership:
///   * auth/pre-auth screens (splash, onboarding, login, PIN) are registered at
///     the ROOT (full-screen, no bottom nav) via `authRoutes`,
///   * wallet / KYC / admin / extras routes live inside a [ShellRoute] that
///     hosts the 4-tab [MainShell] and provides a persistent bottom nav.
///
/// The single redirect gate lives here: loading/splash gate, login gate,
/// first-run onboarding gate, the PIN-set gate, and the optional
/// biometric-unlock gate.
class GoRouterForApp {
  GoRouterForApp(this._ref);

  final Ref _ref;

  late final GoRouter router = GoRouter(
    initialLocation: '/splash',
    // Both notifiers drive redirects: session restore (AuthNotifier) and the
    // biometric capability probe / unlock status (BiometricNotifier).
    refreshListenable: Listenable.merge([
      _ref.read(authProvider),
      _ref.read(biometricProvider),
    ]),
    redirect: (context, state) => _redirect(context, state),
    routes: [
      // Public/auth flow — no bottom nav.
      ...authRoutes,
      // Authenticated shell (4 tabs) hosting feature routes.
      ShellRoute(
        builder: (context, state, child) => MainShell(child: child),
        routes: [
          ...buildFeatureRoutes(),
        ],
      ),
    ],
  );

  String? _redirect(BuildContext context, GoRouterState state) {
    final auth = _ref.read(authProvider).state;
    final bio = _ref.read(biometricProvider).state;
    final location = state.matchedLocation;

    // While the saved session is being restored, park on the splash screen.
    if (auth.isInitializing) {
      return location == '/splash' ? null : '/splash';
    }

    if (!auth.isAuthenticated) {
      // Allow the public flow; everything else funnels to login.
      if (_isRootOrPublic(location)) return null;
      return '/login';
    }

    // Biometric unlock is configured but the capability probe has not
    // completed yet: keep parking on splash so the wallet never flashes
    // before the gate decision is made.
    if (bio.enabled && !bio.checked) {
      return location == '/splash' ? null : '/splash';
    }

    if (!auth.hasPin && !location.startsWith('/pin')) {
      return '/pin/set';
    }

    // Admin console is ADMIN-only.
    if (location.startsWith('/admin') && !auth.isAdmin) {
      return '/wallet';
    }

    // Biometric unlock gate: flag ON + probed + not yet unlocked this cycle
    // routes to the lock screen. The lock screen owns both outcomes —
    // success (proceed with the restored session) and cancel/fail/
    // unavailability (logout -> normal login). Unavailability therefore
    // never silently bypasses the lock.
    final needsUnlock = bio.enabled && bio.checked && !bio.sessionUnlocked;

    if (location.startsWith('/biometric')) {
      // The lock screen is only a valid destination while a unlock is due.
      return needsUnlock ? null : '/wallet';
    }
    if (needsUnlock && !location.startsWith('/pin')) {
      // The mandatory PIN-setup flow outranks the lock: never hijack a
      // redirect to /pin/*, or first-run users could never set their PIN.
      return '/biometric-lock';
    }

    // Signed-in users never see the public flow.
    if (_isRootOrPublic(location)) return '/wallet';

    return null;
  }

  static bool _isRootOrPublic(String location) {
    const public = [
      '/splash',
      '/login',
      '/register',
      '/onboarding',
      '/forgot-password',
      '/reset-password',
      '/verify-email',
    ];
    return public.any(location.startsWith);
  }
}

/// Single entry point used by `main()`.
final routerProvider = Provider<GoRouterForApp>((ref) => GoRouterForApp(ref));
