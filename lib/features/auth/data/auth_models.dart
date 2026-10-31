/// NovaWallet authentication domain models.
///
/// Fields mirror the Spring Boot `novawallet-api` auth contract:
///   LoginRequest → POST /api/v1/auth/login {email, password}
///   AuthResponse → {accessToken, refreshToken, expiresIn, user}
///   UserInfo → {id, firstName, lastName, email, phone, role, emailVerified, pinSet}
///
/// JSON parsing is defensive: optional fields tolerate null, so the client
/// does not crash if a future backend release omits a field.
abstract final class AuthModels {
  AuthModels._();
}

/// Authenticated user information returned by the login endpoint.
class AuthUser {
  const AuthUser({
    required this.id,
    required this.firstName,
    required this.lastName,
    required this.email,
    this.phone,
    required this.role,
    required this.emailVerified,
    required this.pinSet,
  });

  final String id;
  final String firstName;
  final String lastName;
  final String email;
  final String? phone;
  final String role;
  final bool emailVerified;
  final bool pinSet;

  /// Display name, falling back to the email address when names are absent.
  String get displayName => (firstName.isNotEmpty || lastName.isNotEmpty)
      ? '$firstName $lastName'.trim()
      : email;

  factory AuthUser.fromJson(Map<String, dynamic> json) {
    return AuthUser(
      id: (json['id'] as String?) ?? '',
      firstName: (json['firstName'] as String?) ?? '',
      lastName: (json['lastName'] as String?) ?? '',
      email: (json['email'] as String?) ?? '',
      phone: json['phone'] as String?,
      role: (json['role'] as String?) ?? 'USER',
      emailVerified: json['emailVerified'] is bool
          ? json['emailVerified'] as bool
          : false,
      pinSet: json['pinSet'] is bool ? json['pinSet'] as bool : false,
    );
  }

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'id': id,
      'firstName': firstName,
      'lastName': lastName,
      'email': email,
      'phone': phone,
      'role': role,
      'emailVerified': emailVerified,
      'pinSet': pinSet,
    };
  }
}

/// The complete successful login response, parsed from the backend envelope.
class AuthSession {
  const AuthSession({
    required this.accessToken,
    required this.refreshToken,
    required this.expiresIn,
    required this.user,
  });

  final String accessToken;
  final String refreshToken;
  final int expiresIn; // milliseconds
  final AuthUser user;

  factory AuthSession.fromJson(Map<String, dynamic> json) {
    return AuthSession(
      accessToken: (json['accessToken'] as String?) ?? '',
      refreshToken: (json['refreshToken'] as String?) ?? '',
      expiresIn: json['expiresIn'] is int
          ? json['expiresIn'] as int
          : int.tryParse('${json['expiresIn']}') ?? 0,
      user: AuthUser.fromJson(
        (json['user'] as Map<String, dynamic>?) ?? const <String, dynamic>{},
      ),
    );
  }

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'accessToken': accessToken,
      'refreshToken': refreshToken,
      'expiresIn': expiresIn,
      'user': user.toJson(),
    };
  }
}
