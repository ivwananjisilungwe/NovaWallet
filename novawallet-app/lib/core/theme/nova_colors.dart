import 'package:flutter/material.dart';

/// Design tokens captured verbatim from
/// `stitch_duplicate_of_novawallet/novawallet/DESIGN.md` (Material 3 roles).
///
/// Brand: "Modern Trust" — Corporate Modern, light-first, mobile-first.
class NovaColors {
  NovaColors._();

  // ---- Core brand anchors ----
  static const Color primary = Color(0xFF3525CD); // buttons / active / brand
  static const Color primaryContainer = Color(0xFF4F46E5); // indigo-600 (gradient anchor)
  static const Color onPrimary = Color(0xFFFFFFFF);
  static const Color onPrimaryContainer = Color(0xFFDAD7FF);

  static const Color secondary = Color(0xFF006C49); // deep emerald
  static const Color secondaryContainer = Color(0xFF6CF8BB); // light emerald (money-in)
  static const Color onSecondary = Color(0xFFFFFFFF);
  static const Color onSecondaryContainer = Color(0xFF00714D);

  static const Color success = Color(0xFF10B981); // emerald — money-in / success

  static const Color tertiary = Color(0xFF95002B);
  static const Color tertiaryContainer = Color(0xFFBF0F3C);

  static const Color error = Color(0xFFBA1A1A);
  static const Color errorContainer = Color(0xFFFFDAD6);
  static const Color onError = Color(0xFFFFFFFF);
  static const Color onErrorContainer = Color(0xFF93000A);

  static const Color warning = Color(0xFFF59E0B); // amber — pending / KYC

  // Surfaces (slate-tinted light)
  static const Color background = Color(0xFFF8F9FF);
  static const Color onBackground = Color(0xFF0B1C30);
  static const Color surface = Color(0xFFF8F9FF);
  static const Color surfaceContainerLowest = Color(0xFFFFFFFF);
  static const Color surfaceContainerLow = Color(0xFFEFF4FF);
  static const Color surfaceContainer = Color(0xFFE5EEFF);
  static const Color surfaceContainerHigh = Color(0xFFDCE9FF);
  static const Color surfaceContainerHighest = Color(0xFFD3E4FE);
  static const Color onSurface = Color(0xFF0B1C30);
  static const Color onSurfaceVariant = Color(0xFF464555);

  static const Color outline = Color(0xFF777587);
  static const Color outlineVariant = Color(0xFFC7C4D8);

  // Elevation shadows are indigo-tinted (design system spec).
  static const Color shadowTint = Color(0xFF3525CD);
}