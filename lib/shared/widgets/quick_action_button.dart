import 'package:flutter/material.dart';

import '../../core/design/app_tokens.dart';

/// Compact dashboard action with a clear icon and label.
class QuickActionButton extends StatelessWidget {
  const QuickActionButton({
    required this.label,
    required this.icon,
    required this.onTap,
    super.key,
  });

  final String label;
  final IconData icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;

    return Material(
      color: colors.surface,
      borderRadius: BorderRadius.circular(AppTokens.radiusLarge),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppTokens.radiusLarge),
        child: Padding(
          padding: const EdgeInsets.symmetric(
            vertical: AppTokens.space16,
            horizontal: AppTokens.space8,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: colors.primaryContainer,
                  borderRadius: BorderRadius.circular(AppTokens.radiusMedium),
                ),
                alignment: Alignment.center,
                child: Icon(icon, color: colors.onPrimaryContainer, size: 23),
              ),
              const SizedBox(height: AppTokens.space8),
              Text(
                label,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.labelMedium
                    ?.copyWith(fontWeight: FontWeight.w600),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
