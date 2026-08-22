import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/utils/formatters.dart';
import '../../../core/widgets/skeleton.dart';
import '../../../core/widgets/widgets.dart';
import '../../transaction/models/transaction.dart';

/// Recent transactions section.
class DashboardRecentTransactions extends StatelessWidget {
  const DashboardRecentTransactions({
    super.key,
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
              style: Theme.of(
                context,
              ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
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
  State<_AnimatedTransactionTile> createState() =>
      _AnimatedTransactionTileState();
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
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeOutCubic));
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
