import 'package:flutter/material.dart';

import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/widgets.dart';

/// Virtual cards — Tab #3. Backend cards are V2/post-MVP, so this shows the
/// intended experience with a soft "coming soon" state (per design 20).
class CardsScreen extends StatelessWidget {
  const CardsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('My cards')),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Container(
                height: 200,
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [NovaColors.primary, NovaColors.primaryContainer],
                  ),
                  borderRadius: BorderRadius.circular(24),
                  boxShadow: [
                    BoxShadow(
                      color: NovaColors.shadowTint.withValues(alpha: 0.25),
                      blurRadius: 20,
                      offset: const Offset(0, 10),
                    ),
                  ],
                ),
                child: const Padding(
                  padding: EdgeInsets.all(24),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Icon(Icons.credit_card, color: NovaColors.onPrimary, size: 32),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Nova Virtual •••• •••• •••• 0000',
                            style: TextStyle(color: NovaColors.onPrimary, letterSpacing: 1.5),
                          ),
                          SizedBox(height: 6),
                          Text(
                            'Coming soon',
                            style: TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.w700,
                              color: NovaColors.onPrimary,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 28),
              const EmptyStateView(
                icon: Icons.credit_card_off_outlined,
                title: 'Virtual cards are on the way',
                message:
                    'We\'re building secure virtual cards for online payments. You\'ll be able to create and manage them here soon.',
              ),
            ],
          ),
        ),
      ),
    );
  }
}
