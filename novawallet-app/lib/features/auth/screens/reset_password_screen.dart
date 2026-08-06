import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/widgets.dart';
import '../providers/auth_provider.dart';

/// Reset-password screen (per design 04). Expects a `token` query param.
class ResetPasswordScreen extends ConsumerStatefulWidget {
  const ResetPasswordScreen({super.key, this.token});

  final String? token;

  @override
  ConsumerState<ResetPasswordScreen> createState() => _ResetPasswordScreenState();
}

class _ResetPasswordScreenState extends ConsumerState<ResetPasswordScreen> {
  final _password = TextEditingController();
  final _confirm = TextEditingController();
  bool _obscure = true;
  bool _submitting = false;
  bool _done = false;

  @override
  void dispose() {
    _password.dispose();
    _confirm.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final p = _password.text;
    final c = _confirm.text;
    if (p.length < 8) {
      _toast('Password must be at least 8 characters.');
      return;
    }
    if (p != c) {
      _toast('Passwords do not match.');
      return;
    }
    if (widget.token == null) {
      _toast('Missing reset token.');
      return;
    }
    setState(() => _submitting = true);
    try {
      await ref.read(authProvider.notifier).resetPassword(
            token: widget.token!,
            newPassword: p,
          );
      if (mounted) setState(() => _done = true);
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
    if (_done) {
      return Scaffold(
        backgroundColor: NovaColors.background,
        body: SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(32),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.check_circle_outline, size: 56, color: NovaColors.success),
                const SizedBox(height: 16),
                const Text('Password updated', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w700)),
                const SizedBox(height: 24),
                PillButton(label: 'Log in', onPressed: () => context.go('/login')),
              ],
            ),
          ),
        ),
      );
    }
    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('Reset password')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _field('New password', _password, Icons.lock_outline, obscure: _obscure,
                  onToggle: () => setState(() => _obscure = !_obscure)),
              const SizedBox(height: 16),
              _field('Confirm new password', _confirm, Icons.lock_outline, obscure: _obscure),
              const SizedBox(height: 24),
              PillButton(label: 'Update password', loading: _submitting, onPressed: _submit),
            ],
          ),
        ),
      ),
    );
  }

  Widget _field(String label, TextEditingController c, IconData icon,
      {bool obscure = false, VoidCallback? onToggle}) {
    return TextField(
      controller: c,
      obscureText: obscure,
      decoration: InputDecoration(
        labelText: label,
        prefixIcon: Icon(icon),
        suffixIcon: onToggle != null
            ? IconButton(
                icon: Icon(obscure ? Icons.visibility_off : Icons.visibility),
                onPressed: onToggle,
              )
            : null,
        border: const OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(16))),
      ),
    );
  }
}
