import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/design_tokens.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/widgets.dart';
import '../../../core/widgets/branded_refresh.dart';
import '../../../core/widgets/skeleton.dart';
import '../../auth/providers/auth_provider.dart';
import '../../notification/providers/notification_provider.dart';
import '../../transaction/models/transaction.dart';
import '../../transaction/providers/transaction_provider.dart';
import '../models/wallet.dart';
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

    // Show skeleton while wallet is loading
    if (walletAsync.isLoading) {
      return Scaffold(
        backgroundColor: Theme.of(context).colorScheme.surface,
        body: SafeArea(
          child: const DashboardSkeleton(),
        ),
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
              _TopBar(
                name: user?.firstName ?? 'there',
                unreadCount: unreadCount,
                onNotifications: () => context.push('/notifications'),
                onProfile: () => context.go('/profile'),
              ),
              const SizedBox(height: 20),
              _BalanceCard(
                wallet: wallet!,
                onDeposit: () => context.push('/deposit'),
                onWithdraw: () => context.push('/withdraw'),
              ),
              const SizedBox(height: 24),
              _QuickActions(
                onSend: () => context.push('/send'),
                onRequest: () => context.push('/request'),
                onAirtime: () => context.push('/airtime'),
                onBills: () => context.push('/bills'),
              ),
              const SizedBox(height: 24),
              _RecentTransactions(
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

/// Top app bar with greeting, notification bell, and profile avatar.
class _TopBar extends StatelessWidget {
  const _TopBar({
    required this.name,
    required this.unreadCount,
    required this.onNotifications,
    required this.onProfile,
  });

  final String name;
  final int unreadCount;
  final VoidCallback onNotifications;
  final VoidCallback onProfile;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Hi, $name',
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
            ),
            Text(
              'Good to see you!',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: scheme.onSurfaceVariant,
                  ),
            ),
          ],
        ),
        Row(
          children: [
            // Notification bell
            Stack(
              clipBehavior: Clip.none,
              children: [
                IconButton(
                  onPressed: onNotifications,
                  icon: const Icon(Icons.notifications_outlined),
                  style: IconButton.styleFrom(
                    backgroundColor: scheme.surfaceContainerLow,
                    foregroundColor: scheme.onSurface,
                  ),
                ),
                if (unreadCount > 0)
                  Positioned(
                    right: 4,
                    top: 4,
                    child: Container(
                      padding: const EdgeInsets.all(4),
                      decoration: BoxDecoration(
                        color: scheme.error,
                        shape: BoxShape.circle,
                      ),
                      constraints: const BoxConstraints(
                        minWidth: 16,
                        minHeight: 16,
                      ),
                      child: Text(
                        unreadCount > 9 ? '9+' : '$unreadCount',
                        style: TextStyle(
                          color: scheme.onError,
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ),
              ],
            ),
            const SizedBox(width: 12),
            // Profile avatar
            GestureDetector(
              onTap: onProfile,
              child: Container(
                width: 40,
                height: 40,
                decoration: const BoxDecoration(
                  gradient: LinearGradient(
                    colors: [SemanticColors.brandPrimary, SemanticColors.moneyIn],
                  ),
                  shape: BoxShape.circle,
                ),
                child: Center(
                  child: Text(
                    name.isNotEmpty ? name[0].toUpperCase() : '?',
                    style: const TextStyle(
                      color: SemanticColors.onBrandPrimary,
                      fontWeight: FontWeight.bold,
                      fontSize: 16,
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }
}

/// Balance card with gradient background and action buttons.
class _BalanceCard extends ConsumerStatefulWidget {
  const _BalanceCard({
    required this.wallet,
    required this.onDeposit,
    required this.onWithdraw,
  });

  final Wallet wallet;
  final VoidCallback onDeposit;
  final VoidCallback onWithdraw;

  @override
  ConsumerState<_BalanceCard> createState() => _BalanceCardState();
}

class _BalanceCardState extends ConsumerState<_BalanceCard>
    with SingleTickerProviderStateMixin {
  late final AnimationController _shimmerController;
  late final Animation<double> _shimmerAnimation;

  @override
  void initState() {
    super.initState();
    _shimmerController = AnimationController(
      duration: const Duration(milliseconds: 2000),
      vsync: this,
    )..repeat();
    _shimmerAnimation = _shimmerController;
  }

  @override
  void dispose() {
    _shimmerController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final balance = widget.wallet.balance;
    final maskedAccount = _maskAccount(widget.wallet.accountNumber);

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        // Brand hero card — identical gradient in both themes (white ink on
        // indigo), so it uses the theme-invariant brand tokens.
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            SemanticColors.brandPrimary, // #3525CD — brand indigo
            SemanticColors.brandPrimaryContainer, // #4F46E5 — gradient anchor
          ],
        ),
        boxShadow: ElevationTokens.shadowCard,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Account label
          Row(
            children: [
              const Icon(
                Icons.account_balance_wallet_outlined,
                color: SemanticColors.onBrandPrimary,
                size: 20,
              ),
              const SizedBox(width: 8),
              Text(
                'NovaWallet',
                style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      color: SemanticColors.onBrandPrimary.withValues(alpha: 0.9),
                      fontWeight: FontWeight.w600,
                    ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          // Masked account number
          Text(
            maskedAccount,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: SemanticColors.onBrandPrimary.withValues(alpha: 0.7),
                ),
          ),
          const SizedBox(height: 24),
          // Balance amount with count-up (800ms) layered under shimmer
          TweenAnimationBuilder<double>(
            tween: Tween<double>(
              begin: 0,
              end: double.tryParse(balance.replaceAll(RegExp(r'[^0-9.]'), '')) ?? 0,
            ),
            duration: const Duration(milliseconds: 800),
            curve: Curves.easeOutCubic,
            builder: (context, animatedValue, _) {
              return AnimatedBuilder(
                animation: _shimmerAnimation,
                builder: (context, child) {
                  return ShaderMask(
                    blendMode: BlendMode.srcIn,
                    shaderCallback: (bounds) {
                      return LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        colors: [
                          SemanticColors.onBrandPrimary,
                          SemanticColors.onBrandPrimary.withValues(alpha: 0.6),
                          SemanticColors.onBrandPrimary,
                        ],
                        stops: const [0.0, 0.5, 1.0],
                        transform: _SlidingGradientTransform(
                          slidePercent: _shimmerAnimation.value,
                        ),
                      ).createShader(bounds);
                    },
                    child: Text(
                      formatZmw(animatedValue.toStringAsFixed(2)),
                      style: Theme.of(context).textTheme.displayMedium?.copyWith(
                            color: SemanticColors.onBrandPrimary,
                            fontWeight: FontWeight.bold,
                            height: 1.1,
                          ),
                    ),
                  );
                },
              );
            },
          ),
          const SizedBox(height: 4),
          Text(
            'Available Balance',
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: SemanticColors.onBrandPrimary.withValues(alpha: 0.7),
                ),
          ),
          const SizedBox(height: 24),
          // Action buttons
          Row(
            children: [
              Expanded(
                child: PillButton(
                  label: 'Deposit',
                  icon: Icons.add_circle_outline,
                  variant: PillButtonVariant.outlined,
                  onPressed: widget.onDeposit,
                  semanticsLabel: 'Deposit money',
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: PillButton(
                  label: 'Withdraw',
                  icon: Icons.remove_circle_outline,
                  variant: PillButtonVariant.filled,
                  onPressed: widget.onWithdraw,
                  semanticsLabel: 'Withdraw money',
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  String _maskAccount(String accountNumber) {
    if (accountNumber.length <= 4) return accountNumber;
    final visible = accountNumber.substring(accountNumber.length - 4);
    return '**** **** **** $visible';
  }
}

/// Transform for sliding gradient in balance card.
class _SlidingGradientTransform extends GradientTransform {
  const _SlidingGradientTransform({required this.slidePercent});

  final double slidePercent;

  @override
  Matrix4? transform(Rect bounds, {TextDirection? textDirection}) {
    final double width = bounds.width;
    final double translate = width * slidePercent * 2 - width;
    return Matrix4.translationValues(translate, 0.0, 0.0);
  }
}

/// Quick actions grid.
class _QuickActions extends StatelessWidget {
  const _QuickActions({
    required this.onSend,
    required this.onRequest,
    required this.onAirtime,
    required this.onBills,
  });

  final VoidCallback onSend;
  final VoidCallback onRequest;
  final VoidCallback onAirtime;
  final VoidCallback onBills;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final actions = [
      _ActionData('Send', Icons.send_outlined, scheme.primary, onSend),
      _ActionData('Request', Icons.call_received_outlined, scheme.secondary, onRequest),
      // Amber passes ≥ 4.5:1 on dark surfaces as-is, so it stays the same
      // semantic token in both themes.
      _ActionData('Airtime', Icons.phone_android_outlined, SemanticColors.warning, onAirtime),
      _ActionData('Bills', Icons.receipt_long_outlined, scheme.secondary, onBills),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Quick Actions',
          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w600,
              ),
        ),
        const SizedBox(height: 16),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: actions.map((action) {
            return _AnimatedAction(action: action);
          }).toList(),
        ),
      ],
    );
  }
}

class _ActionData {
  const _ActionData(this.label, this.icon, this.color, this.onTap);

  final String label;
  final IconData icon;
  final Color color;
  final VoidCallback onTap;
}

class _AnimatedAction extends StatefulWidget {
  const _AnimatedAction({required this.action});

  final _ActionData action;

  @override
  State<_AnimatedAction> createState() => _AnimatedActionState();
}

class _AnimatedActionState extends State<_AnimatedAction>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final Animation<double> _scaleAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(milliseconds: 150),
      vsync: this,
    );
    _scaleAnimation = Tween<double>(
      begin: 1.0,
      end: 0.85,
    ).animate(CurvedAnimation(
      parent: _controller,
      curve: Curves.easeOutCubic,
      reverseCurve: Curves.easeOutCubic,
    ));
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _handleTapDown(TapDownDetails details) {
    _controller.forward();
  }

  void _handleTapUp(TapUpDetails details) {
    _controller.reverse();
    widget.action.onTap();
    HapticFeedback.lightImpact();
  }

  void _handleTapCancel() {
    _controller.reverse();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _scaleAnimation,
      builder: (context, child) {
        return Transform.scale(
          scale: _scaleAnimation.value,
          child: GestureDetector(
            onTapDown: _handleTapDown,
            onTapUp: _handleTapUp,
            onTapCancel: _handleTapCancel,
            child: child!,
          ),
        );
      },
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(
              color: widget.action.color.withValues(alpha: 0.15),
              shape: BoxShape.circle,
            ),
            child: Icon(
              widget.action.icon,
              color: widget.action.color,
              size: 24,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            widget.action.label,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: Theme.of(context).colorScheme.onSurface,
                  fontWeight: FontWeight.w500,
                ),
          ),
        ],
      ),
    );
  }
}

/// Recent transactions section.
class _RecentTransactions extends StatelessWidget {
  const _RecentTransactions({
    required this.transactionsAsync,
    required this.onViewAll,
  });

  final AsyncValue<List<WalletTransaction>> transactionsAsync;
  final VoidCallback onViewAll;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              'Recent Transactions',
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
            ),
            TextButton(
              onPressed: onViewAll,
              child: Text(
                'View All',
                style: TextStyle(
                  color: Theme.of(context).colorScheme.primary,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),
        _buildTransactionsList(context),
      ],
    );
  }

  Widget _buildTransactionsList(BuildContext context) {
    if (transactionsAsync.isLoading) {
      return const TransactionListSkeleton();
    }

    if (transactionsAsync.hasError) {
      return ErrorStateView(
        title: 'Could not load transactions',
        message: transactionsAsync.error.toString(),
        onRetry: () {}, // Will be handled by parent refresh
      );
    }

    final transactions = transactionsAsync.value ?? [];

    if (transactions.isEmpty) {
      return EmptyStateView(
        icon: Icons.receipt_long_outlined,
        title: 'No transactions yet',
        message: 'Your transaction history will appear here.',
        actionLabel: 'Send Money',
        onAction: onViewAll,
      );
    }

    return ListView.separated(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: transactions.length.clamp(0, 5),
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        final tx = transactions[index];
        return _AnimatedTransactionTile(transaction: tx, index: index);
      },
    );
  }
}

