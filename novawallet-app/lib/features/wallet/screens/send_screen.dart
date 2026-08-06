import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/pin_pad.dart';
import '../../../core/widgets/widgets.dart';
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
    final desc = _description.text.trim().isEmpty ? 'Transfer' : _description.text.trim();
    if (receiver.isEmpty || amount.isEmpty) {
      _toast('Enter the recipient wallet and amount.');
      return;
    }
    if (double.tryParse(amount) == null) {
      _toast('Enter a valid amount.');
      return;
    }
    setState(() => _submitting = true);
    try {
      // Collect the PIN before calling the backend.
      final pin = await _promptPin();
      if (pin == null) return;
      await ref.read(walletRepositoryProvider).transfer(
            receiverWalletId: receiver,
            amount: amount,
            description: desc,
            pin: pin,
            idempotencyKey: _generateKey(receiver, amount),
          );
      await ref.read(walletProvider.notifier).refreshBalance();
      if (!mounted) return;
      context.push('/success?title=Transfer sent&amount=$amount&ref=$receiver');
    } on ApiException catch (e) {
      if (e.isConflict && mounted) {
        context.push('/insufficient');
      } else if (mounted) {
        _toast(e.displayMessage);
      }
    } catch (_) {
      if (mounted) _toast('Transfer failed. Please try again.');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<String?> _promptPin() {
    return showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => Padding(
        padding: EdgeInsets.only(
          left: 16,
          right: 16,
          top: 24,
          bottom: MediaQuery.of(ctx).viewInsets.bottom + 24,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              'Enter your PIN',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700, color: NovaColors.onBackground),
            ),
            const SizedBox(height: 24),
            PinPad(
              length: 4,
              onComplete: (pin) => Navigator.of(ctx).pop(pin),
            ),
          ],
        ),
      ),
    );
  }

  String _generateKey(String receiver, String amount) =>
      'send-$receiver-$amount-${DateTime.now().millisecondsSinceEpoch}';

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
                  border: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(16))),
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
                  border: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(16))),
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
