import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/providers.dart';
import '../data/kyc_repository.dart';
import '../models/kyc_status.dart';

final kycRepositoryProvider = Provider<KycRepository>(
  (ref) => KycRepository(ref.watch(apiClientProvider)),
);

/// Current user's KYC status.
final kycStatusProvider =
    AsyncNotifierProvider<KycNotifier, KycStatus>(KycNotifier.new);

class KycNotifier extends AsyncNotifier<KycStatus> {
  @override
  Future<KycStatus> build() =>
      ref.watch(kycRepositoryProvider).getStatus();

  Future<void> refresh() async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard(
      () => ref.read(kycRepositoryProvider).getStatus(),
    );
  }
}
