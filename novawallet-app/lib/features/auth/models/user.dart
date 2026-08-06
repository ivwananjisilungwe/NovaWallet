/// Authenticated user profile, mirroring backend `UserSummaryResponse` /
/// `UserProfileResponse` fields actually exposed by `GET /v1/users/me`.
class User {
  const User({
    required this.id,
    this.firstName,
    this.lastName,
    this.email,
    this.phone,
    this.role = 'USER',
    this.kycStatus,
    this.kycTier,
    this.walletId,
    this.accountNumber,
    this.balanceZmw,
    this.walletStatus,
  });

  final String id;
  final String? firstName;
  final String? lastName;
  final String? email;
  final String? phone;

  /// `USER` or `ADMIN`.
  final String role;
  final String? kycStatus;
  final int? kycTier;

  // Wallet summary (present after KYC approval).
  final String? walletId;
  final String? accountNumber;
  final String? balanceZmw;

  /// `ACTIVE` | `FROZEN`.
  final String? walletStatus;

  String get fullName {
    final parts = [firstName, lastName].whereType<String>();
    return parts.isEmpty ? 'there' : parts.join(' ');
  }

  String get initials {
    final parts = [firstName, lastName].whereType<String>().take(2);
    return parts.map((p) => p.isEmpty ? '' : p[0].toUpperCase()).join();
  }

  bool get isAdmin => role == 'ADMIN';
  bool get isFrozen => walletStatus == 'FROZEN';

  factory User.fromJson(Map<String, dynamic> json) {
    final wallet = json['wallet'];
    return User(
      id: (json['id'] ?? json['userId'] ?? '').toString(),
      firstName: json['firstName'] as String?,
      lastName: json['lastName'] as String?,
      email: json['email'] as String?,
      phone: json['phone'] as String?,
      role: json['role'] as String? ?? 'USER',
      kycStatus: json['kycStatus'] as String?,
      kycTier: json['kycTier'] as int?,
      walletId: wallet is Map<String, dynamic>
          ? wallet['id']?.toString()
          : json['walletId']?.toString(),
      accountNumber: wallet is Map<String, dynamic>
          ? wallet['accountNumber'] as String?
          : json['accountNumber'] as String?,
      balanceZmw: wallet is Map<String, dynamic>
          ? wallet['balance']?.toString()
          : json['balance']?.toString(),
      walletStatus: wallet is Map<String, dynamic>
          ? wallet['status'] as String?
          : json['walletStatus'] as String?,
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'firstName': firstName,
        'lastName': lastName,
        'email': email,
        'phone': phone,
        'role': role,
        'kycStatus': kycStatus,
        'kycTier': kycTier,
        'walletId': walletId,
        'accountNumber': accountNumber,
        'balance': balanceZmw,
        'walletStatus': walletStatus,
      };
}

/// Session payload returned by register/login/refresh:
/// `{"accessToken", "refreshToken", "user": {...}}`.
class AuthResponse {
  const AuthResponse({
    required this.accessToken,
    required this.refreshToken,
    this.user,
  });

  final String accessToken;
  final String refreshToken;
  final User? user;

  factory AuthResponse.fromJson(Map<String, dynamic> json) => AuthResponse(
        accessToken: json['accessToken'] as String? ?? '',
        refreshToken: json['refreshToken'] as String? ?? '',
        user: json['user'] == null
            ? null
            : User.fromJson(json['user'] as Map<String, dynamic>),
      );
}