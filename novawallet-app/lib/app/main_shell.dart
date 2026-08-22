import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../core/theme/nova_colors.dart';
import '../features/admin/routes.dart';
import '../features/auth/providers/auth_provider.dart';
import '../features/auth/providers/biometric_provider.dart';
import '../features/extras/routes.dart';
import '../features/kyc/routes.dart';
import '../features/transaction/routes.dart';

/// Root 4-tab shell hosting feature screens (Home / Send / Cards / Profile).
///
/// The active tab's screen is rendered by [child] (nested via GoRouter's
/// ShellRoute). Tapping a tab navigates to that tab's root route.
///
/// Also observes app lifecycle: when the app returns from background while
/// biometric unlock is enabled, the lock is re-armed so the router sends the
/// user back to `/biometric-lock` (resume re-lock).
class MainShell extends ConsumerStatefulWidget {
  const MainShell({super.key, required this.child});

  final Widget child;

  @override
  ConsumerState<MainShell> createState() => _MainShellState();
}

class _MainShellState extends ConsumerState<MainShell>
    with WidgetsBindingObserver {
  int get _index {
    final matched = GoRouter.of(context).state.matchedLocation;
    if (matched.startsWith('/send')) return 1;
    if (matched.startsWith('/cards')) return 2;
    if (matched.startsWith('/profile')) return 3;
    return 0;
  }

  static const _tabs = [
    (Icons.home_outlined, Icons.home, 'Home', '/wallet'),
    (Icons.send_outlined, Icons.send, 'Send', '/send'),
    (Icons.credit_card_outlined, Icons.credit_card, 'Cards', '/cards'),
    (Icons.person_outline, Icons.person, 'Profile', '/profile'),
  ];

  /// Last lifecycle state seen; used to detect background -> foreground
  /// transitions only (the initial `resumed` right after build is ignored).
  AppLifecycleState? _lastLifecycleState;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    final wasBackgrounded =
        _lastLifecycleState == AppLifecycleState.paused ||
            _lastLifecycleState == AppLifecycleState.hidden;
    _lastLifecycleState = state;

    if (state != AppLifecycleState.resumed || !wasBackgrounded || !mounted) {
      return;
    }

    // Re-arm the biometric gate for this resume-cycle. The router's
    // refreshListenable re-runs the redirect and parks on /biometric-lock.
    final bio = ref.read(biometricProvider);
    final auth = ref.read(authProvider);
    if (bio.state.enabled && auth.state.isAuthenticated) {
      ref.read(biometricProvider.notifier).requireUnlock();
    }
  }

  void _onTabTap(int i) {
    final route = _tabs[i].$4;
    if (GoRouter.of(context).state.matchedLocation != route) {
      GoRouter.of(context).go(route);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NovaColors.background,
      body: widget.child,
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: _onTabTap,
        backgroundColor: NovaColors.surfaceContainerLowest,
        indicatorColor: NovaColors.secondaryContainer,
        destinations: [
          for (final (icon, activeIcon, label, _) in _tabs)
            NavigationDestination(
              icon: Icon(icon),
              selectedIcon: Icon(activeIcon),
              label: label,
            ),
        ],
      ),
    );
  }
}

/// Aggregated shell routes — single source of truth for navigation.
/// Every feature exports a `List<RouteBase>` from its own `routes.dart`.
List<RouteBase> buildFeatureRoutes() {
  return [
    ...transactionRoutes,
    ...kycRoutes,
    ...adminRoutes,
    ...extrasRoutes,
  ];
}