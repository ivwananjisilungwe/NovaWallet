import 'package:flutter/material.dart';

import 'package:novawallet_mobile/features/splash/presentation/splash_page.dart';

import 'theme/app_theme.dart';

/// Root widget for NovaWallet.
///
/// Centralizes global Material configuration and delegates navigation
/// and screen composition to AppShell.
class NovaWalletApp extends StatelessWidget {
  const NovaWalletApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'NovaWallet',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,

      // Use the operating system's preferred appearance automatically.
      themeMode: ThemeMode.system,
      home: const SplashPage(),
    );
  }
}
