import '../../../core/utils/formatters.dart';

/// KYC status overview mirroring backend `KycStatusResponse`.
class KycStatus {
  const KycStatus({
    required this.kycStatus,
    required this.currentTier,
    this.tierName,
    this.walletLimit,
    this.dailySendLimit,
    this.documents = const [],
    this.submittedAt,
    this.approvedAt,
    this.rejectionReason,
  });

  final String kycStatus; // NOT_SUBMITTED | PENDING | APPROVED | REJECTED
  final int currentTier;
  final String? tierName;
  final String? walletLimit;
  final String? dailySendLimit;
  final List<KycDocument> documents;
  final DateTime? submittedAt;
  final DateTime? approvedAt;
  final String? rejectionReason;

  bool get isApproved => kycStatus == 'APPROVED';
  bool get isPending => kycStatus == 'PENDING';
  bool get isRejected => kycStatus == 'REJECTED';
  bool get isNotSubmitted => kycStatus == 'NOT_SUBMITTED';

  factory KycStatus.fromJson(Map<String, dynamic> json) => KycStatus(
        kycStatus: (json['kycStatus'] ?? 'NOT_SUBMITTED').toString(),
        currentTier: json['currentTier'] as int? ?? 0,
        tierName: json['tierName'] as String?,
        walletLimit: json['walletLimit']?.toString(),
        dailySendLimit: json['dailySendLimit']?.toString(),
        documents: (json['documents'] as List<dynamic>? ?? const [])
            .map((e) => KycDocument.fromJson(e as Map<String, dynamic>))
            .toList(),
        submittedAt: parseBackendDate(json['submittedAt'] as String?),
        approvedAt: parseBackendDate(json['approvedAt'] as String?),
        rejectionReason: json['rejectionReason'] as String?,
      );
}

/// Individual KYC document, mirroring `KycDocumentResponse`.
class KycDocument {
  const KycDocument({
    required this.id,
    required this.documentType,
    this.fileName,
    this.status,
    this.rejectionReason,
  });

  final String id;
  final String documentType; // NATIONAL_ID | PASSPORT | ...
  final String? fileName;
  final String? status;
  final String? rejectionReason;

  factory KycDocument.fromJson(Map<String, dynamic> json) => KycDocument(
        id: (json['id'] ?? '').toString(),
        documentType: (json['documentType'] ?? '').toString(),
        fileName: json['fileName'] as String?,
        status: json['status']?.toString(),
        rejectionReason: json['rejectionReason'] as String?,
      );
}
