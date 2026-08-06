import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/nova_colors.dart';

/// Security center (per design 33). Change PIN / password entry points.
class SecurityScreen extends StatelessWidget {
  const SecurityScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final items = <Widget>[
      _Item(
        icon: Icons.pin_outlined,
        title: 'Transaction PIN',
        subtitle: 'Used for transfers and withdrawals',
        onTap: () => ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('PIN change is coming soon.')),
        ),
      ),
      _Item(
        icon: Icons.lock_outline,
        title: 'Change password',
        subtitle: 'Update your account password',
        onTap: () => context.push('/change-password'),
      ),
    ];

    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('Security')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: NovaColors.secondaryContainer,
                borderRadius: BorderRadius.circular(16),
              ),
              child: const Row(
                children: [
                  Icon(Icons.shield_outlined, color: NovaColors.onSecondaryContainer),
                  SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      'Your account is protected by end-to-end encryption and a transaction PIN.',
                      style: TextStyle(color: NovaColors.onSecondaryContainer, height: 1.4),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
            Container(
              decoration: BoxDecoration(
                color: NovaColors.surfaceContainerLowest,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Column(children: items),
            ),
          ],
        ),
      ),
    );
  }
}

class _Item extends StatelessWidget {
  const _Item({required this.icon, required this.title, required this.subtitle, required this.onTap});

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: NovaColors.primary),
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
      subtitle: Text(subtitle),
      trailing: const Icon(Icons.chevron_right, color: NovaColors.onSurfaceVariant),
      onTap: onTap,
    );
  }
}
