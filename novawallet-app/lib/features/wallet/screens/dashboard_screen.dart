import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/widgets.dart';
import '../../auth/providers/auth_provider.dart';
import '../../notification/providers/notification_provider.dart';
import '../../transaction/models/transaction.dart';
import '../../transaction/providers/transaction_provider.dart';
import '../providers/wallet_provider.dart';

/// Home / wallet dashboard (per design 07-home-dashboard). Tab #1 root.
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

    return Scaffold(
      backgroundColor: NovaColors.background,
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () => ref.read(walletProvider.notifier).refreshBalance(),
          child: ListView(
            padding: const EdgeInsets.all(20),
            children: [
              _TopBar(
                name: user?.firstName ?? 'there',
                unreadCount: unreadCount,
                onNotifications: () => context.push('/notifications'),
                onProfile: () => context.go('/profile'),
              ),
              const SizedBox(height: 20),
              walletAsync.when(
                loading: () => const _BalanceSkeleton(),
                error: (e, _) {
                  if (e is ApiException && e.statusCode == 404) {
                    return const _KycPrompt();
                  }
                  return _WalletError(message: '$e');
                },
                data: (w) => BalanceCard(
                  balanceLabel: 'Available balance',
                  balanceAmount: formatZmw(w.balance),
                  accountNumber: w.maskedAccount,
                  frozen: w.isFrozen,
                  onDeposit: () => context.push('/deposit'),
                  onWithdraw: () => context.push('/withdraw'),
                ),
              ),
              const SizedBox(height: 24),
              _QuickActions(onSend: () => context.go('/send')),
              const SizedBox(height: 28),
              SectionHeader(
                title: 'Recent transactions',
                actionLabel: 'View all',
                onAction: () => context.push('/transactions'),
              ),
              const SizedBox(height: 12),
              if (txsAsync.isLoading)
                const LoadingView(message: 'Loading transactions…')
              else if (txsAsync.hasError)
                const EmptyStateView(
                  icon: Icons.error_outline,
                  title: 'Couldn\'t load',
                  message: 'Pull to retry.',
                )
              else if (txsAsync.value!.isEmpty)
                const EmptyStateView(
                  icon: Icons.receipt_long_outlined,
                  title: 'No transactions yet',
                  message: 'Tap Deposit to add money to your wallet.',
                )
              else
                ...txsAsync.value!
                    .take(3)
                    .map(
                      (tx) => Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: TransactionTile(
                          title: tx.displayTitle,
                          subtitle: formatRelativeDate(tx.createdAt),
                          amountText: formatZmw(tx.amount),
                          incoming: tx.isIncoming,
                          onTap: () => context.push('/transactions/${tx.reference}'),
                        ),
                      ),
                    ),
              const SizedBox(height: 100),
            ],
          ),
        ),
      ),
    );
  }
}
class _TopBar extends StatelessWidget {
  const _TopBar({
    required this.name,
    required this.onNotifications,
    required this.onProfile,
    this.unreadCount = 0,
  });

  final String name;
  final VoidCallback onNotifications;
  final VoidCallback onProfile;

  /// Number of unread notifications from [unreadCountProvider].
  final int unreadCount;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Hi, $name',
                style: const TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w700,
                  color: NovaColors.onBackground,
                ),
              ),
              const Text(
                'Welcome back',
                style: TextStyle(fontSize: 14, color: NovaColors.onSurfaceVariant),
              ),
            ],
          ),
        ),
        Stack(
          clipBehavior: Clip.none,
          children: [
            IconButton(
              onPressed: onNotifications,
              icon: const Icon(Icons.notifications_none, color: NovaColors.onSurface),
            ),
            if (unreadCount > 0)
              Positioned(
                right: 6,
                top: 6,
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 2),
                  constraints: const BoxConstraints(minWidth: 16),
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: NovaColors.error,
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(
                    unreadCount > 99 ? '99+' : '$unreadCount',
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 10,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ),
          ],
        ),
        GestureDetector(onTap: onProfile, child: InitialsAvatar(name: name)),
      ],
    );
  }
}

class _BalanceSkeleton extends StatelessWidget {
  const _BalanceSkeleton();

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 180,
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [NovaColors.primary, NovaColors.primaryContainer],
        ),
        borderRadius: BorderRadius.circular(24),
      ),
      child: const Center(child: CircularProgressIndicator(color: Colors.white)),
    );
  }
}

class _WalletError extends StatelessWidget {
  const _WalletError({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return ErrorStateView(title: 'Wallet unavailable', message: message);
  }
}

/// KYC prompt shown when wallet is not found (404) — backend creates wallet only after KYC approval.
class _KycPrompt extends StatelessWidget {
  const _KycPrompt();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: NovaColors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: NovaColors.outlineVariant),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 64,
            height: 64,
            decoration: BoxDecoration(
              color: NovaColors.primary.withValues(alpha: 0.12),
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.verified_user_outlined,
              size: 28,
              color: NovaColors.primary,
            ),
          ),
          const SizedBox(height: 16),
          Text(
            'Complete identity verification',
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w700,
              color: NovaColors.onSurface,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Your wallet is created once your KYC is approved. Verify your identity to get started.',
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 14,
              color: NovaColors.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 20),
          PillButton(
            label: 'Start verification',
            filled: true,
            expanded: true,
            onPressed: () => context.push('/kyc/upload'),
          ),
          const SizedBox(height: 12),
          TextButton(
            onPressed: () => context.push('/kyc/status'),
            style: TextButton.styleFrom(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
              minimumSize: Size.zero,
              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
            ),
            child: const Text(
              'Check KYC status',
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: NovaColors.primary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _QuickActions extends StatelessWidget {
  const _QuickActions({required this.onSend});

  final VoidCallback onSend;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SectionHeader(title: 'Quick actions'),
        const SizedBox(height: 12),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            _Action(icon: Icons.send, label: 'Send', onTap: onSend),
            const _Action(icon: Icons.qr_code_scanner, label: 'Request', onTap: null),
            const _Action(icon: Icons.phone_android, label: 'Airtime', onTap: null),
            const _Action(icon: Icons.receipt_long, label: 'Bills', onTap: null),
          ],
        ),
      ],
    );
  }
}

class _Action extends StatelessWidget {
  const _Action({required this.icon, required this.label, this.onTap});

  final IconData icon;
  final String label;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Column(
        children: [
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(color: NovaColors.surfaceContainerLow, shape: BoxShape.circle),
            child: Icon(icon, color: NovaColors.primary, size: 24),
          ),
          const SizedBox(height: 6),
          Text(label, style: const TextStyle(fontSize: 12, color: NovaColors.onSurfaceVariant)),
        ],
      ),
    );
  }
}
