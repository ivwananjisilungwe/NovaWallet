import 'package:flutter/material.dart';

import '../theme/nova_colors.dart';

/// Rounded avatar circle with initials (no network dependency).
class InitialsAvatar extends StatelessWidget {
  const InitialsAvatar({super.key, required this.name, this.size = 40});

  final String name;
  final double size;

  @override
  Widget build(BuildContext context) {
    final initials = name
        .split(' ')
        .where((p) => p.isNotEmpty)
        .take(2)
        .map((p) => p[0].toUpperCase())
        .join();
    return Container(
      width: size,
      height: size,
      decoration: const BoxDecoration(
        color: NovaColors.surfaceContainerHigh,
        shape: BoxShape.circle,
      ),
      alignment: Alignment.center,
      child: Text(
        initials,
        style: TextStyle(
          fontSize: size * 0.38,
          fontWeight: FontWeight.w700,
          color: NovaColors.primary,
        ),
      ),
    );
  }
}
