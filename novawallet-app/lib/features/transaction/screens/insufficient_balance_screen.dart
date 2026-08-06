import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/widgets.dart';

/// Insufficient-balance state (per design 37).
class InsufficientBalanceScreen extends StatelessWidget {
  const InsufficientBalanceScreen({super.key});

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
              const Icon(Icons.error_outline, size: 64, color: NovaColors.error),
              const SizedBox(height: 20),
              const Text(
                'Insufficient balance',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 24, fontWeight: FontWeight.w700, color: NovaColors.onBackground),
              ),
              const SizedBox(height: 12),
              const Text(
                'You don\'t have enough funds for this transaction. Add money to continue.',
                textAlign: TextAlign.center,
                style: TextStyle(color: NovaColors.onSurfaceVariant, height: 1.5),
              ),
              const SizedBox(height: 32),
              PillButton(label: 'Deposit', onPressed: () => context.go('/deposit')),
              const SizedBox(height: 12),
              OutlinedButton(
                onPressed: () => Navigator.of(context).popUntil((r) => r.isFirst),
                style: OutlinedButton.styleFrom(
                  minimumSize: const Size.fromHeight(56),
                  shape: const StadiumBorder(),
                ),
                child: const Text('Back home'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
