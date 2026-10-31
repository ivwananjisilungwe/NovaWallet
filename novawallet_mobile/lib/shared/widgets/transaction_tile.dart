import 'package:flutter/material.dart';

import 'package:novawallet_mobile/core/design/app_tokens.dart';
import 'package:novawallet_mobile/features/transactions/presentation/transaction_model.dart';

class TransactionTile extends StatelessWidget {
  const TransactionTile({
    required this.transaction,
    this.onTap,
    this.currency,
    super.key,
  });

  final Object transaction;
  final VoidCallback? onTap;
  final String? currency;

  String get _title {
    if (transaction is SampleTransaction) {
      return (transaction as SampleTransaction).title;
    }
    final tx = transaction as Transaction;
    return tx.description?.isNotEmpty == true ? tx.description! : tx.reference;
  }

  String get _subtitle {
    if (transaction is SampleTransaction) {
      return (transaction as SampleTransaction).subtitle;
    }
    final tx = transaction as Transaction;
    return tx.createdAt ?? '';
  }

  String get _status {
    if (transaction is SampleTransaction) {
      return (transaction as SampleTransaction).status;
    }
    return (transaction as Transaction).status;
  }

  String get _amount {
    if (transaction is SampleTransaction) {
      return (transaction as SampleTransaction).amount;
    }
    final tx = transaction as Transaction;
    final prefix = tx.isCredit ? '+' : '−';
    final effectiveCurrency = tx.currency ?? currency;
    return '$prefix ${effectiveCurrency == null || effectiveCurrency.isEmpty ? '' : '$effectiveCurrency '}${tx.amount}';
  }

  IconData get _icon {
    if (transaction is SampleTransaction) {
      final kind = (transaction as SampleTransaction).kind;
      return switch (kind) {
        TransactionKind.purchase => Icons.shopping_bag_outlined,
        TransactionKind.received => Icons.south_west_rounded,
        TransactionKind.payment => Icons.phone_android_rounded,
      };
    }
    final tx = transaction as Transaction;
    return tx.isCredit ? Icons.south_west_rounded : Icons.north_east_rounded;
  }

  Color _tint(ColorScheme colors) {
    if (transaction is SampleTransaction) {
      final kind = (transaction as SampleTransaction).kind;
      return switch (kind) {
        TransactionKind.purchase => colors.secondary,
        TransactionKind.received => colors.tertiary,
        TransactionKind.payment => colors.primary,
      };
    }
    final tx = transaction as Transaction;
    return tx.isCredit ? colors.tertiary : colors.primary;
  }

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final tile = Padding(
      padding: const EdgeInsets.symmetric(vertical: AppTokens.space8),
      child: Row(
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: colors.surfaceContainerHigh,
              borderRadius: BorderRadius.circular(AppTokens.radiusMedium),
            ),
            child: Icon(_icon, color: _tint(colors)),
          ),
          const SizedBox(width: AppTokens.space12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(_title, style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: AppTokens.space2),
                Text(_subtitle, style: Theme.of(context).textTheme.bodySmall),
                Text(
                  _status,
                  style: Theme.of(context).textTheme.labelSmall
                      ?.copyWith(color: colors.onSurfaceVariant),
                ),
              ],
            ),
          ),
          const SizedBox(width: AppTokens.space8),
          Flexible(
            child: Text(
              _amount,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              textAlign: TextAlign.right,
              style: Theme.of(context).textTheme.titleSmall
                  ?.copyWith(fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
    return onTap == null
        ? tile
        : InkWell(
            onTap: onTap,
            borderRadius: BorderRadius.circular(AppTokens.radiusMedium),
            child: tile,
          );
  }
}
