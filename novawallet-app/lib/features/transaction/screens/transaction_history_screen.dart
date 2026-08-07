import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/nova_colors.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/widgets.dart';
import '../providers/transaction_provider.dart';
import '../../wallet/providers/wallet_provider.dart';
import '../providers/transaction_provider.dart' show transactionPaginationProvider, TransactionPaginationState;

/// Full transaction history (per design 08).
class TransactionHistoryScreen extends ConsumerWidget {
  const TransactionHistoryScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final wallet = ref.watch(walletProvider).valueOrNull;
    final pagination = wallet == null
        ? const TransactionPaginationState()
        : ref.watch(transactionPaginationProvider(wallet.id));

    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('Transactions')),
      body: RefreshIndicator(
        onRefresh: () async {
          if (wallet != null) {
            ref.read(transactionPaginationProvider(wallet.id).notifier).refresh();
          }
        },
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            if (pagination.transactions.isEmpty && !pagination.isLoadingMore)
              const EmptyStateView(
                icon: Icons.receipt_long_outlined,
                title: 'No transactions yet',
                message: 'Your activity will appear here.',
              )
            else
              Column(
                children: [
                  const SectionHeader(title: 'All activity'),
                  ...pagination.transactions.map((tx) {
                    return TransactionTile(
                      title: tx.displayTitle,
                      subtitle: formatRelativeDate(tx.createdAt),
                      amountText: formatZmw(tx.amount),
                      incoming: tx.isIncoming,
                      onTap: () => context.push('/transactions/${tx.reference}'),
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
                          onPressed: () => ref.read(transactionPaginationProvider(wallet.id).notifier).loadMore(),
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
