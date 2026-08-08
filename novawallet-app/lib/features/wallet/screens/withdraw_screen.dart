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
    final amount = _amount.text.trim().replaceAll(',', '');
    if (double.tryParse(amount) == null || double.parse(amount) <= 0) {
      _toast('Enter a valid amount.');
      return;
    }
    final wallet = ref.read(walletProvider).valueOrNull;
    if (wallet == null) {
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
      final transaction = await _promptPin(
        walletId: wallet.id,
        amount: amount,
        description: desc,
        fee: fee,
        payload: payload,
      );
      if (transaction == null) return;
      await ref.read(walletProvider.notifier).refreshBalance();
      if (!mounted) return;
      _clearOperation();
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

  Future<WalletTransaction?> _promptPin({
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
    return showModalBottomSheet<WalletTransaction>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheetState) => SafeArea(
          child: SingleChildScrollView(
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
                          : () => Navigator.of(ctx).pop(),
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
                _SummaryRow(label: 'Amount', value: formatZmw(amount)),
                _SummaryRow(label: 'Fee', value: formatZmw(fee.totalFee)),
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
                            .withdraw(
                              walletId: walletId,
                              amount: amount,
                              description: description,
                              pin: pin,
                              idempotencyKey: _idempotencyKey,
                            );
                        if (ctx.mounted) Navigator.of(ctx).pop(transaction);
                      } on ApiException catch (e) {
                        if (!e.isNetworkError) _rotateOperationKey();
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
                              'Withdrawal failed. Check the transaction history before retrying.';
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
