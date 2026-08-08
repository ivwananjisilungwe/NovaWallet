import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:novawallet_app/core/providers.dart';

import '../data/statement_repository.dart';
import '../models/statement_summary.dart';

final statementRepositoryProvider = Provider<StatementRepository>(
  (ref) => StatementRepository(ref.watch(apiClientProvider)),
);

/// Monthly statement summary keyed by `(walletId, month)` where month is
/// `yyyy-MM` (e.g. `2026-08`). Family args mean the fetch re-runs whenever
/// the wallet or the selected month changes.
final statementProvider =
    FutureProvider.family<StatementSummary, ({String walletId, String month})>(
  (ref, arg) => ref
      .watch(statementRepositoryProvider)
      .getMonthlyStatement(arg.walletId, arg.month),
);
