import 'package:flutter/material.dart';

import 'package:novawallet_mobile/core/design/app_tokens.dart';
import 'package:novawallet_mobile/features/auth/data/session_store.dart';
import 'package:novawallet_mobile/features/cards/presentation/cards_page.dart';
import 'package:novawallet_mobile/features/dashboard/presentation/dashboard_page.dart';
import 'package:novawallet_mobile/features/discover/presentation/discover_page.dart';
import 'package:novawallet_mobile/features/profile/presentation/profile_page.dart';
import 'package:novawallet_mobile/features/transactions/presentation/transactions_page.dart';

class AppShell extends StatefulWidget {
  const AppShell({super.key, this.sessionStore});

  final SessionStore? sessionStore;

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  int _selectedIndex = 0;

  static const _destinations = <NavigationDestination>[
    NavigationDestination(icon: Icon(Icons.home_outlined), label: 'Home'),
    NavigationDestination(
      icon: Icon(Icons.receipt_long_outlined),
      label: 'Activity',
    ),
    NavigationDestination(
      icon: Icon(Icons.credit_card_outlined),
      label: 'Cards',
    ),
    NavigationDestination(icon: Icon(Icons.person_outline), label: 'Profile'),
    NavigationDestination(
      icon: Icon(Icons.explore_outlined),
      label: 'Discover',
    ),
  ];

  @override
  Widget build(BuildContext context) {
    final pages = <Widget>[
      DashboardPage(
        onViewAll: () => setState(() => _selectedIndex = 1),
        onDiscover: () => setState(() => _selectedIndex = 4),
        sessionStore: widget.sessionStore,
      ),
      TransactionsPage(sessionStore: widget.sessionStore),
      const CardsPage(),
      const ProfilePage(),
      const DiscoverPage(),
    ];

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(
              maxWidth: AppTokens.contentMaxWidth,
            ),
            child: IndexedStack(index: _selectedIndex, children: pages),
          ),
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: (index) =>
            setState(() => _selectedIndex = index),
        destinations: _destinations,
      ),
    );
  }
}