class _AnimatedTransactionTile extends StatefulWidget {
  const _AnimatedTransactionTile({
    required this.transaction,
    required this.index,
  });

  final WalletTransaction transaction;
  final int index;

  @override
  State<_AnimatedTransactionTile> createState() => _AnimatedTransactionTileState();
}

class _AnimatedTransactionTileState extends State<_AnimatedTransactionTile>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final Animation<Offset> _slideAnimation;
  late final Animation<double> _fadeAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: Duration(milliseconds: 300 + widget.index * 50),
      vsync: this,
    );
    _slideAnimation = Tween<Offset>(
      begin: const Offset(0, 0.3),
      end: Offset.zero,
    ).animate(CurvedAnimation(
      parent: _controller,
      curve: Curves.easeOutCubic,
    ));
    _fadeAnimation = CurvedAnimation(
      parent: _controller,
      curve: Curves.easeOutCubic,
    );
    _controller.forward();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SlideTransition(
      position: _slideAnimation,
      child: FadeTransition(
        opacity: _fadeAnimation,
        child: TransactionTile(
          title: widget.transaction.displayTitle,
          subtitle: formatRelativeDate(widget.transaction.createdAt),
          amountText: widget.transaction.signedAmount,
          incoming: widget.transaction.isIncoming,
          heroTag: widget.transaction.reference,
          onTap: () {
            context.push('/transactions/${widget.transaction.reference}');
          },
        ),
      ),
    );
  }
}