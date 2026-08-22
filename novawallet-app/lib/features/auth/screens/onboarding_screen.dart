import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/widgets.dart';

/// First-run onboarding (per Stitch novawallet_welcome_1).
/// Single trust-led page: brand, hero, feature pills, CTAs.
class OnboardingScreen extends StatelessWidget {
  const OnboardingScreen({super.key});

  static const _features = [
    (icon: Icons.bolt, label: 'Instant transfers'),
    (icon: Icons.shield_outlined, label: 'Bank-grade security'),
    (icon: Icons.credit_card_outlined, label: 'Virtual cards'),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NovaColors.background,
      body: SafeArea(
        child: SingleChildScrollView(
          child: Column(
            children: [
              const SizedBox(height: 32),
              // Top logo
              Container(
                width: 72,
                height: 72,
                decoration: const BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [NovaColors.primary, NovaColors.primaryContainer],
                  ),
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.account_balance_wallet,
                  color: NovaColors.onPrimary,
                  size: 36,
                ),
              ),
              // Hero copy
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Column(
                  children: [
                    Text(
                      'Your money, simplified.',
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.w700,
                        height: 1.33,
                        letterSpacing: -0.01,
                        color: NovaColors.primary,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Send, receive, deposit and withdraw — all from your phone, right here in Zambia.',
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontSize: 16,
                        height: 1.5,
                        color: NovaColors.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 32),
              // Hero illustration (gradient placeholder)
              Container(
                width: 220,
                height: 220,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: RadialGradient(
                    colors: [
                      NovaColors.primaryContainer.withValues(alpha: 0.25),
                      NovaColors.primaryContainer.withValues(alpha: 0.0),
                    ],
                  ),
                ),
                child: const Icon(
                  Icons.account_balance_wallet,
                  size: 100,
                  color: NovaColors.primaryContainer,
                ),
              ),
              const SizedBox(height: 32),
              // Feature pills
              Wrap(
                alignment: WrapAlignment.center,
                spacing: 8,
                runSpacing: 8,
                children: [
                  for (final f in _features)
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 8,
                      ),
                      decoration: BoxDecoration(
                        color: NovaColors.surfaceContainerLow,
                        borderRadius: BorderRadius.circular(999),
                        border: Border.all(
                          color: NovaColors.surfaceContainerHigh,
                        ),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(f.icon, size: 16, color: NovaColors.primary),
                          const SizedBox(width: 6),
                          Text(
                            f.label,
                            style: const TextStyle(
                              fontSize: 14,
                              color: NovaColors.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 24),
              // CTAs
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Column(
                  children: [
                    PillButton(
                      label: 'Get started',
                      onPressed: () => context.go('/register'),
                    ),
                    const SizedBox(height: 12),
                    PillButton(
                      label: 'I already have an account',
                      variant: PillButtonVariant.outlined,
                      onPressed: () => context.push('/login'),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              // Footer
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Text(
                  'By continuing you agree to our Terms & Privacy Policy.',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 12,
                    color: NovaColors.onSurfaceVariant.withValues(alpha: 0.8),
                  ),
                ),
              ),
              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }
}
