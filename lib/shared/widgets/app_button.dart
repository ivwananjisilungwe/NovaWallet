import 'package:flutter/material.dart';

import '../../core/design/app_tokens.dart';

/// Consistent primary and secondary action button for NovaWallet.
///
/// Use primary actions for the main action on a screen and secondary
/// actions when an action is useful but should receive less emphasis.
class AppButton extends StatelessWidget {
  const AppButton({
    required this.label,
    required this.onPressed,
    this.icon,
    this.isLoading = false,
    this.isExpanded = true,
    this.variant = AppButtonVariant.primary,
    super.key,
  });

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool isLoading;
  final bool isExpanded;
  final AppButtonVariant variant;

  @override
  Widget build(BuildContext context) {
    final child = AnimatedSwitcher(
      duration: const Duration(milliseconds: 180),
      child: isLoading
          ? SizedBox(
              key: const ValueKey('loading'),
              width: 20,
              height: 20,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: variant == AppButtonVariant.primary
                    ? Theme.of(context).colorScheme.onPrimary
                    : Theme.of(context).colorScheme.primary,
              ),
            )
          : Row(
              key: const ValueKey('content'),
              mainAxisSize: MainAxisSize.min,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                if (icon != null) ...[
                  Icon(icon, size: 18),
                  const SizedBox(width: AppTokens.space8),
                ],
                Text(label),
              ],
            ),
    );

    final button = variant == AppButtonVariant.primary
        ? FilledButton(onPressed: isLoading ? null : onPressed, child: child)
        : OutlinedButton(onPressed: isLoading ? null : onPressed, child: child);

    return SizedBox(
      width: isExpanded ? double.infinity : null,
      height: AppTokens.buttonHeight,
      child: button,
    );
  }
}

enum AppButtonVariant { primary, secondary }
