import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/providers.dart';
import '../data/transaction_repository.dart';
import '../models/fee_estimate.dart';
import '../models/transaction.dart';

final transactionRepositoryProvider = Provider<TransactionRepository>(
  (ref) => TransactionRepository(ref.watch(apiClientProvider)),
);

/// Paginated transaction history for the given wallet id. Pass the wallet id as
/// the family argument so it invalidates when the wallet changes.
final transactionsProvider = FutureProvider.family<List<WalletTransaction>, String>(
  (ref, walletId) =>
      ref.watch(transactionRepositoryProvider).getHistory(walletId, page: 0, size: 20),
);

/// A single transaction fetched by reference.
final transactionByReferenceProvider =
    FutureProvider.family<WalletTransaction, String>(
  (ref, reference) =>
      ref.watch(transactionRepositoryProvider).getByReference(reference),
);

/// Live fee estimate for `(type, amount)`.
final feeEstimateProvider =
    FutureProvider.family<FeeEstimate, ({String type, String amount})>(
  (ref, arg) =>
      ref.watch(transactionRepositoryProvider).estimateFee(
            type: arg.type,
            amount: arg.amount,
          ),
);

/// Paginated transaction state for a wallet.
class TransactionPaginationState {
  const TransactionPaginationState({
    this.transactions = const [],
    this.page = 0,
    this.hasMore = true,
    this.isLoadingMore = false,
  });

  final List<WalletTransaction> transactions;
  final int page;
  final bool hasMore;
  final bool isLoadingMore;

  TransactionPaginationState copyWith({
    List<WalletTransaction>? transactions,
    int? page,
    bool? hasMore,
    bool? isLoadingMore,
  }) => TransactionPaginationState(
    transactions: transactions ?? this.transactions,
    page: page ?? this.page,
    hasMore: hasMore ?? this.hasMore,
    isLoadingMore: isLoadingMore ?? this.isLoadingMore,
  );
}

class TransactionPaginationNotifier extends StateNotifier<TransactionPaginationState> {
  TransactionPaginationNotifier(this._repo, this._walletId)
      : super(const TransactionPaginationState()) {
    loadInitial();
  }

  final TransactionRepository _repo;
  final String _walletId;

  Future<void> loadInitial() async {
    state = const TransactionPaginationState();
    final items = await _repo.getHistory(_walletId, page: 0);
    state = TransactionPaginationState(
      transactions: items,
      page: 0,
      hasMore: items.length >= 20, // if we got a full page, there might be more
    );
  }

  Future<void> loadMore() async {
    if (!state.hasMore || state.isLoadingMore) return;
    state = state.copyWith(isLoadingMore: true);
    final nextPage = state.page + 1;
    final items = await _repo.getHistory(_walletId, page: nextPage);
    state = TransactionPaginationState(
      transactions: [...state.transactions, ...items],
      page: nextPage,
      hasMore: items.length >= 20,
    );
  }

  Future<void> refresh() async => loadInitial();
}

/// Paginated transaction state provider for a wallet.
final transactionPaginationProvider = 
    StateNotifierProvider.family<TransactionPaginationNotifier, TransactionPaginationState, String>(
  (ref, walletId) => TransactionPaginationNotifier(
    ref.watch(transactionRepositoryProvider),
    walletId,
  ),
);
