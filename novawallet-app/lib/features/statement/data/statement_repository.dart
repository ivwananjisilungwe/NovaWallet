import 'package:novawallet_app/core/network/api_client.dart';
import 'package:novawallet_app/core/network/api_exception.dart';

import '../models/statement_summary.dart';

/// Backend calls for the statement domain:
///   GET /v1/wallets/{id}/statements?month=yyyy-MM   (monthly summary)
class StatementRepository {
  const StatementRepository(this._api);

  final ApiClient _api;

  /// Monthly summary for [walletId] in `yyyy-MM` format, e.g. `2026-08`.
  Future<StatementSummary> getMonthlyStatement(
    String walletId,
    String month,
  ) async {
    final summary = await _api.get<StatementSummary>(
      '/v1/wallets/$walletId/statements',
      query: {'month': month},
      parser: (json) => StatementSummary.fromJson(json as Map<String, dynamic>),
    );
    if (summary == null) {
      throw const ApiException(message: 'Statement unavailable');
    }
    return summary;
  }
}
