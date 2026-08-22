import 'package:flutter/material.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/pin_pad.dart';
import '../models/fee_estimate.dart';
import '../models/transaction.dart';

/// Type-safe outcome of the PIN authorization sheet.
///
/// The sheet never reports raw errors/booleans back: callers switch on the
/// sealed variants to decide between staying, routing to the insufficient
/// recovery screen, or proceeding with the executed transaction.
sealed class PinOutcome {
  const PinOutcome();
}

/// User cancelled (or dismissed) the sheet before any request was sent.
class PinCancelled extends PinOutcome {
  const PinCancelled();
}

/// Definitive backend rejection (HTTP 400 insufficient balance). The sheet
/// closes and the caller routes to the recovery screen.
class PinInsufficient extends PinOutcome {
  const PinInsufficient();
}

/// The transfer executed successfully.
class PinSuccess extends PinOutcome {
  const PinSuccess(this.transaction);

  final WalletTransaction transaction;
}

/// Shows the transfer confirmation bottom sheet (per design 13-confirm-pin).
///
/// While [submitPin] is in flight the sheet is non-dismissible (drag, back,
/// and barrier are all blocked); a definitive insufficient-balance rejection
/// pops [PinInsufficient] so the caller can route to recovery, ambiguous or
/// retryable failures surface an inline error and rebuild the pad via an
/// incremented generation key.
Future<PinOutcome?> showTransferPinSheet(
  BuildContext context, {
  required String receiver,
  required String amount,
  required FeeEstimate fee,

  /// Authorizes the transfer with the entered PIN. Implementations should
  /// prepare their idempotency key before the request and rotate it after
  /// non-ambiguous failures so retries cannot double-spend.
  required Future<WalletTransaction> Function(String pin) submitPin,
}) {
  var pinGeneration = 0;
  var authorizing = false;
  String? inlineError;
  final total = (double.parse(amount) + double.parse(fee.totalFee))
      .toStringAsFixed(2);
  return showModalBottomSheet<PinOutcome>(
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
                                  : () => Navigator.of(ctx).pop(PinCancelled()),
                              child: const Text('Cancel'),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        const Text('Your PIN authorizes this transfer.'),
                        const SizedBox(height: 16),
                        _SummaryRow(label: 'Recipient', value: receiver),
                        _SummaryRow(label: 'Amount', value: formatZmw(amount)),
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
                                final transaction = await submitPin(pin);
                                if (ctx.mounted) {
                                  Navigator.of(
                                    ctx,
                                  ).pop(PinSuccess(transaction));
                                }
                              } on ApiException catch (e) {
                                if (e.isInsufficientBalance) {
                                  // Definitive business outcome: close the
                                  // sheet and route to recovery. Everything
                                  // else is retried inline in the sheet.
                                  if (ctx.mounted) {
                                    Navigator.of(ctx).pop(PinInsufficient());
                                  }
                                  return;
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
