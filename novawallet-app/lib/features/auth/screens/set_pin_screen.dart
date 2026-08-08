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
  int _pinResetToken = 0;
  String? _errorMessage;

  Future<void> _onComplete(String pin) async {
    if (!_confirmMode) {
      setState(() {
        _first = pin;
        _confirmMode = true;
        _pinResetToken++;
        _errorMessage = null;
      });
      return;
    }
    if (pin != _first) {
      setState(() {
        _first = null;
        _confirmMode = false;
        _pinResetToken++;
        _errorMessage = 'PINs do not match. Enter a new PIN to try again.';
      });
      return;
    }
    setState(() {
      _submitting = true;
      _errorMessage = null;
    });
    try {
      await ref.read(authProvider.notifier).setPin(pin);
      if (!mounted) return;
      context.go('/wallet');
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _first = null;
        _confirmMode = false;
        _submitting = false;
        _pinResetToken++;
        _errorMessage = e.displayMessage;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _first = null;
        _confirmMode = false;
        _submitting = false;
        _pinResetToken++;
        _errorMessage = 'Something went wrong. Please try again.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NovaColors.background,
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) => SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: BoxConstraints(
                minHeight: constraints.maxHeight - 48,
              ),
              child: IntrinsicHeight(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Spacer(),
                    const Icon(
                      Icons.pin_outlined,
                      size: 48,
                      color: NovaColors.primary,
                    ),
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
                      style: TextStyle(
                        color: NovaColors.onSurfaceVariant,
                        height: 1.4,
                      ),
                    ),
                    const SizedBox(height: 32),
                    _InlinePinError(message: _errorMessage),
                    if (_errorMessage != null) const SizedBox(height: 16),
                    PinPad(
                      length: 4,
                      confirmMode: _confirmMode,
                      resetToken: _pinResetToken,
                      onComplete: _submitting ? (_) {} : _onComplete,
                    ),
                    const Spacer(),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _InlinePinError extends StatelessWidget {
  const _InlinePinError({required this.message});

  final String? message;

  @override
  Widget build(BuildContext context) {
    return AnimatedSwitcher(
      duration: const Duration(milliseconds: 180),
      child: message == null
          ? const SizedBox.shrink()
          : Semantics(
              key: ValueKey(message),
              liveRegion: true,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(
                    Icons.error_outline,
                    size: 20,
                    color: NovaColors.error,
                  ),
                  const SizedBox(width: 8),
                  Flexible(
                    child: Text(
                      message!,
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        color: NovaColors.error,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ],
              ),
            ),
    );
  }
}
