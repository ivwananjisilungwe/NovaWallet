import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/widgets.dart';
import '../providers/auth_provider.dart';

/// Registration screen (per design 01-register) — email + password only,
/// plus an optional "Sign in with Google" button.
///
/// The backend `RegisterRequest` still requires first/last name and phone, so
/// the email local-part becomes the display name and a placeholder +260 number
/// is used. A later profile step collects real details.
class RegisterScreen extends ConsumerStatefulWidget {
  const RegisterScreen({super.key});

  @override
  ConsumerState<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends ConsumerState<RegisterScreen> {
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _obscure = true;
  bool _agree = false;
  bool _submitting = false;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final email = _email.text.trim();
    final pw = _password.text;
    if (email.isEmpty || pw.isEmpty) {
      _toast('Enter your email and password.');
      return;
    }
    if (pw.length < 8) {
      _toast('Password must be at least 8 characters.');
      return;
    }
    if (!_agree) {
      _toast('Please agree to the Terms & Conditions.');
      return;
    }
    final local = email.split('@').first;
    final names = local.split(RegExp(r'[._]'));
    final firstName = names.isNotEmpty ? _cap(names.first) : 'User';
    final second = names.length > 1 ? _cap(names.sublist(1).join(' ')) : '';

    setState(() => _submitting = true);
    try {
      await ref.read(authProvider.notifier).register(
            firstName: firstName,
            lastName: second.isEmpty ? firstName : second,
            email: email,
            phone: '+260700000000',
            password: pw,
          );
      if (!mounted) return;
      // New accounts start email-unverified; send them to the verify screen.
      context.go('/verify-email');
    } on ApiException catch (e) {
      if (mounted) _toast(e.displayMessage);
    } catch (_) {
      if (mounted) _toast('Registration failed. Please try again.');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  String _cap(String s) => s.isEmpty ? s : s[0].toUpperCase() + s.substring(1);

  void _toast(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NovaColors.background,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              IconButton(
                onPressed: () => Navigator.of(context).maybePop(),
                icon: const Icon(Icons.arrow_back, color: NovaColors.onSurface),
              ),
              const SizedBox(height: 8),
              const Text('Create your account',
                  style: TextStyle(fontSize: 28, fontWeight: FontWeight.w700, color: NovaColors.onBackground, letterSpacing: -0.02)),
              const SizedBox(height: 8),
              const Text('Open your free wallet in minutes',
                  style: TextStyle(fontSize: 16, color: NovaColors.onSurfaceVariant)),
              const SizedBox(height: 32),
              TextField(
                controller: _email,
                keyboardType: TextInputType.emailAddress,
                decoration: const InputDecoration(
                  labelText: 'Email address',
                  prefixIcon: Icon(Icons.mail_outline),
                  border: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(16))),
                ),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _password,
                obscureText: _obscure,
                decoration: InputDecoration(
                  labelText: 'Password',
                  prefixIcon: const Icon(Icons.lock_outline),
                  helperText: 'At least 8 characters with a number and a letter',
                  suffixIcon: IconButton(
                    icon: Icon(_obscure ? Icons.visibility_off : Icons.visibility),
                    onPressed: () => setState(() => _obscure = !_obscure),
                  ),
                  border: const OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(16))),
                ),
              ),
              const SizedBox(height: 8),
              CheckboxListTile(
                value: _agree,
                onChanged: (v) => setState(() => _agree = v ?? false),
                contentPadding: EdgeInsets.zero,
                title: const Text('I agree to the Terms & Conditions and Privacy Policy'),
                controlAffinity: ListTileControlAffinity.leading,
              ),
              const SizedBox(height: 8),
              PillButton(label: 'Create account', loading: _submitting, onPressed: _submit),
              const SizedBox(height: 20),
              const OrDivider(label: 'or sign up with'),
              const SizedBox(height: 4),
              GoogleSignInButton(
                onPressed: () => _toast('Google sign-in is coming soon.'),
              ),
              const SizedBox(height: 20),
              TextButton(
                onPressed: () => context.go('/login'),
                child: const Text('Already have an account? Log in'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
