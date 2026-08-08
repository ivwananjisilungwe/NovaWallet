import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/pin_pad.dart';
import '../providers/auth_provider.dart';

/// Change-transaction-PIN screen. Reached from the profile Security screen.
/// Three-phase flow: current PIN, then new PIN, then confirm. On success
/// shows a confirmation snackbar and pops back to the Security screen.
class ChangePinScreen extends ConsumerStatefulWidget {
  const ChangePinScreen({super.key});

  @override
  ConsumerState<ChangePinScreen> createState() => _ChangePinScreenState();
}

class _ChangePinScreenState extends ConsumerState<ChangePinScreen> {
  String? _currentPin;
  String? _newPin;
  bool _submitting = false;
  int _pinResetToken = 0;
  String? _errorMessage;

  Future<void> _onComplete(String pin) async {
    if (_currentPin == null) {
      setState(() {
        _currentPin = pin;
        _pinResetToken++;
        _errorMessage = null;
      });
      return;
    }
    if (_newPin == null) {
      setState(() {
        _newPin = pin;
        _pinResetToken++;
        _errorMessage = null;
      });
      return;
    }
    if (pin != _newPin) {
      // Confirmation mismatch — restart from the new-PIN step.
      setState(() {
        _newPin = null;
        _pinResetToken++;
        _errorMessage = 'PINs do not match. Enter the new PIN again.';
      });
      return;
    }
    setState(() {
      _submitting = true;
      _errorMessage = null;
    });
    try {
      await ref
          .read(authProvider.notifier)
          .changePin(currentPin: _currentPin!, newPin: _newPin!);
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('PIN changed successfully')));
      context.pop();
    } on ApiException catch (e) {
      if (!mounted) return;
      // e.g. wrong current PIN (400) — restart the whole flow.
      setState(() {
        _currentPin = null;
        _newPin = null;
        _pinResetToken++;
        _errorMessage = e.displayMessage;
      });
    } catch (_) {
      // Any other error (network, unexpected) — also restart the flow.
      if (!mounted) return;
      setState(() {
        _currentPin = null;
        _newPin = null;
        _pinResetToken++;
        _errorMessage = 'Something went wrong. Please try again.';
      });
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  String get _title => switch ((_currentPin, _newPin)) {
    (null, _) => 'Enter current PIN',
    (_, null) => 'Enter new PIN',
    _ => 'Confirm new PIN',
  };

  String get _subtitle => switch ((_currentPin, _newPin)) {
    (null, _) =>
      'Verify your identity with your current 4-digit transaction PIN.',
    (_, null) => 'Choose a new 4-digit transaction PIN.',
    _ => 'Enter the new PIN again to confirm.',
  };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('Change PIN')),
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
                      _title,
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontSize: 26,
                        fontWeight: FontWeight.w700,
                        color: NovaColors.onBackground,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      _subtitle,
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        color: NovaColors.onSurfaceVariant,
                        height: 1.4,
                      ),
                    ),
                    const SizedBox(height: 32),
                    _InlinePinError(message: _errorMessage),
                    if (_errorMessage != null) const SizedBox(height: 16),
                    PinPad(
                      length: 4,
                      confirmMode: _currentPin != null && _newPin != null,
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
