import 'package:flutter/material.dart';

import '../theme/nova_colors.dart';

/// Horizontal "or continue with X" divider line.
class OrDivider extends StatelessWidget {
  const OrDivider({super.key, required this.label, this.padding = 20});

  final String label;
  final double padding;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.symmetric(vertical: padding),
      child: Row(
        children: [
          const Expanded(child: Divider(color: NovaColors.outlineVariant)),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Text(
              label,
              style: const TextStyle(
                color: NovaColors.onSurfaceVariant,
                fontSize: 13,
              ),
            ),
          ),
          const Expanded(child: Divider(color: NovaColors.outlineVariant)),
        ],
      ),
    );
  }
}
