import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/widgets.dart';
import '../../transaction/models/fee_estimate.dart';
import '../../transaction/providers/transaction_provider.dart';
import '../providers/wallet_provider.dart';
import '../../transaction/widgets/send_balance_indicator.dart';
import '../../transaction/widgets/send_fee_estimate_card.dart';
import '../../transaction/widgets/transfer_pin_sheet.dart';

/// Send money (transfer) — per design 12-send-transfer + 13-confirm-pin.
///
/// Enhanced with balance display and fee estimation for fintech UX.
/// The step widgets (balance indicator, fee estimate card, PIN confirmation
/// sheet) live in `../widgets/`.
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

  // Fee estimation state
  FeeEstimate? _feeEstimate;
  bool _feeLoading = false;
  String? _feeError;
  Timer? _feeDebounceTimer;

  @override
  void dispose() {
    _feeDebounceTimer?.cancel();
    _recipient.dispose();
    _amount.dispose();
    _description.dispose();
    super.dispose();
  }

  /// Estimate fee when amount changes (debounced).
  void _onAmountChanged(String value) {
    setState(() {}); // Update balance indicator
    final amount = value.trim().replaceAll(',', '');
    if (amount.isEmpty ||
        double.tryParse(amount) == null ||
        double.parse(amount) <= 0) {
      setState(() {
        _feeEstimate = null;
        _feeError = null;
      });
      return;
    }
    // Debounce fee estimation
    _feeDebounceTimer?.cancel();
    _feeDebounceTimer = Timer(const Duration(milliseconds: 500), () {
      if (mounted && _amount.text.trim().replaceAll(',', '') == amount) {
        _estimateFee(amount);
      }
    });
  }

  Future<void> _estimateFee(String amount) async {
    setState(() {
      _feeLoading = true;
      _feeError = null;
    });
    try {
      final fee = await ref
          .read(transactionRepositoryProvider)
          .estimateFee(type: 'TRANSFER', amount: amount);
      if (mounted && _amount.text.trim().replaceAll(',', '') == amount) {
        setState(() {
          _feeEstimate = fee;
          _feeLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _feeError = 'Could not load fee';
          _feeLoading = false;
        });
      }
    }
  }

  Future<void> _startTransfer() async {
    // Re-entry guard: disables the double-tap while the fee estimate is being
    // fetched (before the PIN sheet takes over the interaction).
    if (_submitting) return;
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
        case PinCancelled():
          return;
        case PinInsufficient():
          _clearOperation();
          if (mounted) context.push('/insufficient');
          return;
        case PinSuccess(:final transaction):
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

  Future<PinOutcome?> _promptPin({
    required String receiver,
    required String amount,
    required String description,
    required FeeEstimate fee,
    required String payload,
  }) {
    return showTransferPinSheet(
      context,
      receiver: receiver,
      amount: amount,
      fee: fee,
      submitPin: (pin) async {
        _prepareOperation(payload);
        try {
          return await ref
              .read(walletRepositoryProvider)
              .transfer(
                receiverWalletId: receiver,
                amount: amount,
                description: description,
                pin: pin,
                idempotencyKey: _idempotencyKey,
              );
        } on ApiException catch (e) {
          // Non-ambiguous failures get a fresh operation key so the retry
          // cannot replay the rejected request.
          if (!e.isAmbiguous) _rotateOperationKey();
          rethrow;
        }
      },
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
    final walletAsync = ref.watch(walletProvider);
    final wallet = walletAsync.valueOrNull;
    final balance = double.tryParse(wallet?.balance ?? '0') ?? 0;

    // Calculate amount for balance check
    final amountText = _amount.text.trim().replaceAll(',', '');
    final amount = double.tryParse(amountText) ?? 0;
    final fee = _feeEstimate != null
        ? (double.tryParse(_feeEstimate!.totalFee) ?? 0)
        : 0;
    final total = amount + fee;
    final hasInsufficientBalance = total > balance && amount > 0;

    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('Send money')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Balance indicator
              SendBalanceIndicator(
                balance: balance,
                isLoading: walletAsync.isLoading,
              ),
              const SizedBox(height: 24),
              TextField(
                controller: _recipient,
                decoration: InputDecoration(
                  labelText: 'Recipient wallet ID',
                  prefixIcon: const Icon(Icons.person_outline),
                  suffixIcon: _recipient.text.isNotEmpty
                      ? IconButton(
                          icon: const Icon(Icons.clear, size: 20),
                          onPressed: () {
                            _recipient.clear();
                            setState(() {});
                          },
                        )
                      : null,
                  border: const OutlineInputBorder(
                    borderRadius: BorderRadius.all(Radius.circular(16)),
                  ),
                ),
                onChanged: (_) => setState(() {}),
              ),
              const SizedBox(height: 20),
              AmountInput(
                controller: _amount,
                quickAmounts: ['20', '50', '100', '200'],
                onChanged: _onAmountChanged,
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
              const SizedBox(height: 20),
              // Fee estimate card
              if (amount > 0) ...[
                SendFeeEstimateCard(
                  feeEstimate: _feeEstimate,
                  isLoading: _feeLoading,
                  error: _feeError,
                  hasInsufficientBalance: hasInsufficientBalance,
                  total: total,
                ),
                const SizedBox(height: 20),
              ],
              const SizedBox(height: 8),
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
