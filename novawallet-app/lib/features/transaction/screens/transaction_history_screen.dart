import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/nova_colors.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/widgets.dart';
import '../../wallet/providers/wallet_provider.dart';
import '../providers/transaction_provider.dart'
    show transactionPaginationProvider, TransactionPaginationState;

/// Full transaction history (per design 08) with type filter chips.
class TransactionHistoryScreen extends ConsumerStatefulWidget {
  const TransactionHistoryScreen({super.key});

  @override
  ConsumerState<TransactionHistoryScreen> createState() =>
      _TransactionHistoryScreenState();
}

class _TransactionHistoryScreenState
    extends ConsumerState<TransactionHistoryScreen> {
  String _filter = 'All';

  static const _filters = ['All', 'Deposits', 'Sends', 'Withdrawals'];

  bool _matchesFilter(dynamic tx) {
    switch (_filter) {
      case 'Deposits':
        return tx.type == 'DEPOSIT';
      case 'Sends':
        return tx.type == 'TRANSFER_DEBIT' || tx.type == 'TRANSFER_CREDIT';
      case 'Withdrawals':
        return tx.type == 'WITHDRAWAL';
      default:
        return true;
    }
  }

  @override
  Widget build(BuildContext context) {
    final wallet = ref.watch(walletProvider).valueOrNull;
    final pagination = wallet == null
        ? const TransactionPaginationState()
        : ref.watch(transactionPaginationProvider(wallet.id));

    final filtered = pagination.transactions.where(_matchesFilter).toList();

    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('Transactions')),
      body: RefreshIndicator(
        onRefresh: () async {
          if (wallet != null) {
            await ref
                .read(transactionPaginationProvider(wallet.id).notifier)
                .refresh();
          }
        },
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            // Filter chips row
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: _filters.map((label) {
                  final selected = _filter == label;
                  return Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: FilterChip(
                      label: Text(label),
                      selected: selected,
                      onSelected: (value) {
                        HapticFeedback.selectionClick();
                        setState(() => _filter = label);
                      },
                      selectedColor: NovaColors.primary.withValues(alpha: 0.15),
                      checkmarkColor: NovaColors.primary,
                      labelStyle: TextStyle(
                        fontWeight: FontWeight.w600,
                        color: selected
                            ? NovaColors.primary
                            : NovaColors.onSurfaceVariant,
                      ),
                      side: BorderSide(
                        color: selected
                            ? NovaColors.primary
                            : NovaColors.outlineVariant,
                      ),
                    ),
                  );
                }).toList(),
              ),
            ),
            const SizedBox(height: 16),
            if (filtered.isEmpty && !pagination.isLoadingMore)
              EmptyStateView(
                icon: Icons.receipt_long_outlined,
                title: _filter == 'All'
                    ? 'No transactions yet'
                    : 'No $_filter found',
                message: _filter == 'All'
                    ? 'Your activity will appear here.'
                    : 'Try a different filter.',
              )
            else
              Column(
                children: [
                  SectionHeader(
                    title: _filter == 'All' ? 'All activity' : _filter,
                  ),
                  const SizedBox(height: 12),
                  ...filtered.map((tx) {
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: TransactionTile(
                        title: tx.displayTitle,
                        subtitle: formatRelativeDate(tx.createdAt),
                        amountText: formatZmw(tx.amount),
                        incoming: tx.isIncoming,
                        heroTag: tx.reference,
                        onTap: () =>
                            context.push('/transactions/${tx.reference}'),
                      ),
                    );
                  }),
                  if (pagination.isLoadingMore)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 16),
                      child: Center(child: CircularProgressIndicator()),
                    )
                  else if (pagination.hasMore && wallet != null)
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      child: Center(
                        child: TextButton(
                          onPressed: () => ref
                              .read(
                                transactionPaginationProvider(
                                  wallet.id,
                                ).notifier,
                              )
                              .loadMore(),
                          child: const Text('Load more'),
                        ),
                      ),
                    )
                  else
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 16),
                      child: Center(child: Text('No more transactions')),
                    ),
                ],
              ),
          ],
        ),
      ),
    );
  }
}
