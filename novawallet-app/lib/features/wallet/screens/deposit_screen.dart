import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/widgets.dart';
import '../providers/wallet_provider.dart';

/// Deposit funds (per design 10-deposit). No PIN required for money-in.
class DepositScreen extends ConsumerStatefulWidget {
  const DepositScreen({super.key});

  @override
  ConsumerState<DepositScreen> createState() => _DepositScreenState();
}

class _DepositScreenState extends ConsumerState<DepositScreen> {
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
        ? 'Deposit'
        : _description.text.trim();
    final payload = '${wallet.id}\u0000$amount\u0000$desc';
    _prepareOperation(payload);
    setState(() => _submitting = true);
    try {
      final transaction = await ref
          .read(walletRepositoryProvider)
          .deposit(
            walletId: wallet.id,
            amount: amount,
            description: desc,
            idempotencyKey: _idempotencyKey,
          );
      await ref.read(walletProvider.notifier).refreshBalance();
      if (!mounted) return;
      _clearOperation();
      context.push(
        Uri(
          path: '/success',
          queryParameters: {
            'title': 'Deposit successful',
            'amount': transaction.amount,
            'ref': transaction.reference,
          },
        ).toString(),
      );
    } on ApiException catch (e) {
      if (!e.isNetworkError) _clearOperation();
      if (mounted) _toast(e.displayMessage);
    } catch (_) {
      if (mounted) {
        _toast(
          'Deposit status is uncertain. Check the transaction history before retrying.',
        );
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  void _prepareOperation(String payload) {
    if (_operationPayload == payload && _idempotencyKey != null) return;
    _operationPayload = payload;
    _idempotencyKey = _newOpaqueKey();
  }

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
      appBar: AppBar(title: const Text('Deposit')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Add money to your wallet',
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w700,
                  color: NovaColors.onBackground,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Fund via mobile money or card in seconds.',
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
                label: 'Deposit',
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
