import 'package:flutter/material.dart';

import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/widgets.dart';

/// Success state (per design 15) — shown after a deposit, transfer or withdraw.
/// Reads optional `title`, `amount` and `ref` query params.
class SuccessScreen extends StatelessWidget {
  const SuccessScreen({
    super.key,
    this.title = 'Done!',
    this.amount,
    this.reference,
  });

  final String title;
  final String? amount;
  final String? reference;

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
              Container(
                width: 96,
                height: 96,
                alignment: Alignment.center,
                decoration: const BoxDecoration(
                  color: NovaColors.secondaryContainer,
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.check_rounded, size: 52, color: NovaColors.onSecondaryContainer),
              ),
              const SizedBox(height: 24),
              Text(
                title,
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 26, fontWeight: FontWeight.w700, color: NovaColors.onBackground),
              ),
              if (amount != null) ...[
                const SizedBox(height: 12),
                Text(
                  'ZMW $amount',
                  textAlign: TextAlign.center,
                  style: const TextStyle(fontSize: 32, fontWeight: FontWeight.w700, color: NovaColors.success),
                ),
              ],
              if (reference != null) ...[
                const SizedBox(height: 8),
                Text(
                  'Ref: $reference',
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: NovaColors.onSurfaceVariant),
                ),
              ],
              const SizedBox(height: 40),
              PillButton(
                label: 'Done',
                onPressed: () => Navigator.of(context).popUntil((route) => route.isFirst),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
