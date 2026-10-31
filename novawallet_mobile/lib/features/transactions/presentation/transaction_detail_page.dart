import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:novawallet_mobile/core/design/app_tokens.dart';
import 'package:novawallet_mobile/features/auth/data/auth_service.dart';
import 'package:novawallet_mobile/features/auth/data/session_store.dart';
import 'package:novawallet_mobile/features/auth/presentation/login_page.dart';
import 'package:novawallet_mobile/features/transactions/data/transaction.dart';
import 'package:novawallet_mobile/features/transactions/data/transaction_service.dart';

class TransactionDetailPage extends StatefulWidget {
  const TransactionDetailPage({
    super.key,
    this.transaction,
    this.reference,
    this.transactionService,
    this.sessionStore,
    this.currency,
  });

  final Transaction? transaction;
  final String? reference;
  final TransactionService? transactionService;
  final SessionStore? sessionStore;
  final String? currency;

  @override
  State<TransactionDetailPage> createState() => _TransactionDetailPageState();
}

// Common naming variants used by tests/navigation.
typedef TransactionDetailsPage = TransactionDetailPage;
typedef TransactionDetailView = TransactionDetailPage;

class _TransactionDetailPageState extends State<TransactionDetailPage>
    with SingleTickerProviderStateMixin {
  Transaction? _transaction;
  bool _loading = false;
  String? _error;
  bool _requestInFlight = false;

  late final AnimationController _entranceController;
  bool _reducedMotionHandled = false;
  bool _entranceStarted = false;

  bool _copyInProgress = false;

  bool get _needsFetch =>
      widget.transaction == null && (widget.reference?.isNotEmpty ?? false);

  @override
  void initState() {
    super.initState();
    _transaction = widget.transaction;
    _entranceController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: AppTokens.animationEntranceMs),
    );
    if (_needsFetch) {
      _loading = true;
    }
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_reducedMotionHandled) return;
    if (MediaQuery.disableAnimationsOf(context)) {
      _reducedMotionHandled = true;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        _entranceController.value = 1.0;
      });
    }
    // Start the fetch after dependencies are available (needs context for service).
    if (_needsFetch && !_requestInFlight && _loading && _transaction == null) {
      // Defer to post-frame to avoid setState during build.
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        _fetch();
      });
    }
    // Start entrance animation once.
    if (!_entranceStarted) {
      _entranceStarted = true;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        if (MediaQuery.disableAnimationsOf(context)) {
          _entranceController.value = 1.0;
        } else {
          _entranceController.forward();
        }
      });
    }
  }

  @override
  void dispose() {
    _entranceController.dispose();
    super.dispose();
  }

  TransactionService get _service =>
      widget.transactionService ??
      HttpTransactionService(sessionStore: widget.sessionStore!);

  Future<void> _fetch() async {
    if (_requestInFlight) return;
    _requestInFlight = true;
    if (mounted) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }
    try {
      final tx = await _service.getTransactionByReference(widget.reference!);
      if (!mounted) return;
      setState(() {
        _transaction = tx;
        _loading = false;
      });
    } on AuthException catch (error) {
      if (error.type == AuthErrorType.sessionExpired ||
          error.statusCode == 401) {
        await widget.sessionStore?.clear();
        if (!mounted) return;
        Navigator.of(context).pushReplacement(
          MaterialPageRoute<void>(
            builder: (_) => LoginPage(sessionStore: widget.sessionStore),
          ),
        );
        return;
      }
      if (!mounted) return;
      setState(() {
        _error = error.userMessage;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = 'Unable to load this transaction. Please try again.';
        _loading = false;
      });
    } finally {
      _requestInFlight = false;
    }
  }

  String? get _currency => _transaction?.currency ?? widget.currency;

  String _typeLabel(String type) => switch (type) {
    'DEPOSIT' => 'Deposit',
    'WITHDRAWAL' => 'Withdrawal',
    'TRANSFER_DEBIT' => 'Transfer sent',
    'TRANSFER_CREDIT' => 'Transfer received',
    'FEE' => 'Fee',
    _ => type,
  };

  IconData _typeIcon(String type) => switch (type) {
    'DEPOSIT' => Icons.south_west_rounded,
    'WITHDRAWAL' => Icons.north_east_rounded,
    'TRANSFER_DEBIT' => Icons.north_east_rounded,
    'TRANSFER_CREDIT' => Icons.south_west_rounded,
    'FEE' => Icons.receipt_long_outlined,
    _ => Icons.receipt_outlined,
  };

  String _statusLabel(String status) => switch (status) {
    'PENDING' => 'Pending',
    'SUCCESSFUL' => 'Successful',
    'FAILED' => 'Failed',
    _ => status,
  };

  IconData _statusIcon(String status) => switch (status) {
    'SUCCESSFUL' => Icons.check_circle_rounded,
    'FAILED' => Icons.cancel_rounded,
    'PENDING' => Icons.schedule_rounded,
    _ => Icons.info_outline_rounded,
  };

  Color _statusColor(ColorScheme colors, String status) => switch (status) {
    'SUCCESSFUL' => colors.tertiary,
    'FAILED' => colors.error,
    _ => colors.onSurfaceVariant,
  };

  String _formatAmount(Transaction tx) {
    final prefix = tx.isCredit ? '+' : '\u2212';
    final currency = _currency;
    return '$prefix ${currency == null || currency.isEmpty ? '' : '$currency '}${tx.amount}';
  }

  String _formatDate(String? raw) {
    if (raw == null || raw.isEmpty) return '\u2014';
    try {
      final dt = DateTime.parse(raw);
      String two(int value) => value.toString().padLeft(2, '0');
      return '${dt.year}-${two(dt.month)}-${two(dt.day)} '
          '${two(dt.hour)}:${two(dt.minute)}';
    } catch (_) {
      return raw;
    }
  }

  Future<void> _copyReference(String reference) async {
    if (_copyInProgress) return;
    _copyInProgress = true;
    try {
      await Clipboard.setData(ClipboardData(text: reference));
      if (!mounted) return;
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('Reference copied.')));
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Could not copy reference.')),
      );
    } finally {
      Future.delayed(const Duration(milliseconds: 300), () {
        if (mounted) {
          setState(() => _copyInProgress = false);
        }
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;

    if (_loading) {
      return Scaffold(
        appBar: AppBar(title: const Text('Transaction details')),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    if (_error != null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Transaction details')),
        body: Padding(
          padding: AppTokens.pagePadding,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(_error!, style: TextStyle(color: colors.error)),
              const SizedBox(height: AppTokens.space12),
              FilledButton(onPressed: _fetch, child: const Text('Retry')),
            ],
          ),
        ),
      );
    }

    final tx = _transaction;
    if (tx == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Transaction details')),
        body: Padding(
          padding: AppTokens.pagePadding,
          child: Text(
            'Transaction details are unavailable.',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ),
      );
    }

    final amountText = _formatAmount(tx);

    // Staggered entrance: summary first, then detail rows.
    final summaryOpacity = CurvedAnimation(
      parent: _entranceController,
      curve: const Interval(0.0, 0.5, curve: Curves.easeOut),
    );
    final summarySlide =
        Tween<Offset>(begin: const Offset(0, 0.08), end: Offset.zero).animate(
          CurvedAnimation(
            parent: _entranceController,
            curve: const Interval(0.0, 0.5, curve: Curves.easeOutCubic),
          ),
        );

    final detailsOpacity = CurvedAnimation(
      parent: _entranceController,
      curve: const Interval(0.3, 0.9, curve: Curves.easeOut),
    );

    Widget staggeredRow(int index, Widget child) {
      final start = (0.3 + index * (AppTokens.animationStaggerMs / 1000.0))
          .clamp(0.0, 0.85);
      final end = (start + 0.35).clamp(0.0, 1.0);
      final opacity = CurvedAnimation(
        parent: _entranceController,
        curve: Interval(start, end, curve: Curves.easeOut),
      );
      final slide =
          Tween<Offset>(begin: const Offset(0, 0.06), end: Offset.zero).animate(
            CurvedAnimation(
              parent: _entranceController,
              curve: Interval(start, end, curve: Curves.easeOutCubic),
            ),
          );
      return FadeTransition(
        opacity: opacity,
        child: SlideTransition(position: slide, child: child),
      );
    }

    final detailRows = <Widget>[];
    detailRows.add(_DetailRow(label: 'Reference', value: tx.reference));
    if (tx.reference.isNotEmpty) {
      detailRows.add(
        Align(
          alignment: Alignment.centerLeft,
          child: TextButton.icon(
            onPressed: _copyInProgress
                ? null
                : () => _copyReference(tx.reference),
            style: TextButton.styleFrom(
              minimumSize: const Size(48, AppTokens.iconButtonSize),
            ),
            icon: Icon(
              _copyInProgress ? Icons.check_rounded : Icons.copy_rounded,
              size: 18,
            ),
            label: const Text('Copy reference'),
          ),
        ),
      );
    }
    if (tx.description != null && tx.description!.trim().isNotEmpty) {
      detailRows.add(_DetailRow(label: 'Description', value: tx.description!));
    }
    detailRows.add(_DetailRow(label: 'Date', value: _formatDate(tx.createdAt)));
    detailRows.add(_DetailRow(label: 'Status', value: _statusLabel(tx.status)));
    detailRows.add(_DetailRow(label: 'Type', value: _typeLabel(tx.type)));
    if (tx.balanceBefore != null && tx.balanceBefore!.trim().isNotEmpty) {
      detailRows.add(
        _DetailRow(
          label: 'Balance before',
          value:
              '${_currency == null || _currency!.isEmpty ? '' : '$_currency '}${tx.balanceBefore}',
        ),
      );
    }
    if (tx.balanceAfter != null && tx.balanceAfter!.trim().isNotEmpty) {
      detailRows.add(
        _DetailRow(
          label: 'Balance after',
          value:
              '${_currency == null || _currency!.isEmpty ? '' : '$_currency '}${tx.balanceAfter}',
        ),
      );
    }
    if (tx.senderWalletId != null && tx.senderWalletId!.trim().isNotEmpty) {
      detailRows.add(
        _DetailRow(label: 'Sender wallet', value: tx.senderWalletId!),
      );
    }
    if (tx.receiverWalletId != null && tx.receiverWalletId!.trim().isNotEmpty) {
      detailRows.add(
        _DetailRow(label: 'Receiver wallet', value: tx.receiverWalletId!),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Transaction details')),
      body: SingleChildScrollView(
        padding: AppTokens.pagePadding,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            FadeTransition(
              opacity: summaryOpacity,
              child: SlideTransition(
                position: summarySlide,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    Center(
                      child: Container(
                        width: 64,
                        height: 64,
                        decoration: BoxDecoration(
                          color: colors.surfaceContainerHigh,
                          borderRadius: BorderRadius.circular(
                            AppTokens.radiusLarge,
                          ),
                        ),
                        child: Icon(
                          _typeIcon(tx.type),
                          size: 32,
                          color: tx.isCredit ? colors.tertiary : colors.primary,
                        ),
                      ),
                    ),
                    const SizedBox(height: AppTokens.space12),
                    Center(
                      child: Text(
                        _typeLabel(tx.type),
                        style: Theme.of(context).textTheme.titleLarge,
                        textAlign: TextAlign.center,
                      ),
                    ),
                    const SizedBox(height: AppTokens.space8),
                    Center(
                      child: Semantics(
                        label: 'Amount $amountText',
                        child: Text(
                          amountText,
                          style: Theme.of(context).textTheme.headlineLarge
                              ?.copyWith(
                                fontWeight: FontWeight.w700,
                                color: tx.isCredit
                                    ? colors.tertiary
                                    : colors.primary,
                              ),
                          textAlign: TextAlign.center,
                        ),
                      ),
                    ),
                    const SizedBox(height: AppTokens.space12),
                    Center(
                      child: Semantics(
                        label: 'Status ${_statusLabel(tx.status)}',
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: AppTokens.space12,
                            vertical: AppTokens.space4,
                          ),
                          decoration: BoxDecoration(
                            color: _statusColor(
                              colors,
                              tx.status,
                            ).withValues(alpha: 0.12),
                            borderRadius: BorderRadius.circular(
                              AppTokens.radiusMedium,
                            ),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(
                                _statusIcon(tx.status),
                                size: 16,
                                color: _statusColor(colors, tx.status),
                              ),
                              const SizedBox(width: AppTokens.space4),
                              Text(
                                _statusLabel(tx.status),
                                style: Theme.of(context).textTheme.labelLarge
                                    ?.copyWith(
                                      color: _statusColor(colors, tx.status),
                                      fontWeight: FontWeight.w600,
                                    ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: AppTokens.space24),
            FadeTransition(
              opacity: detailsOpacity,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  for (int i = 0; i < detailRows.length; i++)
                    staggeredRow(i, detailRows[i]),
                ],
              ),
            ),
            const SizedBox(height: AppTokens.space24),
          ],
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
      padding: const EdgeInsets.symmetric(vertical: AppTokens.space8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 120,
            child: Text(
              label,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: Theme.of(context).textTheme.bodyMedium,
              softWrap: true,
            ),
          ),
        ],
      ),
    );
  }
}
