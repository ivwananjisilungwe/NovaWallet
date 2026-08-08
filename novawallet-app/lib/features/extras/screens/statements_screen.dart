import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import 'package:novawallet_app/core/theme/nova_colors.dart';
import 'package:novawallet_app/core/utils/formatters.dart';
import 'package:novawallet_app/core/widgets/widgets.dart';
import 'package:novawallet_app/features/statement/models/statement_summary.dart';
import 'package:novawallet_app/features/statement/providers/statement_provider.dart';
import 'package:novawallet_app/features/wallet/providers/wallet_provider.dart';

/// Statements (per design 28). Live monthly summary from the backend.
class StatementsScreen extends ConsumerStatefulWidget {
  const StatementsScreen({super.key});

  @override
  ConsumerState<StatementsScreen> createState() => _StatementsScreenState();
}

class _StatementsScreenState extends ConsumerState<StatementsScreen> {
  DateTime _month = DateTime.now();

  void _shift(int delta) =>
      setState(() => _month = DateTime(_month.year, _month.month + delta));

  /// `yyyy-MM` — the backend statement month key.
  String get _monthKey =>
      '${_month.year.toString().padLeft(4, '0')}-'
      '${_month.month.toString().padLeft(2, '0')}';

  @override
  Widget build(BuildContext context) {
    final walletAsync = ref.watch(walletProvider);

    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('Statements')),
      body: SafeArea(
        child: walletAsync.when(
          loading: () => const LoadingView(message: 'Loading wallet…'),
          error: (error, _) => ErrorStateView(
            message: '$error',
            onRetry: () => ref.invalidate(walletProvider),
          ),
          data: (wallet) => _buildMonthSummary(wallet.id),
        ),
      ),
    );
  }

  Widget _buildMonthSummary(String walletId) {
    final label = DateFormat('MMMM yyyy').format(_month);
    final statementArg = (walletId: walletId, month: _monthKey);
    final summaryAsync = ref.watch(statementProvider(statementArg));

    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              IconButton(
                onPressed: () => _shift(-1),
                icon: const Icon(Icons.chevron_left),
              ),
              Text(
                label,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w700,
                  color: NovaColors.onBackground,
                ),
              ),
              IconButton(
                onPressed: () => _shift(1),
                icon: const Icon(Icons.chevron_right),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Expanded(
            child: summaryAsync.when(
              loading: () => const LoadingView(message: 'Loading statement…'),
              error: (error, _) => ErrorStateView(
                message: '$error',
                onRetry: () => ref.invalidate(statementProvider(statementArg)),
              ),
              data: (summary) => RefreshIndicator(
                onRefresh: () =>
                    ref.refresh(statementProvider(statementArg).future),
                child: ListView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  children: [
                    _SummaryCard(summary: summary),
                    const SizedBox(height: 20),
                    if (summary.hasActivity)
                      _BreakdownCard(summary: summary)
                    else
                      const EmptyStateView(
                        icon: Icons.receipt_long_outlined,
                        title: 'No activity this month',
                        message: 'Deposits, withdrawals and transfers will '
                            'appear in this statement once you have activity.',
                      ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Gradient hero card with the month's net change headline.
class _SummaryCard extends StatelessWidget {
  const _SummaryCard({required this.summary});

  final StatementSummary summary;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [NovaColors.primary, NovaColors.primaryContainer],
        ),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Statement summary',
            style: TextStyle(color: Colors.white70, fontSize: 13),
          ),
          const SizedBox(height: 8),
          Text(
            _signedZmw(summary.netChange),
            style: const TextStyle(
              color: Colors.white,
              fontSize: 28,
              fontWeight: FontWeight.w700,
              letterSpacing: -0.02,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            '${formatNumber(summary.transactionCount)} '
            'transaction${summary.transactionCount == 1 ? '' : 's'}',
            style: const TextStyle(color: Colors.white70, fontSize: 13),
          ),
        ],
      ),
    );
  }
}

/// White card breaking the month into deposits / withdrawals / transfers /
/// fees, with the net change (green when positive, red when negative).
class _BreakdownCard extends StatelessWidget {
  const _BreakdownCard({required this.summary});

  final StatementSummary summary;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: NovaColors.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: NovaColors.surfaceContainerLow),
      ),
      child: Column(
        children: [
          _BreakdownRow(
            label: 'Net change',
            amount: _signedZmw(summary.netChange),
            color: _netChangeColor(summary.netChange),
            emphasized: true,
          ),
          const Divider(height: 1, color: NovaColors.surfaceContainerLow),
          _BreakdownRow(
            label: 'Deposits',
            amount: formatZmw(summary.totalDeposits),
            color: NovaColors.success,
          ),
          const Divider(height: 1, color: NovaColors.surfaceContainerLow),
          _BreakdownRow(
            label: 'Withdrawals',
            amount: formatZmw(summary.totalWithdrawals),
            color: NovaColors.error,
          ),
          const Divider(height: 1, color: NovaColors.surfaceContainerLow),
          _BreakdownRow(
            label: 'Transfers in',
            amount: formatZmw(summary.totalTransfersIn),
            color: NovaColors.success,
          ),
          const Divider(height: 1, color: NovaColors.surfaceContainerLow),
          _BreakdownRow(
            label: 'Transfers out',
            amount: formatZmw(summary.totalTransfersOut),
            color: NovaColors.error,
          ),
          const Divider(height: 1, color: NovaColors.surfaceContainerLow),
          _BreakdownRow(
            label: 'Fees',
            amount: formatZmw(summary.totalFees),
            color: NovaColors.error,
          ),
          const Divider(height: 1, color: NovaColors.surfaceContainerLow),
          _BreakdownRow(
            label: 'Transactions',
            amount: formatNumber(summary.transactionCount),
            color: NovaColors.onSurface,
          ),
        ],
      ),
    );
  }
}

class _BreakdownRow extends StatelessWidget {
  const _BreakdownRow({
    required this.label,
    required this.amount,
    required this.color,
    this.emphasized = false,
  });

  final String label;
  final String amount;
  final Color color;
  final bool emphasized;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.symmetric(
        horizontal: 16,
        vertical: emphasized ? 16 : 14,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: TextStyle(
              fontSize: emphasized ? 15 : 14,
              fontWeight: emphasized ? FontWeight.w700 : FontWeight.w500,
              color: emphasized
                  ? NovaColors.onSurface
                  : NovaColors.onSurfaceVariant,
            ),
          ),
          Text(
            amount,
            style: TextStyle(
              fontSize: emphasized ? 16 : 14,
              fontWeight: FontWeight.w700,
              color: color,
            ),
          ),
        ],
      ),
    );
  }
}

/// `+ZMW 1,234.56` / `-ZMW 456.78` / `ZMW 0.00`.
String _signedZmw(String raw) {
  final value = double.tryParse(raw) ?? 0.0;
  if (value == 0) return formatZmw('0.00');
  final body = formatZmw(value.abs(), includeSymbol: false);
  return value > 0 ? '+ZMW $body' : '-ZMW $body';
}

/// Green for a positive net change, red for a negative one, neutral at zero.
Color _netChangeColor(String raw) {
  final value = double.tryParse(raw) ?? 0.0;
  if (value > 0) return NovaColors.success;
  if (value < 0) return NovaColors.error;
  return NovaColors.onSurface;
}