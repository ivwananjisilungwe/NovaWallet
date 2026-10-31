import 'package:novawallet_mobile/core/config/app_config.dart';
import 'package:novawallet_mobile/core/network/authenticated_http_client.dart';
import 'package:novawallet_mobile/features/auth/data/session_store.dart';
import 'package:novawallet_mobile/features/user/data/user_profile.dart';

abstract class UserService {
  Future<UserProfile> getProfile();
}

class HttpUserService implements UserService {
  HttpUserService({
    required this.sessionStore,
    AuthenticatedHttpClient? httpClient,
    String? baseUrl,
  }) : _httpClient =
           httpClient ?? AuthenticatedHttpClient(sessionStore: sessionStore),
       _baseUrl = baseUrl ?? AppConfig.userBaseUrl;

  final SessionStore sessionStore;
  final AuthenticatedHttpClient _httpClient;
  final String _baseUrl;

  @override
  Future<UserProfile> getProfile() async {
    final data = await _httpClient.getJson(Uri.parse('$_baseUrl/me'));
    return UserProfile.fromJson(data);
  }
}
