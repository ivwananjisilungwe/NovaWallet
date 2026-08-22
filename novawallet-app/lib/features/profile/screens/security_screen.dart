import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/nova_colors.dart';
import '../../auth/providers/biometric_provider.dart';

/// Security center (per design 33). Change PIN / password entry points plus
/// the optional biometric-unlock toggle.
class SecurityScreen extends ConsumerWidget {
  const SecurityScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final bio = ref.watch(biometricProvider);

    final items = <Widget>[
      _Item(
        icon: Icons.pin_outlined,
        title: 'Transaction PIN',
        subtitle: 'Used for transfers and withdrawals',
        onTap: () => context.push('/pin/change'),
      ),
      _Item(
        icon: Icons.lock_outline,
        title: 'Change password',
        subtitle: 'Update your account password',
        onTap: () => context.push('/change-password'),
      ),
    ];

    // Capability-gated visibility:
    //   * unsupported hardware            -> tile hidden entirely,
    //   * supported but nothing enrolled  -> visible but disabled,
    //   * fully usable                    -> active switch.
    final Widget? biometricTile = !bio.state.checked
        ? null
        : !bio.state.deviceSupported
            ? null
            : SwitchListTile(
                secondary: Icon(
                  Icons.fingerprint,
                  color: bio.state.usable
                      ? NovaColors.primary
                      : NovaColors.onSurfaceVariant,
                ),
                title: const Text(
                  'Biometric unlock',
                  style: TextStyle(fontWeight: FontWeight.w600),
                ),
                subtitle: Text(
                  bio.state.usable
                      ? 'Use fingerprint or Face ID to open the app'
                      : 'No biometrics enrolled on this device',
                ),
                value: bio.state.enabled && bio.state.usable,
                onChanged: bio.state.usable
                    ? (value) => ref
                        .read(biometricProvider.notifier)
                        .setEnabled(value)
                    : null,
              );

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
            if (biometricTile != null) ...[
              Container(
                decoration: BoxDecoration(
                  color: NovaColors.surfaceContainerLowest,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: biometricTile,
              ),
              const SizedBox(height: 20),
            ],
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
