import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app/app.dart';
import 'app/router.dart';
import 'core/network/api_client.dart';
import 'core/providers.dart';
import 'core/storage/token_storage.dart';

/// Entry point.
///
/// Composes the real dependency graph (overrides the throwing core providers)
/// and boots the app with Riverpod + GoRouter.
void main() {
  WidgetsFlutterBinding.ensureInitialized();

  final container = ProviderContainer(
    overrides: [
      apiClientProvider.overrideWithValue(
        ApiClient(dio: Dio(), storage: TokenStorage()),
      ),
    ],
  );

  runApp(
    UncontrolledProviderScope(
      container: container,
      child: NovawalletApp(router: container.read(routerProvider)),
    ),
  );
}