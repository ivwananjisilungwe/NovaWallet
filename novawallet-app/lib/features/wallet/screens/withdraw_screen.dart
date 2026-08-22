import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/pin_pad.dart';
import '../../../core/widgets/widgets.dart';
import '../../transaction/models/fee_estimate.dart';
import '../../transaction/models/transaction.dart';
import '../../transaction/providers/transaction_provider.dart';
import '../models/wallet.dart';
import '../providers/wallet_provider.dart';

/// Withdraw funds (per design 11-withdraw). Requires the transaction PIN.
class WithdrawScreen extends ConsumerStatefulWidget {
  const WithdrawScreen({super.key});

  @override
  ConsumerState<WithdrawScreen> createState() => _WithdrawScreenState();
}

class _WithdrawScreenState extends ConsumerState<WithdrawScreen> {
  final _amount = TextEditingController();
  final _description = TextEditingController();
  bool _submitting = false;
  String? _operationPayload;
  String? _idempotencyKey;

  @override
  void dispose() {
    _amount.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    // Re-entry guard: disables the double-tap during the async wallet load.
    if (_submitting) return;
    final amount = _amount.text.trim().replaceAll(',', '');
    if (double.tryParse(amount) == null || double.parse(amount) <= 0) {
      _toast('Enter a valid amount.');
      return;
    }
    setState(() => _submitting = true);
    final wallet = await _readWallet();
    if (!mounted) return;
    if (wallet == null) {
      setState(() => _submitting = false);
      _toast('Wallet not loaded yet.');
      return;
    }
    final desc = _description.text.trim().isEmpty
        ? 'Withdrawal'
        : _description.text.trim();
    final payload = '${wallet.id}\u0000$amount\u0000$desc';
    _prepareOperation(payload);
    setState(() => _submitting = true);
    try {
      final fee = await ref
          .read(transactionRepositoryProvider)
          .estimateFee(type: 'WITHDRAWAL', amount: amount);
      if (!mounted) return;
      // The bottom sheet now owns the interaction state: the underlying
      // button is hidden behind the modal barrier, and its loading spinner
      // must stop — an indeterminate spinner would keep animating frames
      // underneath the sheet for the whole PIN flow.
      setState(() => _submitting = false);
      final outcome = await _promptPin(
        walletId: wallet.id,
        amount: amount,
        description: desc,
        fee: fee,
        payload: payload,
      );
      if (outcome == null) return;
      switch (outcome) {
        case _PinCancelled():
          return;
        case _PinInsufficient():
          _clearOperation();
          if (mounted) context.push('/insufficient');
          return;
        case _PinSuccess(:final transaction):
          _clearOperation();
          // Best-effort: refreshBalance is internally guarded and never throws.
          await ref.read(walletProvider.notifier).refreshBalance();
          if (!mounted) return;
          context.push(
            Uri(
              path: '/success',
              queryParameters: {
                'title': 'Withdrawal successful',
                'amount': transaction.amount,
                'ref': transaction.reference,
              },
            ).toString(),
          );
          return;
      }
    } on ApiException catch (e) {
      if (!e.isNetworkError) _clearOperation();
      if (mounted) _toast('Fee estimate unavailable. ${e.displayMessage}');
    } catch (_) {
      if (mounted) {
        _toast('Fee estimate unavailable. Please try again.');
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<Wallet?> _readWallet() async {
    final current = ref.read(walletProvider).valueOrNull;
    if (current != null) return current;
    try {
      return await ref.read(walletProvider.future);
    } catch (_) {
      return null;
    }
  }

  Future<_PinOutcome?> _promptPin({
    required String walletId,
    required String amount,
    required String description,
    required FeeEstimate fee,
    required String payload,
  }) {
    var pinGeneration = 0;
    var authorizing = false;
    String? inlineError;
    final total = (double.parse(amount) + double.parse(fee.totalFee))
        .toStringAsFixed(2);
    return showModalBottomSheet<_PinOutcome>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheetState) => PopScope(
          canPop: !authorizing,
          child: SafeArea(
            child: SingleChildScrollView(
              child: LayoutBuilder(
                builder: (ctx, constraints) => GestureDetector(
                  // While the withdrawal request is in flight the sheet must
                  // not be dismissible by dragging. This recognizer sits
                  // deeper than the sheet's own drag detector (and the scroll
                  // view), so it wins the gesture arena and swallows the
                  // gesture. Back and barrier dismissal are blocked by
                  // PopScope above. Outside the in-flight window the handlers
                  // are null and the sheet behaves normally.
                  onVerticalDragStart: authorizing ? (_) {} : null,
                  onVerticalDragUpdate: authorizing ? (_) {} : null,
                  onVerticalDragEnd: authorizing ? (_) {} : null,
                  child: ConstrainedBox(
                    constraints: constraints.maxHeight.isFinite
                        ? BoxConstraints(minHeight: constraints.maxHeight)
                        : const BoxConstraints(),
                    child: Padding(
                      padding: EdgeInsets.only(
                        left: 24,
                        right: 24,
                        top: 16,
                        bottom: MediaQuery.of(ctx).viewInsets.bottom + 24,
                      ),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Row(
                            children: [
                              const Expanded(
                                child: Text(
                                  'Confirm withdrawal',
                                  style: TextStyle(
                                    fontSize: 20,
                                    fontWeight: FontWeight.w700,
                                    color: NovaColors.onBackground,
                                  ),
                                ),
                              ),
                              TextButton(
                                onPressed: authorizing
                                    ? null
                                    : () => Navigator.of(
                                        ctx,
                                      ).pop(_PinCancelled()),
                                child: const Text('Cancel'),
                              ),
                            ],
                          ),
                          const SizedBox(height: 8),
                          const Text('Your PIN authorizes this withdrawal.'),
                          const SizedBox(height: 16),
                          _SummaryRow(
                            label: 'Destination',
                            value: 'Linked payout account',
                          ),
                          _SummaryRow(
                            label: 'Amount',
                            value: formatZmw(amount),
                          ),
                          _SummaryRow(
                            label: 'Fee',
                            value: formatZmw(fee.totalFee),
                          ),
                          _SummaryRow(
                            label: 'Total',
                            value: formatZmw(total),
                            strong: true,
                          ),
                          if (inlineError != null) ...[
                            const SizedBox(height: 12),
                            Text(
                              inlineError!,
                              textAlign: TextAlign.center,
                              style: const TextStyle(color: NovaColors.error),
                            ),
                          ],
                          const SizedBox(height: 16),
                          if (authorizing)
                            // Static, non-animating in-flight state: an
                            // indeterminate spinner would run forever and
                            // block any dismissal/lifecycle pumpAndSettle
                            // while the request is pending. Back, barrier and
                            // drag dismissal are blocked by PopScope and the
                            // gesture recognizer above, so a stable label is
                            // the correct affordance here.
                            const Center(
                              child: Padding(
                                padding: EdgeInsets.symmetric(vertical: 24),
                                child: Text(
                                  'Authorizing…',
                                  style: TextStyle(
                                    color: NovaColors.onSurfaceVariant,
                                  ),
                                ),
                              ),
                            )
                          else
                            PinPad(
                              key: ValueKey(pinGeneration),
                              length: 4,
                              onComplete: (pin) async {
                                setSheetState(() {
                                  authorizing = true;
                                  inlineError = null;
                                });
                                try {
                                  _prepareOperation(payload);
                                  final transaction = await ref
                                      .read(walletRepositoryProvider)
                                      .withdraw(
                                        walletId: walletId,
                                        amount: amount,
                                        description: description,
                                        pin: pin,
                                        idempotencyKey: _idempotencyKey,
                                      );
                                  if (ctx.mounted) {
                                    Navigator.of(
                                      ctx,
                                    ).pop(_PinSuccess(transaction));
                                  }
                                } on ApiException catch (e) {
                                  if (e.isInsufficientBalance) {
                                    // Definitive business outcome: close the
                                    // sheet and route to recovery. Everything
                                    // else is retried inline in the sheet.
                                    if (ctx.mounted) {
                                      Navigator.of(
                                        ctx,
                                      ).pop(_PinInsufficient());
                                    }
                                    return;
                                  }
                                  if (!e.isAmbiguous) {
                                    _rotateOperationKey();
                                  }
                                  if (!ctx.mounted) return;
                                  setSheetState(() {
                                    authorizing = false;
                                    inlineError = e.displayMessage;
                                    pinGeneration++;
                                  });
                                } catch (_) {
                                  if (!ctx.mounted) return;
                                  setSheetState(() {
                                    authorizing = false;
                                    inlineError =
                                        'Withdrawal failed. Check the '
                                        'transaction history before retrying.';
                                    pinGeneration++;
                                  });
                                }
                              },
                            ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  void _prepareOperation(String payload) {
    if (_operationPayload == payload && _idempotencyKey != null) return;
    _operationPayload = payload;
    _idempotencyKey = _newOpaqueKey();
  }

  void _rotateOperationKey() => _idempotencyKey = _newOpaqueKey();

  void _clearOperation() {
    _operationPayload = null;
    _idempotencyKey = null;
  }

  String _newOpaqueKey() {
    final random = Random.secure();
    return List.generate(
      24,
      (_) => random.nextInt(256).toRadixString(16).padLeft(2, '0'),
    ).join();
  }

  void _toast(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('Withdraw')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Cash out from your wallet',
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w700,
                  color: NovaColors.onBackground,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Withdraw to your linked mobile money or bank account.',
                style: TextStyle(color: NovaColors.onSurfaceVariant),
              ),
              const SizedBox(height: 24),
              AmountInput(
                controller: _amount,
                quickAmounts: ['50', '100', '250', '500'],
                onChanged: (_) => setState(() {}),
              ),
              const SizedBox(height: 20),
              TextField(
                controller: _description,
                decoration: const InputDecoration(
                  labelText: 'Description (optional)',
                  prefixIcon: Icon(Icons.notes),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.all(Radius.circular(16)),
                  ),
                ),
              ),
              const SizedBox(height: 28),
              PillButton(
                label: 'Withdraw',
                loading: _submitting,
                onPressed: _submit,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Type-safe outcome of the PIN authorization sheet.
///
/// The sheet never reports raw errors/booleans back: callers switch on the
/// sealed variants to decide between staying, routing to the insufficient
/// recovery screen, or proceeding with the executed transaction.
sealed class _PinOutcome {
  const _PinOutcome();
}

/// User cancelled (or dismissed) the sheet before any request was sent.
class _PinCancelled extends _PinOutcome {
  const _PinCancelled();
}

/// Definitive backend rejection (HTTP 400 insufficient balance). The sheet
/// closes and the caller routes to the recovery screen.
class _PinInsufficient extends _PinOutcome {
  const _PinInsufficient();
}

/// The withdrawal executed successfully.
class _PinSuccess extends _PinOutcome {
  const _PinSuccess(this.transaction);

  final WalletTransaction transaction;
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow({
    required this.label,
    required this.value,
    this.strong = false,
  });

  final String label;
  final String value;
  final bool strong;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Row(
      children: [
        Expanded(child: Text(label)),
        Flexible(
          child: Text(
            value,
            textAlign: TextAlign.end,
            style: TextStyle(
              fontWeight: strong ? FontWeight.w700 : FontWeight.w500,
            ),
          ),
        ),
      ],
    ),
  );
}
