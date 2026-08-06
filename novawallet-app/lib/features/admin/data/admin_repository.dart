import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/providers.dart';

/// Pending KYC item from `GET /v1/admin/kyc/pending`.
class KycQueueItem {
  const KycQueueItem({
    required this.userId,
    required this.name,
    required this.email,
    required this.tier,
  });

  final String userId;
  final String name;
  final String email;
  final String tier;

  factory KycQueueItem.fromJson(Map<String, dynamic> json) => KycQueueItem(
        userId: (json['userId'] ?? json['id'] ?? '').toString(),
        name: (json['fullName'] ?? json['firstName'] ?? 'User').toString(),
        email: (json['email'] ?? '').toString(),
        tier: (json['tier'] ?? json['kycTier'] ?? '').toString(),
      );
}

/// Backend calls for the admin console (all ADMIN-gated).
class AdminRepository {
  const AdminRepository(this._api);

  final ApiClient _api;

  Future<List<KycQueueItem>> pendingKyc() async {
    final items = await _api.get<List<KycQueueItem>>(
      '/v1/admin/kyc/pending',
      parser: (json) => (json as List)
          .map((e) => KycQueueItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
    return items ?? const [];
  }

  Future<void> approveKyc(String userId, {required int tier}) async {
    await _api.post<dynamic>(
      '/v1/admin/kyc/$userId/approve',
      body: {'tier': tier},
    );
  }

  Future<void> rejectKyc(String userId, {required String reason}) async {
    await _api.post<dynamic>(
      '/v1/admin/kyc/$userId/reject',
      body: {'reason': reason},
    );
  }
}

final adminRepositoryProvider = Provider<AdminRepository>(
  (ref) => AdminRepository(ref.watch(apiClientProvider)),
);
