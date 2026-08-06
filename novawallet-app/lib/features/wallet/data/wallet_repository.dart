import '../../../core/network/api_client.dart';
import '../../transaction/models/transaction.dart';
import '../models/wallet.dart';

/// Backend calls for the wallet domain:
///   GET  /v1/wallets/me               (current user's wallet)
///   GET  /v1/wallets/{id}/balance     (fresh balance)
class WalletRepository {
  const WalletRepository(this._api);

  final ApiClient _api;

  Future<Wallet> getMyWallet() async {
    final wallet = await _api.get<Wallet>(
      '/v1/wallets/me',
      parser: (json) => Wallet.fromJson(json as Map<String, dynamic>),
    );
    if (wallet == null) throw Exception('Wallet not found');
    return wallet;
  }

  Future<Wallet> getBalance(String walletId) async {
    final wallet = await _api.get<Wallet>(
      '/v1/wallets/$walletId/balance',
      parser: (json) => Wallet.fromJson(json as Map<String, dynamic>),
    );
    if (wallet == null) throw Exception('Balance unavailable');
    return wallet;
  }

  Future<WalletTransaction> deposit({
    required String walletId,
    required String amount,
    required String description,
    String? idempotencyKey,
  }) async {
    return _moneyMove(
      walletId: walletId,
      path: '/v1/wallets/$walletId/deposit',
      amount: amount,
      description: description,
      pin: null,
      idempotencyKey: idempotencyKey,
    );
  }

  Future<WalletTransaction> withdraw({
    required String walletId,
    required String amount,
    required String description,
    required String pin,
    String? idempotencyKey,
  }) async {
    return _moneyMove(
      walletId: walletId,
      path: '/v1/wallets/$walletId/withdraw',
      amount: amount,
      description: description,
      pin: pin,
      idempotencyKey: idempotencyKey,
    );
  }

  Future<WalletTransaction> transfer({
    required String receiverWalletId,
    required String amount,
    required String description,
    required String pin,
    String? idempotencyKey,
  }) async {
    final txn = await _api.post<WalletTransaction>(
      '/v1/transfers',
      body: {
        'receiverWalletId': receiverWalletId,
        'amount': amount,
        'pin': pin,
        'description': description,
      },
      parser: (json) => WalletTransaction.fromJson(json as Map<String, dynamic>),
      idempotencyKey: idempotencyKey,
    );
    if (txn == null) throw Exception('Transfer failed');
    return txn;
  }

  Future<WalletTransaction> _moneyMove({
    required String walletId,
    required String path,
    required String amount,
    required String description,
    String? pin,
    String? idempotencyKey,
  }) async {
    final txn = await _api.post<WalletTransaction>(
      path,
      body: {
        'amount': amount,
        'description': description,
        'pin': ?pin,
      },
      parser: (json) => WalletTransaction.fromJson(json as Map<String, dynamic>),
      idempotencyKey: idempotencyKey,
    );
    if (txn == null) throw Exception('Request failed');
    return txn;
  }
}
