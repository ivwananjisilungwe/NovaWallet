import 'package:flutter/material.dart';

import '../theme/nova_colors.dart';
import 'pill_button.dart';

/// Indigo gradient balance card — the anchor of the dashboard.
/// Mirrors the home-dashboard hero card: masked account number, action
/// buttons, and a subtle FROZEN badge when [frozen] is true.
class BalanceCard extends StatelessWidget {
  const BalanceCard({
    super.key,
    required this.balanceLabel,
    required this.balanceAmount,
    this.accountNumber,
    this.frozen = false,
    this.onDeposit,
    this.onWithdraw,
  });

  final String balanceLabel;
  final String balanceAmount;

  /// e.g. `NW ••••0421`
  final String? accountNumber;
  final bool frozen;
  final VoidCallback? onDeposit;
  final VoidCallback? onWithdraw;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [NovaColors.primary, NovaColors.primaryContainer],
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: NovaColors.shadowTint.withValues(alpha: 0.25),
            blurRadius: 20,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Balance row: label + amount + masked account + copy hint.
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      balanceLabel,
                      style: TextStyle(
                        fontSize: 14,
                        color: NovaColors.onPrimary.withValues(alpha: 0.9),
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      balanceAmount,
                      style: const TextStyle(
                        fontSize: 28,
                        fontWeight: FontWeight.w700,
                        letterSpacing: -0.02,
                        color: NovaColors.onPrimary,
                      ),
                    ),
                  ],
                ),
                if (accountNumber != null)
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 6,
                    ),
                    decoration: BoxDecoration(
                      color: NovaColors.onPrimary.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(999),
                    ),
                    child: Row(
                      children: [
                        Text(
                          accountNumber!,
                          style: const TextStyle(
                            fontSize: 13,
                            color: NovaColors.onPrimary,
                          ),
                        ),
                        const SizedBox(width: 4),
                        const Icon(
                          Icons.content_copy,
                          size: 14,
                          color: NovaColors.onPrimary,
                        ),
                      ],
                    ),
                  ),
              ],
            ),
            if (frozen) ...[
              const SizedBox(height: 8),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  color: NovaColors.warning,
                  borderRadius: BorderRadius.circular(999),
                ),
                child: const Text(
                  'FROZEN',
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 0.5,
                    color: NovaColors.onWarning,
                  ),
                ),
              ),
            ],
            const SizedBox(height: 20),
            // Action row: Deposit (solid white) / Withdraw (outline).
            Row(
              children: [
                Expanded(
                  child: PillButton(
                    label: 'Deposit',
                    variant: PillButtonVariant.filled,
                    expanded: false,
                    onPressed: onDeposit,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: PillButton(
                    label: 'Withdraw',
                    variant: PillButtonVariant.outlined,
                    expanded: false,
                    onPressed: onWithdraw,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
