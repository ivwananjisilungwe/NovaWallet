import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/nova_colors.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/widgets.dart';
import '../models/transaction.dart';
import '../providers/transaction_provider.dart';
import '../../wallet/providers/wallet_provider.dart';

/// Full transaction history (per design 08).
class TransactionHistoryScreen extends ConsumerWidget {
  const TransactionHistoryScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final wallet = ref.watch(walletProvider).valueOrNull;
    final txsAsync = wallet == null
        ? const AsyncValue<List<WalletTransaction>>.loading()
        : ref.watch(transactionsProvider(wallet.id));

    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('Transactions')),
      body: RefreshIndicator(
        onRefresh: () async {
          if (wallet != null) {
            ref.invalidate(transactionsProvider(wallet.id));
          }
        },
        child: txsAsync.when(
          loading: () => const LoadingView(message: 'Loading…'),
          error: (e, _) => ListView(
            children: const [
              SizedBox(height: 120),
              ErrorStateView(title: 'Couldn\'t load', message: 'Pull to retry.'),
            ],
          ),
          data: (txs) {
            if (txs.isEmpty) {
              return ListView(
                children: const [
                  SizedBox(height: 120),
                  EmptyStateView(
                    icon: Icons.receipt_long_outlined,
                    title: 'No transactions yet',
                    message: 'Your activity will appear here.',
                  ),
                ],
              );
            }
            return ListView.separated(
              padding: const EdgeInsets.all(20),
              itemCount: txs.length + 1,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (context, i) {
                if (i == 0) {
                  return const SectionHeader(title: 'All activity');
                }
                final tx = txs[i - 1];
                return TransactionTile(
                  title: tx.displayTitle,
                  subtitle: formatRelativeDate(tx.createdAt),
                  amountText: formatZmw(tx.amount),
                  incoming: tx.isIncoming,
                  onTap: () => context.push('/transactions/${tx.reference}'),
                );
              },
            );
          },
        ),
      ),
    );
  }
}
