import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/providers.dart';
import '../data/wallet_repository.dart';
import '../models/wallet.dart';

/// Repository singleton for the wallet domain.
final walletRepositoryProvider = Provider<WalletRepository>(
  (ref) => WalletRepository(ref.watch(apiClientProvider)),
);

/// Current wallet + async state. Exposes actions to re-fetch and refresh the
/// balance after any money movement.
final walletProvider =
    AsyncNotifierProvider<WalletNotifier, Wallet>(WalletNotifier.new);

class WalletNotifier extends AsyncNotifier<Wallet> {
  @override
  Future<Wallet> build() => ref
      .watch(walletRepositoryProvider)
      .getMyWallet();

  Future<void> refresh() async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard(
      () => ref.read(walletRepositoryProvider).getMyWallet(),
    );
  }

  Future<void> refreshBalance() async {
    final current = state.valueOrNull;
    if (current == null) return;
    state = await AsyncValue.guard(
      () => ref.read(walletRepositoryProvider).getBalance(current.id),
    );
  }

  /// Invalidate so the provider re-runs `build` on next read.
  Future<void> reload() => ref.refresh(walletProvider.future);
}
