import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'network/api_client.dart';
import 'storage/token_storage.dart';

// ---------------------------------------------------------------------------
// Composition root for core dependencies.
//
// Feature modules should consume these providers rather than constructing
// their own HTTP/storage stacks. Tests override [apiClientProvider] with a
// mock/ApiClient pointing at a fake backend.
// ---------------------------------------------------------------------------

/// Platform secure storage singleton.
final tokenStorageProvider = Provider<TokenStorage>((ref) {
  return TokenStorage();
});

/// Dio instance shared by [ApiClient] (and any ad-hoc needs).
final dioProvider = Provider<Dio>((ref) => Dio());

/// The configured API client; throws until wired in `main()`.
final apiClientProvider = Provider<ApiClient>((ref) {
  throw UnimplementedError(
    'apiClientProvider must be overridden in main() with the real client.',
  );
});