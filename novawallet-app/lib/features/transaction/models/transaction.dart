import '../../../core/utils/formatters.dart';

/// Transaction mirroring backend `TransactionResponse`.
class WalletTransaction {
  const WalletTransaction({
    required this.id,
    required this.reference,
    required this.type,
    required this.amount,
    required this.status,
    this.balanceBefore,
    this.balanceAfter,
    this.description,
    this.senderWalletId,
    this.receiverWalletId,
    this.createdAt,
  });

  final String id;
  final String reference;
  final String type; // DEPOSIT | WITHDRAWAL | TRANSFER_DEBIT | TRANSFER_CREDIT | FEE
  final String amount;
  final String status; // SUCCESSFUL | PENDING | FAILED
  final String? balanceBefore;
  final String? balanceAfter;
  final String? description;
  final String? senderWalletId;
  final String? receiverWalletId;
  final DateTime? createdAt;

  bool get isIncoming =>
      type == 'DEPOSIT' || type == 'TRANSFER_CREDIT';

  bool get isOutgoing =>
      type == 'WITHDRAWAL' || type == 'TRANSFER_DEBIT' || type == 'FEE';

  String get displayTitle {
    switch (type) {
      case 'DEPOSIT':
        return 'Deposit';
      case 'WITHDRAWAL':
        return 'Withdrawal';
      case 'TRANSFER_CREDIT':
        return 'Money received';
      case 'TRANSFER_DEBIT':
        return 'Money sent';
      case 'FEE':
        return 'Platform fee';
      default:
        return description ?? type;
    }
  }

  String get signedAmount {
    final prefix = isIncoming ? '+' : '-';
    return '$prefix${formatZmw(amount, includeSymbol: false)}';
  }

  factory WalletTransaction.fromJson(Map<String, dynamic> json) =>
      WalletTransaction(
        id: (json['id'] ?? '').toString(),
        reference: (json['reference'] ?? '').toString(),
        type: (json['type'] ?? 'DEPOSIT').toString(),
        amount: (json['amount'] ?? '0').toString(),
        status: (json['status'] ?? 'SUCCESSFUL').toString(),
        balanceBefore: json['balanceBefore']?.toString(),
        balanceAfter: json['balanceAfter']?.toString(),
        description: json['description'] as String?,
        senderWalletId: json['senderWalletId']?.toString(),
        receiverWalletId: json['receiverWalletId']?.toString(),
        createdAt: parseBackendDate(json['createdAt'] as String?),
      );
}
