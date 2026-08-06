import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/widgets.dart';
import '../../auth/providers/auth_provider.dart';
import '../providers/profile_provider.dart';

/// Profile tab (Tab #4 root) — per design 18-profile-settings.
class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authProvider).state;
    final profileAsync = ref.watch(profileProvider);
    final user = auth.user;

    final email = profileAsync.valueOrNull?.email ?? user?.email ?? '—';

    return Scaffold(
      backgroundColor: NovaColors.background,
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () => ref.refresh(profileProvider.future),
          child: ListView(
            padding: const EdgeInsets.all(20),
            children: [
              const SizedBox(height: 8),
              Row(
                children: [
                  InitialsAvatar(name: user?.fullName ?? 'N', size: 56),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          user?.fullName ?? 'NovaWallet user',
                          style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w700, color: NovaColors.onBackground),
                        ),
                        Text(
                          email,
                          style: const TextStyle(color: NovaColors.onSurfaceVariant),
                        ),
                      ],
                    ),
                  ),
                  TextButton(
                    onPressed: () => context.push('/profile/edit'),
                    child: const Text('Edit'),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              _Section(
                children: [
                  _Tile(icon: Icons.fact_check_outlined, title: 'KYC status', onTap: () => context.push('/kyc/status')),
                  _Tile(icon: Icons.shield_outlined, title: 'Security center', onTap: () => context.push('/security')),
                  _Tile(icon: Icons.request_quote_outlined, title: 'Fee estimator', onTap: () => context.push('/fees/estimate')),
                  _Tile(icon: Icons.receipt_long_outlined, title: 'Statements', onTap: () => context.push('/statements')),
                ],
              ),
              if (auth.isAdmin) ...[
                const SizedBox(height: 16),
                _Section(
                  children: [
                    _Tile(icon: Icons.admin_panel_settings_outlined, title: 'Admin console', onTap: () => context.push('/admin')),
                  ],
                ),
              ],
              const SizedBox(height: 16),
              _Section(
                children: [
                  _Tile(
                    icon: Icons.logout,
                    title: 'Log out',
                    destructive: true,
                    onTap: () async {
                      await ref.read(authProvider.notifier).logout();
                      if (context.mounted) context.go('/login');
                    },
                  ),
                ],
              ),
              const SizedBox(height: 32),
            ],
          ),
        ),
      ),
    );
  }
}

class _Section extends StatelessWidget {
  const _Section({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: NovaColors.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: NovaColors.shadowTint.withValues(alpha: 0.06),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(children: children),
    );
  }
}

class _Tile extends StatelessWidget {
  const _Tile({required this.icon, required this.title, required this.onTap, this.destructive = false});

  final IconData icon;
  final String title;
  final VoidCallback onTap;
  final bool destructive;

  @override
  Widget build(BuildContext context) {
    final color = destructive ? NovaColors.error : NovaColors.onSurface;
    return ListTile(
      leading: Icon(icon, color: color),
      title: Text(title, style: TextStyle(color: color, fontWeight: FontWeight.w600)),
      trailing: const Icon(Icons.chevron_right, color: NovaColors.onSurfaceVariant),
      onTap: onTap,
    );
  }
}
