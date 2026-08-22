import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/nova_colors.dart';

/// Single recent-transaction row: colored leading icon, title, subtitle,
/// and right-aligned signed amount (green in / red out).
///
/// When [heroTag] is provided the leading icon is wrapped in a [Hero] so
/// it can animate into [TransactionDetailScreen] — pass `tx.reference` as
/// the tag so each tile has a unique hero.
class TransactionTile extends StatelessWidget {
  const TransactionTile({
    super.key,
    required this.title,
    required this.subtitle,
    required this.amountText,
    required this.incoming,
    this.icon,
    this.onTap,
    this.heroTag,
  });

  final String title;
  final String subtitle;
  final String amountText;
  final bool incoming;
  final IconData? icon;
  final VoidCallback? onTap;

  /// Optional hero tag for shared-element transition. When set the leading
  /// icon is wrapped in a [Hero] whose tag is `tx_icon_$heroTag`.
  final String? heroTag;

  void _handleTap() {
    if (onTap == null) return;
    HapticFeedback.selectionClick();
    onTap!();
  }

  @override
  Widget build(BuildContext context) {
    final color = incoming ? NovaColors.success : NovaColors.error;
    final resolvedIcon =
        icon ?? (incoming ? Icons.arrow_downward : Icons.arrow_upward);

    Widget iconWidget = Container(
      width: 40,
      height: 40,
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        shape: BoxShape.circle,
      ),
      child: Icon(resolvedIcon, size: 20, color: color),
    );

    // Wrap in Hero when a tag is provided so the detail screen can animate
    // the icon. Each transaction's reference is unique so the tag is unique.
    if (heroTag != null) {
      iconWidget = Hero(tag: 'tx_icon_$heroTag', child: iconWidget);
    }

    return Material(
      color: NovaColors.surfaceContainerLowest,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap == null ? null : _handleTap,
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: NovaColors.surfaceContainerLow),
          ),
          child: Row(
            children: [
              // Leading colored icon (optionally hero-wrapped).
              iconWidget,
              const SizedBox(width: 12),
              // Title + subtitle.
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: NovaColors.onSurface,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: const TextStyle(
                        fontSize: 12,
                        color: NovaColors.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              // Signed amount.
              Text(
                amountText,
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: color,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
