import 'package:flutter/material.dart';

import '../../../core/theme/nova_colors.dart';
import '../models/fee_estimate.dart';

/// Fee estimate card with amount breakdown.
class SendFeeEstimateCard extends StatelessWidget {
  const SendFeeEstimateCard({
    super.key,
    required this.feeEstimate,
    required this.isLoading,
    required this.error,
    required this.hasInsufficientBalance,
    required this.total,
  });

  final FeeEstimate? feeEstimate;
  final bool isLoading;
  final String? error;
  final bool hasInsufficientBalance;
  final double total;

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: NovaColors.surfaceContainerLow,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Row(
          children: [
            SizedBox(
              width: 16,
              height: 16,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: NovaColors.primary,
              ),
            ),
            const SizedBox(width: 12),
            Text(
              'Calculating fee...',
              style: TextStyle(color: NovaColors.onSurfaceVariant),
            ),
          ],
        ),
      );
    }

    if (error != null) {
      return Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: NovaColors.errorContainer,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Row(
          children: [
            Icon(Icons.info_outline, color: NovaColors.error, size: 20),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                error!,
                style: TextStyle(color: NovaColors.onErrorContainer),
              ),
            ),
          ],
        ),
      );
    }

    if (feeEstimate == null) return const SizedBox.shrink();

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: hasInsufficientBalance
            ? NovaColors.errorContainer.withValues(alpha: 0.5)
            : NovaColors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(16),
        border: hasInsufficientBalance
            ? Border.all(
                color: NovaColors.error.withValues(alpha: 0.5),
                width: 1,
              )
            : null,
      ),
      child: Column(
        children: [
          _FeeRow(label: 'Fee', value: 'ZMW ${feeEstimate!.totalFee}'),
          if (hasInsufficientBalance) ...[
            const SizedBox(height: 8),
            Row(
              children: [
                Icon(Icons.warning_amber, color: NovaColors.error, size: 16),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Insufficient balance (total: ZMW ${total.toStringAsFixed(2)})',
                    style: TextStyle(
                      color: NovaColors.error,
                      fontSize: 12,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

/// Fee row item.
class _FeeRow extends StatelessWidget {
  const _FeeRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: TextStyle(color: NovaColors.onSurfaceVariant, fontSize: 14),
        ),
        Text(
          value,
          style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
        ),
      ],
    );
  }
}
