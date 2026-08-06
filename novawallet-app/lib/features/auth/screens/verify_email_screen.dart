import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/widgets.dart';
import '../providers/auth_provider.dart';

/// Email-verification interstitial. Expects a `token` query param.
class VerifyEmailScreen extends ConsumerStatefulWidget {
  const VerifyEmailScreen({super.key, this.token});

  final String? token;

  @override
  ConsumerState<VerifyEmailScreen> createState() => _VerifyEmailScreenState();
}

class _VerifyEmailScreenState extends ConsumerState<VerifyEmailScreen> {
  bool _submitting = false;

  Future<void> _verify() async {
    final token = widget.token;
    if (token == null) return;
    setState(() => _submitting = true);
    try {
      await ref.read(authProvider.notifier).verifyEmail(token);
      if (!mounted) return;
      context.go('/pin/set');
    } on ApiException catch (e) {
      if (mounted) _toast(e.displayMessage);
    } finally {
      if (mounted) setState(() => _submitting = false);
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
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Icon(Icons.mark_email_read_outlined, size: 56, color: NovaColors.primary),
              const SizedBox(height: 20),
              const Text(
                'Verify your email',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 26, fontWeight: FontWeight.w700, color: NovaColors.onBackground),
              ),
              const SizedBox(height: 12),
              const Text(
                'We\'ve sent a verification link to your inbox. Open it, then come back and confirm here.',
                textAlign: TextAlign.center,
                style: TextStyle(color: NovaColors.onSurfaceVariant, height: 1.5),
              ),
              const SizedBox(height: 32),
              PillButton(
                label: 'I\'ve verified my email',
                loading: _submitting,
                onPressed: widget.token == null ? null : _verify,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
