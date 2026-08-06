import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/pin_pad.dart';
import '../providers/auth_provider.dart';

/// Set-transaction-PIN screen (per design 06). First-run gate used by the
/// router before unlocking the wallet when the user has no PIN yet.
class SetPinScreen extends ConsumerStatefulWidget {
  const SetPinScreen({super.key});

  @override
  ConsumerState<SetPinScreen> createState() => _SetPinScreenState();
}

class _SetPinScreenState extends ConsumerState<SetPinScreen> {
  String? _first;
  bool _confirmMode = false;
  bool _submitting = false;

  Future<void> _onComplete(String pin) async {
    if (!_confirmMode) {
      setState(() {
        _first = pin;
        _confirmMode = true;
      });
      return;
    }
    if (pin != _first) {
      setState(() {
        _first = null;
        _confirmMode = false;
      });
      _toast('PINs do not match. Try again.');
      return;
    }
    setState(() => _submitting = true);
    try {
      await ref.read(authProvider.notifier).setPin(pin);
      if (!mounted) return;
      context.go('/wallet');
    } on ApiException catch (e) {
      if (mounted) _toast(e.displayMessage);
      setState(() {
        _first = null;
        _confirmMode = false;
        _submitting = false;
      });
    }
  }

  void _toast(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NovaColors.background,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Spacer(),
              const Icon(Icons.pin_outlined, size: 48, color: NovaColors.primary),
              const SizedBox(height: 16),
              Text(
                _confirmMode ? 'Confirm your PIN' : 'Set your PIN',
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 26,
                  fontWeight: FontWeight.w700,
                  color: NovaColors.onBackground,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Your 4-digit transaction PIN is required for transfers and withdrawals.',
                textAlign: TextAlign.center,
                style: TextStyle(color: NovaColors.onSurfaceVariant, height: 1.4),
              ),
              const SizedBox(height: 32),
              PinPad(
                length: 4,
                confirmMode: _confirmMode,
                onComplete: _submitting ? (_) {} : _onComplete,
              ),
              const Spacer(),
            ],
          ),
        ),
      ),
    );
  }
}
