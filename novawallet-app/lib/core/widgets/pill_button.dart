import 'package:flutter/material.dart';

import '../theme/nova_colors.dart';

/// Pill-shaped fill button matching DESIGN.md primary buttons.
///
/// Full-width by default (`minimumSize: Size.fromHeight(56)`), indigo fill,
/// white label, subtle press-scale feedback.
class PillButton extends StatelessWidget {
  const PillButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.filled = true,
    this.loading = false,
    this.expanded = true,
    this.errorColor = false,
  });

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool filled;

  /// Show a small spinner instead of the label while [loading].
  final bool loading;
  final bool expanded;
  final bool errorColor;

  @override
  Widget build(BuildContext context) {
    final child = loading
        ? const SizedBox(
            width: 22,
            height: 22,
            child: CircularProgressIndicator(strokeWidth: 2.5),
          )
        : Row(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (icon != null) ...[
                Icon(icon, size: 20),
                const SizedBox(width: 8),
              ],
              Text(label, style: const TextStyle(fontSize: 16)),
            ],
          );

    final shape = const StadiumBorder();
    final onClicked = loading ? null : onPressed;

    if (filled) {
      return FilledButton(
        onPressed: onClicked,
        style: FilledButton.styleFrom(
          minimumSize: expanded ? const Size.fromHeight(56) : null,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
          shape: shape,
          backgroundColor: errorColor ? NovaColors.error : null,
        ),
        child: child,
      );
    }
    return OutlinedButton(
      onPressed: onClicked,
      style: OutlinedButton.styleFrom(
        minimumSize: expanded ? const Size.fromHeight(56) : null,
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
        shape: shape,
        side: BorderSide(
          color: errorColor ? NovaColors.error : NovaColors.outlineVariant,
          width: 2,
        ),
      ),
      child: child,
    );
  }
}