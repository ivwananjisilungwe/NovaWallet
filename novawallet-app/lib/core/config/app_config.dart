import 'dart:io';

import 'package:flutter/foundation.dart';

/// Central app configuration.
///
/// API base URL resolution order:
/// 1. `--dart-define=API_BASE_URL=...` (highest priority)
/// 2. Platform-aware default:
///    - Android                                 => `http://10.0.2.2:8080/api`
///    - iOS / Web / desktop                     => `http://localhost:8080/api`
///
/// NOTE: On the Android emulator, `10.0.2.2` is a special alias for the host
/// machine's loopback interface (`localhost`), so the emulator reaches a
/// backend running on the dev machine without any LAN IP. Physical Android
/// devices cannot use that alias — run them with
/// `--dart-define=API_BASE_URL=http://<host-LAN-IP>:8080/api`.
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