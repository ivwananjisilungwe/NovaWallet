import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';

/// User profile mirroring backend `UserProfileResponse`.
class UserProfile {
  const UserProfile({
    required this.id,
    this.firstName,
    this.lastName,
    this.email,
    this.phone,
    this.role = 'USER',
    this.emailVerified = false,
    this.pinSet = false,
  });

  final String id;
  final String? firstName;
  final String? lastName;
  final String? email;
  final String? phone;
  final String role;
  final bool emailVerified;
  final bool pinSet;

  bool get isAdmin => role == 'ADMIN';
  String get fullName => [firstName, lastName].whereType<String>().join(' ');

  factory UserProfile.fromJson(Map<String, dynamic> json) => UserProfile(
        id: (json['id'] ?? '').toString(),
        firstName: json['firstName'] as String?,
        lastName: json['lastName'] as String?,
        email: json['email'] as String?,
        phone: json['phone'] as String?,
        role: (json['role'] ?? 'USER').toString(),
        emailVerified: json['emailVerified'] as bool? ?? false,
        pinSet: json['pinSet'] as bool? ?? false,
      );

  Map<String, dynamic> toUpdateJson() => {
        if (firstName != null) 'firstName': firstName,
        if (lastName != null) 'lastName': lastName,
        if (phone != null) 'phone': phone,
      };
}

/// Backend calls for the user domain:
///   GET /v1/users/me
///   PUT /v1/users/me
class UserRepository {
  const UserRepository(this._api);

  final ApiClient _api;

  Future<UserProfile> getProfile() async {
    final profile = await _api.get<UserProfile>(
      '/v1/users/me',
      parser: (json) => UserProfile.fromJson(json as Map<String, dynamic>),
    );
    if (profile == null) throw const ApiException(message: 'Profile unavailable');
    return profile;
  }

  Future<UserProfile> updateProfile(UserProfile profile) async {
    final updated = await _api.put<UserProfile>(
      '/v1/users/me',
      body: profile.toUpdateJson(),
      parser: (json) => UserProfile.fromJson(json as Map<String, dynamic>),
    );
    if (updated == null) throw const ApiException(message: 'Update failed');
    return updated;
  }
}
