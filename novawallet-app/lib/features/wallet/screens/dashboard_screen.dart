import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/widgets/branded_refresh.dart';
import '../../../core/widgets/skeleton.dart';
import '../../../core/widgets/widgets.dart';
import '../../auth/providers/auth_provider.dart';
import '../../notification/providers/notification_provider.dart';
import '../../transaction/models/transaction.dart';
import '../../transaction/providers/transaction_provider.dart';
import '../providers/wallet_provider.dart';
import '../widgets/dashboard_balance_card.dart';
import '../widgets/dashboard_quick_actions.dart';
import '../widgets/dashboard_recent_transactions.dart';
import '../widgets/dashboard_top_bar.dart';

/// Home / wallet dashboard (per design 07-home-dashboard). Tab #1 root.
///
/// The section widgets (top bar, balance card, quick actions, recent
/// transactions) live in `../widgets/`.
class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authProvider).state;
    final walletAsync = ref.watch(walletProvider);
    final user = auth.user;
    final unreadCount = ref.watch(unreadCountProvider).valueOrNull ?? 0;

    final wallet = walletAsync.valueOrNull;
    final txsAsync = wallet == null
        ? const AsyncValue<List<WalletTransaction>>.loading()
        : ref.watch(transactionsProvider(wallet.id));

    // Show skeleton while wallet is loading
    if (walletAsync.isLoading) {
      return Scaffold(
        backgroundColor: Theme.of(context).colorScheme.surface,
        body: SafeArea(child: const DashboardSkeleton()),
      );
    }

    // Show error state if wallet failed to load
    if (walletAsync.hasError) {
      return Scaffold(
        backgroundColor: Theme.of(context).colorScheme.surface,
        body: SafeArea(
          child: ErrorStateView(
            title: 'Wallet unavailable',
            message: walletAsync.error.toString(),
            onRetry: () => ref.read(walletProvider.notifier).refreshBalance(),
          ),
        ),
      );
    }

    return Scaffold(
      backgroundColor: Theme.of(context).colorScheme.surface,
      body: SafeArea(
        child: BrandedRefreshIndicator(
          onRefresh: () => ref.read(walletProvider.notifier).refreshBalance(),
          child: ListView(
            padding: const EdgeInsets.all(20),
            children: [
              DashboardTopBar(
                name: user?.firstName ?? 'there',
                unreadCount: unreadCount,
                onNotifications: () => context.push('/notifications'),
                onProfile: () => context.go('/profile'),
              ),
              const SizedBox(height: 20),
              DashboardBalanceCard(
                wallet: wallet!,
                onDeposit: () => context.push('/deposit'),
                onWithdraw: () => context.push('/withdraw'),
              ),
              const SizedBox(height: 24),
              DashboardQuickActions(
                onSend: () => context.push('/send'),
                onRequest: () => context.push('/request'),
                onAirtime: () => context.push('/airtime'),
                onBills: () => context.push('/bills'),
              ),
              const SizedBox(height: 24),
              DashboardRecentTransactions(
                transactionsAsync: txsAsync,
                onViewAll: () => context.push('/transactions'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
