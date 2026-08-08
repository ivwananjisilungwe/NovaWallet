/// Monthly statement summary mirroring the backend statement response:
/// `{month, totalDeposits, totalWithdrawals, totalTransfersIn,
///   totalTransfersOut, totalFees, netChange, transactionCount}`.
///
/// All monetary values are raw decimal strings (e.g. `"100.00"`); format them
/// for display with `formatZmw()`.
class StatementSummary {
  const StatementSummary({
    required this.month,
    required this.totalDeposits,
    required this.totalWithdrawals,
    required this.totalTransfersIn,
    required this.totalTransfersOut,
    required this.totalFees,
    required this.netChange,
    required this.transactionCount,
  });

  /// `yyyy-MM`, e.g. `2026-08`.
  final String month;

  final String totalDeposits;
  final String totalWithdrawals;
  final String totalTransfersIn;
  final String totalTransfersOut;
  final String totalFees;

  /// `deposits + transfersIn - withdrawals - transfersOut - fees`.
  final String netChange;

  final int transactionCount;

  /// True when the wallet had any activity in this month.
  bool get hasActivity => transactionCount > 0;

  factory StatementSummary.fromJson(Map<String, dynamic> json) {
    final count = json['transactionCount'];
    return StatementSummary(
      month: (json['month'] ?? '').toString(),
      totalDeposits: (json['totalDeposits'] ?? '0').toString(),
      totalWithdrawals: (json['totalWithdrawals'] ?? '0').toString(),
      totalTransfersIn: (json['totalTransfersIn'] ?? '0').toString(),
      totalTransfersOut: (json['totalTransfersOut'] ?? '0').toString(),
      totalFees: (json['totalFees'] ?? '0').toString(),
      netChange: (json['netChange'] ?? '0').toString(),
      transactionCount: count is num
          ? count.toInt()
          : int.tryParse(count?.toString() ?? '') ?? 0,
    );
  }
}
