import 'package:novawallet_mobile/core/config/app_config.dart';
import 'package:novawallet_mobile/core/network/authenticated_http_client.dart';
import 'package:novawallet_mobile/features/auth/data/session_store.dart';
import 'package:novawallet_mobile/features/transactions/data/transaction.dart';

abstract class TransactionService {
  Future<List<Transaction>> getTransactions(
    String walletId, {
    int page = 0,
    int size = 20,
  });

  Future<Transaction> getTransactionByReference(String reference);

  Future<List<Transaction>> getTransactionHistory(
    String walletId, {
    int page = 0,
    int size = 20,
  }) => getTransactions(walletId, page: page, size: size);

  Future<List<Transaction>> fetchTransactions(
    String walletId, {
    int page = 0,
    int size = 20,
  }) => getTransactions(walletId, page: page, size: size);
}

class HttpTransactionService implements TransactionService {
  HttpTransactionService({
    required this.sessionStore,
    AuthenticatedHttpClient? httpClient,
    String? baseUrl,
    String? transactionBaseUrl,
  }) : _httpClient =
           httpClient ?? AuthenticatedHttpClient(sessionStore: sessionStore),
       _baseUrl = baseUrl ?? AppConfig.walletBaseUrl,
       _transactionBaseUrl = transactionBaseUrl ?? AppConfig.transactionBaseUrl;

  final SessionStore sessionStore;
  final AuthenticatedHttpClient _httpClient;
  final String _baseUrl;
  final String _transactionBaseUrl;

  @override
  Future<List<Transaction>> getTransactions(
    String walletId, {
    int page = 0,
    int size = 20,
  }) async {
    final uri = Uri.parse('$_baseUrl/$walletId/transactions').replace(
      queryParameters: <String, String>{'page': '$page', 'size': '$size'},
    );
    final data = await _httpClient.getJson(uri);
    final content = data['content'];
    if (content is List) {
      return content
          .whereType<Map<String, dynamic>>()
          .map(Transaction.fromJson)
          .toList(growable: false);
    }
    // Defensive fallback for envelopes that return a bare list.
    if (data['transactions'] is List) {
      return (data['transactions'] as List)
          .whereType<Map<String, dynamic>>()
          .map(Transaction.fromJson)
          .toList(growable: false);
    }
    return const <Transaction>[];
  }

  @override
  Future<Transaction> getTransactionByReference(String reference) async {
    final data = await _httpClient.getJson(
      Uri.parse('$_transactionBaseUrl/$reference'),
    );
    return Transaction.fromJson(data);
  }

  @override
  Future<List<Transaction>> getTransactionHistory(
    String walletId, {
    int page = 0,
    int size = 20,
  }) => getTransactions(walletId, page: page, size: size);

  @override
  Future<List<Transaction>> fetchTransactions(
    String walletId, {
    int page = 0,
    int size = 20,
  }) => getTransactions(walletId, page: page, size: size);
}
