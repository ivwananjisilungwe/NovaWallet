import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/nova_colors.dart';
import '../../../core/theme/design_tokens.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/widgets.dart';
import '../providers/transaction_provider.dart';

/// Transaction detail card (per design 09). Fetched by reference.
///
/// Features:
/// - Hero-animated icon transition from the transaction list tile
/// - Staggered content fade-in on entry
/// - Share receipt button (copies formatted receipt to clipboard)
/// - Haptic feedback on interactive elements
class TransactionDetailScreen extends ConsumerStatefulWidget {
  const TransactionDetailScreen({super.key, required this.reference});

  final String reference;

  @override
  ConsumerState<TransactionDetailScreen> createState() =>
      _TransactionDetailScreenState();
}

class _TransactionDetailScreenState
    extends ConsumerState<TransactionDetailScreen>
    with SingleTickerProviderStateMixin {
  late final AnimationController _staggerController;
  late final Animation<double> _fadeAnimation;

  @override
  void initState() {
    super.initState();
    _staggerController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 600),
    );
    _fadeAnimation = CurvedAnimation(
      parent: _staggerController,
      curve: Curves.easeOutCubic,
    );
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _staggerController.forward();
    });
  }

  @override
  void dispose() {
    _staggerController.dispose();
    super.dispose();
  }

  void _shareReceipt(dynamic tx) {
    HapticFeedback.lightImpact();
    final buffer = StringBuffer()
      ..writeln('Transaction Receipt')
      ..writeln('-----------------')
      ..writeln('Status: ${tx.status}')
      ..writeln('Type: ${tx.type}')
      ..writeln('Amount: ${formatZmw(tx.amount)}')
      ..writeln('Reference: ${tx.reference}')
      ..writeln('Date: ${formatDateTime(tx.createdAt)}');
    if (tx.description != null) {
      buffer.writeln('Description: ${tx.description}');
    }
    Clipboard.setData(ClipboardData(text: buffer.toString()));
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Receipt copied to clipboard'),
          behavior: SnackBarBehavior.floating,
          duration: Duration(seconds: 2),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final txAsync = ref.watch(transactionByReferenceProvider(widget.reference));

    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(
        title: const Text('Transaction'),
        actions: [
          IconButton(
            icon: const Icon(Icons.share_outlined),
            onPressed: () {
              final tx = txAsync.valueOrNull;
              if (tx != null) _shareReceipt(tx);
            },
            tooltip: 'Share receipt',
          ),
        ],
      ),
      body: SafeArea(
        child: txAsync.when(
          loading: () => const LoadingView(message: 'Loading...'),
          error: (e, _) => ErrorStateView(message: '$e'),
          data: (tx) => FadeTransition(
            opacity: _fadeAnimation,
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const SizedBox(height: 12),
                  Hero(
                    tag: 'tx_icon_${tx.reference}',
                    child: CircleAvatar(
                      radius: 40,
                      backgroundColor: tx.isIncoming
                          ? NovaColors.secondaryContainer
                          : NovaColors.errorContainer,
                      child: Icon(
                        tx.isIncoming
                            ? Icons.arrow_downward
                            : Icons.arrow_upward,
                        color: tx.isIncoming
                            ? NovaColors.onSecondaryContainer
                            : NovaColors.error,
                        size: 32,
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    tx.displayTitle,
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w600,
                      color: NovaColors.onSurface,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    formatZmw(tx.amount),
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 32,
                      fontWeight: FontWeight.w700,
                      color: tx.isIncoming
                          ? NovaColors.success
                          : NovaColors.error,
                      letterSpacing: -0.02,
                    ),
                  ),
                  const SizedBox(height: 24),
                  Center(
                    child: StatusChip(
                      label: tx.status,
                      color: tx.status == 'SUCCESSFUL'
                          ? NovaColors.success
                          : (tx.status == 'PENDING'
                                ? NovaColors.warning
                                : NovaColors.error),
                    ),
                  ),
                  const SizedBox(height: 24),
                  Container(
                    decoration: BoxDecoration(
                      color: NovaColors.surfaceContainerLowest,
                      borderRadius: ShapeTokens.borderRadiusMd,
                    ),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 4,
                    ),
                    child: Column(
                      children: [
                        _DetailRow(label: 'Reference', value: tx.reference),
                        const Divider(height: 1),
                        _DetailRow(label: 'Type', value: tx.type),
                        const Divider(height: 1),
                        _DetailRow(
                          label: 'Date',
                          value: formatDateTime(tx.createdAt),
                        ),
                        if (tx.description != null) ...[
                          const Divider(height: 1),
                          _DetailRow(
                            label: 'Description',
                            value: tx.description!,
                          ),
                        ],
                        if (tx.balanceBefore != null) ...[
                          const Divider(height: 1),
                          _DetailRow(
                            label: 'Balance before',
                            value: formatZmw(tx.balanceBefore),
                          ),
                        ],
                        if (tx.balanceAfter != null) ...[
                          const Divider(height: 1),
                          _DetailRow(
                            label: 'Balance after',
                            value: formatZmw(tx.balanceAfter),
                          ),
                        ],
                      ],
                    ),
                  ),
                  const SizedBox(height: 24),
                  PillButton(
                    label: 'Share Receipt',
                    icon: Icons.share_outlined,
                    variant: PillButtonVariant.outlined,
                    onPressed: () => _shareReceipt(tx),
                  ),
                ],
              ),
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
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: const TextStyle(
              color: NovaColors.onSurfaceVariant,
              fontSize: 14,
            ),
          ),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: const TextStyle(
                fontWeight: FontWeight.w600,
                color: NovaColors.onSurface,
                fontSize: 14,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
