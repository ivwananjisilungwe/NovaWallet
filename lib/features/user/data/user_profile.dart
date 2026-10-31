/// Domain model for the verified `UserProfileResponse`.
class UserProfile {
  const UserProfile({
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

  String get displayName => (firstName.isNotEmpty || lastName.isNotEmpty)
      ? '$firstName $lastName'.trim()
      : email;

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
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
}
