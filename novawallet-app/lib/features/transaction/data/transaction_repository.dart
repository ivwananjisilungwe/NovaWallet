import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';
import '../models/transaction.dart';
import '../models/fee_estimate.dart';

/// Backend calls for the transaction domain:
///   GET /v1/wallets/{id}/transactions   (history)
///   GET /v1/transactions/{reference}    (detail)
///   GET /v1/fees/estimate?type=&amount= (fee estimator)
class TransactionRepository {
  const TransactionRepository(this._api);

  final ApiClient _api;

  Future<List<WalletTransaction>> getHistory(String walletId, {int page = 0, int size = 20}) async {
    final resp = await _api.get<PagedResponse<WalletTransaction>>(
      '/v1/wallets/$walletId/transactions',
      query: {'page': page, 'size': size},
      parser: (json) => PagedResponse.fromJson(
        json as Map<String, dynamic>,
        (item) => WalletTransaction.fromJson(item as Map<String, dynamic>),
      ),
    );
    return resp?.items ?? const [];
  }

  Future<WalletTransaction> getByReference(String reference) async {
    final tx = await _api.get<WalletTransaction>(
      '/v1/transactions/$reference',
      parser: (json) => WalletTransaction.fromJson(json as Map<String, dynamic>),
    );
    if (tx == null) throw const ApiException(message: 'Transaction not found');
    return tx;
  }

  Future<FeeEstimate> estimateFee({
    required String type,
    required String amount,
  }) async {
    final fee = await _api.get<FeeEstimate>(
      '/v1/fees/estimate',
      query: {'type': type, 'amount': amount},
      parser: (json) => FeeEstimate.fromJson(json as Map<String, dynamic>),
    );
    if (fee == null) throw const ApiException(message: 'Fee estimate unavailable');
    return fee;
  }
}
