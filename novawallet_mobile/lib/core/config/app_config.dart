/// NovaWallet runtime configuration.
///
/// Backend contract (verified against the Spring Boot `novawallet-api`
/// project):
///   POST `baseUrl/api/v1/auth/login`
///   Request body: `{"email": String, "password": String}`
///   Success 200: `ApiResponse<AuthResponse>` containing accessToken,
///   refreshToken, expiresIn, and authenticated user details.
///   Errors: `ErrorResponse {status, code, message, timestamp, path?, errors?}`
///     — 401 invalid credentials, 429 rate-limited, 400 validation,
///     5xx unexpected.
///
/// The backend dev profile runs on localhost:8080 (Spring Boot default — no
/// server.port override exists in application.yml). Host loopback does not
/// reach the backend from an Android emulator; point this at
/// http://10.0.2.2:8080 or the host LAN IP when testing on an emulator or
/// physical device.
abstract final class AppConfig {
  AppConfig._();

  /// Configured backend base URL, excluding the API path prefix.
  static const String backendBaseUrl = 'http://localhost:8080';

  /// Full base for unauthenticated auth endpoints.
  static const String authBaseUrl = '$backendBaseUrl/api/v1/auth';

  /// Full base for unauthenticated email-verification endpoints.
  static const String emailBaseUrl = '$backendBaseUrl/api/v1/email';

  /// Full base for protected user endpoints.
  static const String userBaseUrl = '$backendBaseUrl/api/v1/users';

  /// Full base for protected wallet endpoints.
  static const String walletBaseUrl = '$backendBaseUrl/api/v1/wallets';

  /// Full base for protected transaction endpoints.
  static const String transactionBaseUrl =
      '$backendBaseUrl/api/v1/transactions';
}
