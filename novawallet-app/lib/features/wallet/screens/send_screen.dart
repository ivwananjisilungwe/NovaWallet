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
import '../providers/wallet_provider.dart';

/// Send money (transfer) — per design 12-send-transfer + 13-confirm-pin.
class SendScreen extends ConsumerStatefulWidget {
  const SendScreen({super.key});

  @override
  ConsumerState<SendScreen> createState() => _SendScreenState();
}

class _SendScreenState extends ConsumerState<SendScreen> {
  final _recipient = TextEditingController();
  final _amount = TextEditingController();
  final _description = TextEditingController();
  bool _submitting = false;
  String? _operationPayload;
  String? _idempotencyKey;

  @override
  void dispose() {
    _recipient.dispose();
    _amount.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _startTransfer() async {
    final receiver = _recipient.text.trim();
    final amount = _amount.text.trim().replaceAll(',', '');
    final desc = _description.text.trim().isEmpty
        ? 'Transfer'
        : _description.text.trim();
    if (receiver.isEmpty || amount.isEmpty) {
      _toast('Enter the recipient wallet and amount.');
      return;
    }
    if (double.tryParse(amount) == null || double.parse(amount) <= 0) {
      _toast('Enter a valid amount.');
      return;
    }
    final payload = '$receiver\u0000$amount\u0000$desc';
    _prepareOperation(payload);
    setState(() => _submitting = true);
    try {
      final fee = await ref
          .read(transactionRepositoryProvider)
          .estimateFee(type: 'TRANSFER', amount: amount);
      if (!mounted) return;
      final outcome = await _promptPin(
        receiver: receiver,
        amount: amount,
        description: desc,
        fee: fee,
        payload: payload,
      );
      if (outcome == null) return;
      switch (outcome) {
        case _PinCancelled():
          return;
        case _PinConflict():
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
                'title': 'Transfer sent',
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

  Future<_PinOutcome?> _promptPin({
    required String receiver,
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
                  // While the transfer request is in flight the sheet must not
                  // be dismissible by dragging. This recognizer sits deeper
                  // than the sheet's own drag detector (and the scroll view),
                  // so it wins the gesture arena and swallows the gesture.
                  // Back and barrier dismissal are blocked by PopScope above.
                  // Outside the in-flight window the handlers are null and the
                  // sheet behaves normally (scroll + drag to dismiss).
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
                                  'Confirm transfer',
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
                          const Text('Your PIN authorizes this transfer.'),
                          const SizedBox(height: 16),
                          _SummaryRow(label: 'Recipient', value: receiver),
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
                            const Center(child: CircularProgressIndicator())
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
                                      .transfer(
                                        receiverWalletId: receiver,
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
                                  if (e.isConflict) {
                                    // Insufficient balance is a definitive business
                                    // outcome: close the sheet and route to recovery.
                                    if (ctx.mounted) {
                                      Navigator.of(ctx).pop(_PinConflict());
                                    }
                                    return;
                                  }
                                  if (!_isAmbiguous(e)) _rotateOperationKey();
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
                                        'Transfer failed. Check the transaction history before retrying.';
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

  /// Errors where the request may or may not have executed: network failure,
  /// server-side 5xx, timeouts (408) and rate limits (429) must keep the same
  /// idempotency key so a retry cannot double-execute. Definitive 4xx
  /// business failures rotate the key instead.
  bool _isAmbiguous(ApiException e) =>
      e.isNetworkError ||
      (e.statusCode != null &&
          (e.statusCode! >= 500 ||
              e.statusCode! == 408 ||
              e.statusCode! == 429));

  void _toast(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('Send money')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextField(
                controller: _recipient,
                decoration: const InputDecoration(
                  labelText: 'Recipient wallet ID',
                  prefixIcon: Icon(Icons.person_outline),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.all(Radius.circular(16)),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              AmountInput(
                controller: _amount,
                quickAmounts: ['20', '50', '100', '200'],
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
                label: 'Continue',
                loading: _submitting,
                onPressed: _startTransfer,
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

/// Definitive backend rejection (HTTP 409 conflict — e.g. insufficient
/// balance). The sheet closes and the caller routes to the recovery screen.
class _PinConflict extends _PinOutcome {
  const _PinConflict();
}

/// The transfer executed successfully.
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
