import '../../../core/utils/formatters.dart';

/// Wallet details mirroring backend `WalletResponse`:
/// `{id, accountNumber, balance, currency, status, freezeReason, createdAt}`.
class Wallet {
  const Wallet({
    required this.id,
    required this.accountNumber,
    required this.balance,
    this.currency = 'ZMW',
    this.status = 'ACTIVE',
    this.freezeReason,
    this.createdAt,
  });

  final String id;
  final String accountNumber;
  final String balance; // raw decimal string
  final String currency;
  final String status;
  final String? freezeReason;
  final DateTime? createdAt;

  bool get isFrozen => status == 'FROZEN';

  /// `NW-100001` -> `NW •••0001` for display.
  String get maskedAccount {
    final digits = accountNumber.replaceAll(RegExp(r'[^0-9]'), '');
    if (digits.length < 4) return accountNumber;
    final last4 = digits.substring(digits.length - 4);
    return 'NW •••$last4';
  }

  factory Wallet.fromJson(Map<String, dynamic> json) => Wallet(
        id: (json['id'] ?? '').toString(),
        accountNumber: (json['accountNumber'] ?? '').toString(),
        balance: (json['balance'] ?? '0').toString(),
        currency: (json['currency'] ?? 'ZMW').toString(),
        status: (json['status'] ?? 'ACTIVE').toString(),
        freezeReason: json['freezeReason'] as String?,
        createdAt: parseBackendDate(json['createdAt'] as String?),
      );
}
