/// Domain model for the verified `WalletResponse`.
///
/// Balance is kept as a [String] to preserve monetary precision; it is never
/// converted through floating-point arithmetic.
class Wallet {
  const Wallet({
    required this.id,
    required this.accountNumber,
    required this.balance,
    required this.currency,
    required this.status,
    this.freezeReason,
    this.createdAt,
  });

  final String id;
  final String accountNumber;
  final String balance;
  final String currency;
  final String status;
  final String? freezeReason;
  final String? createdAt;

  factory Wallet.fromJson(Map<String, dynamic> json) {
    return Wallet(
      id: (json['id'] as String?) ?? '',
      accountNumber: (json['accountNumber'] as String?) ?? '',
      balance: _stringify(json['balance']),
      currency: (json['currency'] as String?) ?? 'ZMW',
      status: (json['status'] as String?) ?? 'ACTIVE',
      freezeReason: json['freezeReason'] as String?,
      createdAt: json['createdAt']?.toString(),
    );
  }

  static String _stringify(Object? value) {
    if (value == null) return '0';
    if (value is String) return value;
    // Use toString to avoid double conversion; backend sends BigDecimal/number.
    return value.toString();
  }
}

/// Thrown when the backend reports no wallet exists for the user (404).
class WalletNotFoundException implements Exception {
  const WalletNotFoundException([this.message = 'Wallet not found.']);

  final String message;

  @override
  String toString() => 'WalletNotFoundException: $message';
}
