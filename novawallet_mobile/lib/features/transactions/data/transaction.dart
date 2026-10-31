/// Domain model for the verified `TransactionResponse`.
///
/// Monetary values are kept as [String]s to preserve precision; they are
/// never converted through floating-point arithmetic.
class Transaction {
  const Transaction({
    required this.id,
    required this.reference,
    required this.type,
    required this.amount,
    this.balanceBefore,
    this.balanceAfter,
    required this.status,
    this.description,
    this.senderWalletId,
    this.receiverWalletId,
    this.createdAt,
    this.currency,
  });

  final String id;
  final String reference;
  final String type;
  final String amount;
  final String? balanceBefore;
  final String? balanceAfter;
  final String status;
  final String? description;
  final String? senderWalletId;
  final String? receiverWalletId;
  final String? createdAt;
  final String? currency;

  /// Whether money moved into the viewed wallet.
  bool get isCredit => type == 'DEPOSIT' || type == 'TRANSFER_CREDIT';

  /// Best-effort parse of [createdAt] into a [DateTime].
  DateTime? get createdAtDate {
    if (createdAt == null || createdAt!.isEmpty) return null;
    try {
      return DateTime.parse(createdAt!);
    } catch (_) {
      return null;
    }
  }

  factory Transaction.fromJson(Map<String, dynamic> json) {
    return Transaction(
      id: (json['id'] as String?) ?? json['id']?.toString() ?? '',
      reference: (json['reference'] as String?) ?? '',
      type: (json['type'] as String?) ?? '',
      amount: _stringify(json['amount']),
      balanceBefore: json['balanceBefore'] == null
          ? null
          : _stringify(json['balanceBefore']),
      balanceAfter: json['balanceAfter'] == null
          ? null
          : _stringify(json['balanceAfter']),
      status: (json['status'] as String?) ?? '',
      description: json['description'] as String?,
      senderWalletId: json['senderWalletId']?.toString(),
      receiverWalletId: json['receiverWalletId']?.toString(),
      createdAt: json['createdAt']?.toString(),
      currency: json['currency']?.toString(),
    );
  }

  static String _stringify(Object? value) {
    if (value == null) return '0';
    if (value is String) return value;
    return value.toString();
  }
}
