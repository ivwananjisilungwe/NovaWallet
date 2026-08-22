import 'package:flutter/material.dart';

import '../../../core/theme/nova_colors.dart';

/// Balance indicator at top of send form.
class SendBalanceIndicator extends StatelessWidget {
  const SendBalanceIndicator({
    super.key,
    required this.balance,
    required this.isLoading,
  });

  final double balance;
  final bool isLoading;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: NovaColors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: NovaColors.outline.withValues(alpha: 0.2),
          width: 1,
        ),
      ),
      child: Row(
        children: [
          Icon(
            Icons.account_balance_wallet,
            color: NovaColors.primary,
            size: 24,
          ),
          const SizedBox(width: 12),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Available Balance',
                style: TextStyle(
                  fontSize: 12,
                  color: NovaColors.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 2),
              isLoading
                  ? const SizedBox(
                      height: 16,
                      width: 80,
                      child: LinearProgressIndicator(),
                    )
                  : Text(
                      'ZMW ${balance.toStringAsFixed(2)}',
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                        color: NovaColors.onSurface,
                      ),
                    ),
            ],
          ),
        ],
      ),
    );
  }
}
