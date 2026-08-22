import 'package:flutter/material.dart';

import '../core/theme/app_theme.dart';
import 'router.dart';

/// Root application widget — wires theme + router.
class NovawalletApp extends StatelessWidget {
  const NovawalletApp({super.key, required this.router});

  final GoRouterForApp router;

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'NovaWallet',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      darkTheme: AppTheme.dark,
      // Follow the OS setting; a user-facing override can later be added via
      // a Riverpod themeModeProvider without touching this wiring.
      themeMode: ThemeMode.system,
      routerConfig: router.router,
    );
  }
}