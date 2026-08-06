import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/providers.dart';
import '../data/transaction_repository.dart';
import '../models/fee_estimate.dart';
import '../models/transaction.dart';

final transactionRepositoryProvider = Provider<TransactionRepository>(
  (ref) => TransactionRepository(ref.watch(apiClientProvider)),
);

/// Recent transaction history for the given wallet id. Pass the wallet id as
/// the family argument so it invalidates when the wallet changes.
final transactionsProvider = FutureProvider.family<List<WalletTransaction>, String>(
  (ref, walletId) =>
      ref.watch(transactionRepositoryProvider).getHistory(walletId),
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
