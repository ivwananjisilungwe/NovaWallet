import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/nova_colors.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/widgets.dart';
import '../providers/kyc_provider.dart';

/// KYC status (per design 16). Reflects current tier + limits live from backend.
class KycStatusScreen extends ConsumerWidget {
  const KycStatusScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final statusAsync = ref.watch(kycStatusProvider);

    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('KYC status')),
      body: SafeArea(
        child: statusAsync.when(
          loading: () => const LoadingView(message: 'Loading…'),
          error: (e, _) => ErrorStateView(message: '$e'),
          data: (status) => ListView(
            padding: const EdgeInsets.all(20),
            children: [
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [NovaColors.primary, NovaColors.primaryContainer],
                  ),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Verification level',
                      style: TextStyle(color: Colors.white70, fontSize: 13),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      status.tierName ?? 'Unverified',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 22,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        _LimitTile('Wallet limit', formatZmw(status.walletLimit)),
                        const SizedBox(width: 20),
                        _LimitTile('Daily send limit', formatZmw(status.dailySendLimit)),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              if (status.isApproved)
                const _InfoBanner(
                  icon: Icons.verified_outlined,
                  color: NovaColors.success,
                  text: 'Your identity is verified.',
                )
              else if (status.isRejected)
                _InfoBanner(
                  icon: Icons.error_outline,
                  color: NovaColors.error,
                  text: status.rejectionReason ?? 'Verification was rejected. Re-submit your documents.',
                )
              else if (status.isPending)
                const _InfoBanner(
                  icon: Icons.hourglass_top,
                  color: NovaColors.warning,
                  text: 'Your documents are being reviewed. This usually takes 1-2 business days.',
                )
              else
                const _InfoBanner(
                  icon: Icons.info_outline,
                  color: NovaColors.primary,
                  text: 'Verify your identity to unlock higher limits and full wallet features.',
                ),
              if (!status.isApproved) ...[
                const SizedBox(height: 20),
                PillButton(
                  label: status.isRejected ? 'Re-submit documents' : 'Start verification',
                  onPressed: () => context.push('/kyc/upload'),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _LimitTile extends StatelessWidget {
  const _LimitTile(this.label, this.value);

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: const TextStyle(color: Colors.white70, fontSize: 12)),
          const SizedBox(height: 4),
          Text(value, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
        ],
      ),
    );
  }
}

class _InfoBanner extends StatelessWidget {
  const _InfoBanner({required this.icon, required this.color, required this.text});

  final IconData icon;
  final Color color;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          Icon(icon, color: color),
          const SizedBox(width: 12),
          Expanded(
            child: Text(text, style: const TextStyle(color: NovaColors.onSurface, height: 1.4)),
          ),
        ],
      ),
    );
  }
}
