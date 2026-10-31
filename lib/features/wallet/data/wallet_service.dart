import 'package:novawallet_mobile/core/config/app_config.dart';
import 'package:novawallet_mobile/core/network/authenticated_http_client.dart';
import 'package:novawallet_mobile/features/auth/data/auth_service.dart';
import 'package:novawallet_mobile/features/auth/data/session_store.dart';
import 'package:novawallet_mobile/features/wallet/data/wallet.dart';

abstract class WalletService {
  Future<Wallet> getMyWallet();
}

class HttpWalletService implements WalletService {
  HttpWalletService({
    required this.sessionStore,
    AuthenticatedHttpClient? httpClient,
    String? baseUrl,
  }) : _httpClient =
           httpClient ?? AuthenticatedHttpClient(sessionStore: sessionStore),
       _baseUrl = baseUrl ?? AppConfig.walletBaseUrl;

  final SessionStore sessionStore;
  final AuthenticatedHttpClient _httpClient;
  final String _baseUrl;

  @override
  Future<Wallet> getMyWallet() async {
    try {
      final data = await _httpClient.getJson(Uri.parse('$_baseUrl/me'));
      return Wallet.fromJson(data);
    } on AuthException catch (error) {
      if (error.statusCode == 404) {
        throw const WalletNotFoundException();
      }
      rethrow;
    }
  }
}
