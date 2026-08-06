import 'dart:io';

import 'package:flutter/foundation.dart';

/// Central app configuration.
///
/// API base URL resolution order:
/// 1. `--dart-define=API_BASE_URL=...` (highest priority, e.g. for a remote server)
/// 2. Platform-aware default:
///    - Android emulator => `http://10.0.2.2:8080/api`
///    - everything else   => `http://localhost:8080/api`
///
/// The backend serves under the `/api` context path (Spring Boot
/// `server.servlet.context-path`), which is why `/api` is appended here.
class AppConfig {
  AppConfig._();

  static const String _definedBaseUrl = String.fromEnvironment('API_BASE_URL');

  static String get apiBaseUrl {
    if (_definedBaseUrl.isNotEmpty) return _definedBaseUrl;
    if (!kIsWeb && Platform.isAndroid) return 'http://10.0.2.2:8080/api';
    return 'http://localhost:8080/api';
  }

  /// Access token TTL as configured on the backend (15 minutes).
  static const Duration accessTokenTtl = Duration(minutes: 15);

  /// Short buffer subtracted from the real expiry when planning a refresh.
  static const Duration tokenRefreshGrace = Duration(seconds: 30);
}