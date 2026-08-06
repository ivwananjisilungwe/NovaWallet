import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/pin_pad.dart';
import '../../../core/widgets/widgets.dart';
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
    final pin = await _promptPin();
    if (pin == null) return;
    final desc = _description.text.trim().isEmpty ? 'Withdrawal' : _description.text.trim();
    setState(() => _submitting = true);
    try {
      await ref.read(walletRepositoryProvider).withdraw(
            walletId: wallet.id,
            amount: amount,
            description: desc,
            pin: pin,
            idempotencyKey: 'wd-${DateTime.now().millisecondsSinceEpoch}',
          );
      await ref.read(walletProvider.notifier).refreshBalance();
      if (!mounted) return;
      context.push('/success?title=Withdrawal successful&amount=$amount');
    } on ApiException catch (e) {
      if (e.isConflict && mounted) {
        context.push('/insufficient');
      } else if (mounted) {
        _toast(e.displayMessage);
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<String?> _promptPin() {
    return showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => Padding(
        padding: EdgeInsets.only(left: 16, right: 16, top: 24, bottom: MediaQuery.of(ctx).viewInsets.bottom + 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              'Enter your PIN',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700, color: NovaColors.onBackground),
            ),
            const SizedBox(height: 24),
            PinPad(length: 4, onComplete: (pin) => Navigator.of(ctx).pop(pin)),
          ],
        ),
      ),
    );
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
                style: TextStyle(fontSize: 22, fontWeight: FontWeight.w700, color: NovaColors.onBackground),
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
                  border: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(16))),
                ),
              ),
              const SizedBox(height: 28),
              PillButton(label: 'Withdraw', loading: _submitting, onPressed: _submit),
            ],
          ),
        ),
      ),
    );
  }
}
