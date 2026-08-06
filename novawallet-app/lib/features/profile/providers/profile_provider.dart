import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/providers.dart';
import '../data/user_repository.dart';

final userRepositoryProvider = Provider<UserRepository>(
  (ref) => UserRepository(ref.watch(apiClientProvider)),
);

/// Fresh profile data from `GET /v1/users/me`.
final profileProvider = FutureProvider<UserProfile>(
  (ref) => ref.watch(userRepositoryProvider).getProfile(),
);
