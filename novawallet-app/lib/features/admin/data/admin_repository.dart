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

/// Transaction types that carry a fee (mirrors backend `FeeType` enum).
enum FeeType {
  deposit('DEPOSIT', 'Deposit'),
  withdrawal('WITHDRAWAL', 'Withdrawal'),
  transfer('TRANSFER', 'Transfer');

  const FeeType(this.apiValue, this.label);

  /// Wire value sent to / received from the backend (`FeeType.name`).
  final String apiValue;
  final String label;

  static FeeType fromApi(String value) =>
      FeeType.values.asNameMap()[value] ??
      FeeType.values.firstWhere(
        (t) => t.apiValue == value,
        orElse: () => FeeType.deposit,
      );
}

/// Fee rule for one transaction type, from `GET /v1/admin/fees`.
///
/// Fee formula (backend): `totalFee = (amount * percentageFee / 100) + flatFee`,
/// clamped by `minFee` / `maxFee`. `percentageFee` is the percentage number
/// itself (e.g. `0.5` means 0.5%).
class FeeConfig {
  const FeeConfig({
    required this.id,
    required this.transactionType,
    required this.percentageFee,
    required this.flatFee,
    required this.active,
    this.minFee,
    this.maxFee,
  });

  final String id;
  final FeeType transactionType;
  final double percentageFee;
  final double flatFee;
  final double? minFee;
  final double? maxFee;
  final bool active;

  factory FeeConfig.fromJson(Map<String, dynamic> json) => FeeConfig(
        id: (json['id'] ?? '').toString(),
        transactionType:
            FeeType.fromApi((json['transactionType'] ?? '').toString()),
        percentageFee: _nonNullDouble(json['percentageFee']),
        flatFee: _nonNullDouble(json['flatFee']),
        minFee: _nullableDouble(json['minFee']),
        maxFee: _nullableDouble(json['maxFee']),
        active: json['active'] == true,
      );
}

double? _nullableDouble(dynamic raw) {
  if (raw == null) return null;
  if (raw is num) return raw.toDouble();
  return double.tryParse(raw.toString());
}

double _nonNullDouble(dynamic raw) => _nullableDouble(raw) ?? 0.0;

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

  // ---------------------------------------------------------------------------
  // Fee configurations (backend `FeeController`).
  // ---------------------------------------------------------------------------

  /// Lists every fee configuration (`GET /v1/admin/fees`).
  Future<List<FeeConfig>> getFeeConfigurations() async {
    final items = await _api.get<List<FeeConfig>>(
      '/v1/admin/fees',
      parser: (json) => (json as List)
          .map((e) => FeeConfig.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
    return items ?? const [];
  }

  /// Creates a fee configuration (`POST /v1/admin/fees`). The backend
  /// always activates new configurations, so [active] is included for
  /// symmetry with the update payload but may be ignored on create.
  Future<FeeConfig?> createFeeConfiguration({
    required FeeType transactionType,
    required double percentageFee,
    required double flatFee,
    double? minFee,
    double? maxFee,
    bool active = true,
  }) async {
    final body = <String, dynamic>{
      'transactionType': transactionType.apiValue,
      'percentageFee': percentageFee,
      'flatFee': flatFee,
      'minFee': ?minFee,
      'maxFee': ?maxFee,
      'active': active,
    };
    return _api.post<FeeConfig>(
      '/v1/admin/fees',
      body: body,
      parser: (json) => FeeConfig.fromJson(json as Map<String, dynamic>),
    );
  }

  /// Updates a fee configuration (`PUT /v1/admin/fees/{id}`).
  Future<FeeConfig?> updateFeeConfiguration(
    String id, {
    required FeeType transactionType,
    required double percentageFee,
    required double flatFee,
    double? minFee,
    double? maxFee,
    required bool active,
  }) async {
    final body = <String, dynamic>{
      'transactionType': transactionType.apiValue,
      'percentageFee': percentageFee,
      'flatFee': flatFee,
      'minFee': ?minFee,
      'maxFee': ?maxFee,
      'active': active,
    };
    return _api.put<FeeConfig>(
      '/v1/admin/fees/$id',
      body: body,
      parser: (json) => FeeConfig.fromJson(json as Map<String, dynamic>),
    );
  }

  /// Deletes a fee configuration (`DELETE /v1/admin/fees/{id}`).
  Future<void> deleteFeeConfiguration(String id) async {
    await _api.delete<dynamic>('/v1/admin/fees/$id');
  }
}

final adminRepositoryProvider = Provider<AdminRepository>(
  (ref) => AdminRepository(ref.watch(apiClientProvider)),
);
