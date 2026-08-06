import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/nova_colors.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/widgets.dart';
import '../providers/transaction_provider.dart';

/// Transaction detail card (per design 09). Fetched by reference.
class TransactionDetailScreen extends ConsumerWidget {
  const TransactionDetailScreen({super.key, required this.reference});

  final String reference;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final txAsync = ref.watch(transactionByReferenceProvider(reference));

    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('Transaction')),
      body: SafeArea(
        child: txAsync.when(
          loading: () => const LoadingView(message: 'Loading…'),
          error: (e, _) => ErrorStateView(message: '$e'),
          data: (tx) => SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: 12),
                CircleAvatar(
                  radius: 40,
                  backgroundColor: tx.isIncoming
                      ? NovaColors.secondaryContainer
                      : NovaColors.errorContainer,
                  child: Icon(
                    tx.isIncoming ? Icons.arrow_downward : Icons.arrow_upward,
                    color: tx.isIncoming ? NovaColors.onSecondaryContainer : NovaColors.error,
                    size: 32,
                  ),
                ),
                const SizedBox(height: 12),
                Text(
                  tx.displayTitle,
                  textAlign: TextAlign.center,
                  style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600, color: NovaColors.onSurface),
                ),
                const SizedBox(height: 8),
                Text(
                  formatZmw(tx.amount),
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 32,
                    fontWeight: FontWeight.w700,
                    color: tx.isIncoming ? NovaColors.success : NovaColors.error,
                    letterSpacing: -0.02,
                  ),
                ),
                const SizedBox(height: 24),
                StatusChip(
                  label: tx.status,
                  color: tx.status == 'SUCCESSFUL'
                      ? NovaColors.success
                      : (tx.status == 'PENDING' ? NovaColors.warning : NovaColors.error),
                ),
                const SizedBox(height: 24),
                _DetailRow(label: 'Reference', value: tx.reference),
                _DetailRow(label: 'Type', value: tx.type),
                _DetailRow(label: 'Date', value: formatDateTime(tx.createdAt)),
                if (tx.description != null)
                  _DetailRow(label: 'Description', value: tx.description!),
                if (tx.balanceBefore != null)
                  _DetailRow(label: 'Balance before', value: formatZmw(tx.balanceBefore)),
                if (tx.balanceAfter != null)
                  _DetailRow(label: 'Balance after', value: formatZmw(tx.balanceAfter)),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  const _DetailRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(color: NovaColors.onSurfaceVariant, fontSize: 14)),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: const TextStyle(fontWeight: FontWeight.w600, color: NovaColors.onSurface),
            ),
          ),
        ],
      ),
    );
  }
}
