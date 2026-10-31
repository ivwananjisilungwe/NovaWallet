import 'package:flutter/material.dart';

import 'app/app.dart';

/// Application entry point.
void main() {
  // Initialize Flutter's binding before any future platform-service setup.
  WidgetsFlutterBinding.ensureInitialized();

  // Mount the root widget and start the application.
  runApp(const NovaWalletApp());
}
