import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../core/theme/nova_colors.dart';
import '../features/admin/routes.dart';
import '../features/extras/routes.dart';
import '../features/kyc/routes.dart';
import '../features/transaction/routes.dart';

/// Root 4-tab shell hosting feature screens (Home / Send / Cards / Profile).
///
/// The active tab's screen is rendered by [child] (nested via GoRouter's
/// ShellRoute). Tapping a tab navigates to that tab's root route.
class MainShell extends StatefulWidget {
  const MainShell({super.key, required this.child});

  final Widget child;

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
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