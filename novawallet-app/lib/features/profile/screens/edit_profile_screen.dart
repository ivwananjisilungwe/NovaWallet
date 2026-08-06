import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/widgets.dart';
import '../data/user_repository.dart';
import '../providers/profile_provider.dart';

/// Edit profile (per design 19). Updates first/last name + phone via PUT /users/me.
class EditProfileScreen extends ConsumerStatefulWidget {
  const EditProfileScreen({super.key});

  @override
  ConsumerState<EditProfileScreen> createState() => _EditProfileScreenState();
}

class _EditProfileScreenState extends ConsumerState<EditProfileScreen> {
  final _first = TextEditingController();
  final _last = TextEditingController();
  final _phone = TextEditingController();
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    final p = ref.read(profileProvider).valueOrNull;
    _first.text = p?.firstName ?? '';
    _last.text = p?.lastName ?? '';
    _phone.text = p?.phone ?? '';
  }

  @override
  void dispose() {
    _first.dispose();
    _last.dispose();
    _phone.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    final repo = ref.read(userRepositoryProvider);
    final current = ref.read(profileProvider).valueOrNull;
    if (current == null) {
      _toast('Profile not loaded yet.');
      return;
    }
    setState(() => _submitting = true);
    try {
      final updated = await repo.updateProfile(
        UserProfile(
          id: current.id,
          firstName: _first.text.trim(),
          lastName: _last.text.trim(),
          email: current.email,
          phone: _phone.text.trim(),
          role: current.role,
        ),
      );
      ref.invalidate(profileProvider);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Profile updated')),
      );
      ref.read(profileProvider);
      context.pop(updated);
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
      appBar: AppBar(title: const Text('Edit profile')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _field('First name', _first, Icons.person_outline),
              const SizedBox(height: 16),
              _field('Last name', _last, Icons.person_outline),
              const SizedBox(height: 16),
              _field('Phone number', _phone, Icons.phone_outlined, phone: true),
              const SizedBox(height: 28),
              PillButton(label: 'Save changes', loading: _submitting, onPressed: _save),
            ],
          ),
        ),
      ),
    );
  }

  Widget _field(String label, TextEditingController c, IconData icon, {bool phone = false}) {
    return TextField(
      controller: c,
      keyboardType: phone ? TextInputType.phone : null,
      decoration: InputDecoration(
        labelText: label,
        prefixIcon: Icon(icon),
        border: const OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(16))),
      ),
    );
  }
}
