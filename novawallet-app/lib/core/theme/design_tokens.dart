/// Design tokens for NovaWallet — three-layer architecture.
///
/// Primitive (raw values) → Semantic (purpose aliases) → Component (per-widget).
///
/// Sourced from [DESIGN.md] and Stitch design system.
/// All values are `const` so they propagate through widget trees without rebuilds.
library;

import 'package:flutter/material.dart';

// =============================================================================
// PRIMITIVE TOKENS — raw design values, no business meaning
// =============================================================================

class _PrimitiveColors {
  _PrimitiveColors._();

  // Indigo scale
  static const indigo900 = Color(0xFF3525CD);
  static const indigo600 = Color(0xFF4F46E5);
  // Dark-mode inversions (activated for the dark theme)
  static const indigo800 = Color(0xFF3323CC);
  static const indigo700 = Color(0xFF0F0069);
  static const indigo200 = Color(0xFFC3C0FF);
  // Reserved for future use (extended palette)
  // static const indigo500 = Color(0xFF4D44E3);
  // static const indigo100 = Color(0xFFDAD7FF);
  // static const indigo50 = Color(0xFFE2DFFF);

  // Emerald scale
  static const emerald700 = Color(0xFF006C49);
  static const emerald600 = Color(0xFF00714D);
  static const emerald300 = Color(0xFF6CF8BB);
  // Reserved for future use
  // static const emerald500 = Color(0xFF005236);
  // static const emerald200 = Color(0xFF6FFBBE);
  // static const emerald100 = Color(0xFF4EDEA3);

// Rose / error scale
  static const rose800 = Color(0xFFBA1A1A);
  static const rose100 = Color(0xFFFFDAD6);
  static const rose900 = Color(0xFF93000A);
  // Tertiary (deep rose)
  static const tertiary800 = Color(0xFF95002B);
  static const tertiary600 = Color(0xFFBF0F3C);
  // Dark-mode inversion (activated for the dark theme)
  static const tertiary200 = Color(0xFFFFB2B7);
  // Reserved for future use
  // static const tertiary700 = Color(0xFF92002A);
  // static const tertiary400 = Color(0xFFFFDADB);
  // static const tertiary900 = Color(0xFF40000D);

// Amber / warning
  static const amber500 = Color(0xFFF59E0B);
  static const amber900 = Color(0xFF451A03);
  // Slate / neutral
  static const slate50 = Color(0xFFF8F9FF);
  static const slate900 = Color(0xFF0B1C30);
  static const slate700 = Color(0xFF464555);
  static const slate600 = Color(0xFF777587);
  static const slate200 = Color(0xFFC7C4D8);
  static const slate100 = Color(0xFFEFF4FF);
  static const slate50Alt = Color(0xFFFFFFFF);
  static const slate150 = Color(0xFFE5EEFF);
  static const slate200Alt = Color(0xFFDCE9FF);
  static const slate250 = Color(0xFFD3E4FE);
  // Reserved for future use
  // static const slate300 = Color(0xFFCBDBF5);
  // Dark mode surfaces (activated for the dark theme — see SemanticDarkColors)
  static const darkSurface = Color(0xFF1A1D28);
  static const darkSurfaceContainer = Color(0xFF222533);
  static const darkSurfaceContainerLow = Color(0xFF1E2130);
  static const darkSurfaceContainerHigh = Color(0xFF2A2D3D);
  static const darkSurfaceContainerHighest = Color(0xFF323548);
  static const darkOnSurface = Color(0xFFEAF1FF);
  static const darkOnSurfaceVariant = Color(0xFFB3B0C5);
  static const darkOutline = Color(0xFF5F5D70);
  static const darkOutlineVariant = Color(0xFF3E3C4E);
  static const darkBackground = Color(0xFF12141C);
  static const darkOnBackground = Color(0xFFEAF1FF);
}

// =============================================================================
// SEMANTIC TOKENS — purpose aliases derived from primitives
// =============================================================================

/// Semantic color aliases that map a *purpose* to a primitive value.
///
/// Prefer these over raw [NovaColors] in component code so a single
/// rebranding change ripples through the entire app.
class SemanticColors {
  SemanticColors._();

  // Brand actions
  static const brandPrimary = _PrimitiveColors.indigo900;
  static const brandPrimaryContainer = _PrimitiveColors.indigo600;
  static const onBrandPrimary = _PrimitiveColors.slate50Alt;

  // Money-in / success
  static const moneyIn = _PrimitiveColors.emerald700;
  static const moneyInContainer = _PrimitiveColors.emerald300;
  static const onMoneyIn = _PrimitiveColors.slate50Alt;
  static const onMoneyInContainer = _PrimitiveColors.emerald600;

  // Money-out / error
  static const moneyOut = _PrimitiveColors.rose800;
  static const moneyOutContainer = _PrimitiveColors.rose100;
  static const onMoneyOut = _PrimitiveColors.slate50Alt;
  static const onMoneyOutContainer = _PrimitiveColors.rose900;

  // Warning / pending / KYC
  static const warning = _PrimitiveColors.amber500;
  static const onWarning = _PrimitiveColors.amber900;

  // Tertiary (dangerous actions, delete)
  static const danger = _PrimitiveColors.tertiary800;
  static const dangerContainer = _PrimitiveColors.tertiary600;

  // Surfaces
  static const background = _PrimitiveColors.slate50;
  static const onBackground = _PrimitiveColors.slate900;
  static const surface = _PrimitiveColors.slate50;
  static const surfaceContainerLowest = _PrimitiveColors.slate50Alt;
  static const surfaceContainerLow = _PrimitiveColors.slate100;
  static const surfaceContainer = _PrimitiveColors.slate150;
  static const surfaceContainerHigh = _PrimitiveColors.slate200Alt;
  static const surfaceContainerHighest = _PrimitiveColors.slate250;
  static const onSurface = _PrimitiveColors.slate900;
  static const onSurfaceVariant = _PrimitiveColors.slate700;

  // Outlines
  static const outline = _PrimitiveColors.slate600;
  static const outlineVariant = _PrimitiveColors.slate200;

  // Shadow tint (indigo)
  static const shadowTint = _PrimitiveColors.indigo900;
}

/// Dark-mode counterparts of [SemanticColors] — same purposes, dark-tuned
/// primitives. Material 3 tone inversion: accents lighten (so text/icons
/// keep ≥ 4.5:1 on dark surfaces) while containers darken.
///
/// Every foreground/background pair below is verified ≥ 4.5:1 WCAG AA in
/// `test/core/theme/app_theme_dark_test.dart`.
class SemanticDarkColors {
  SemanticDarkColors._();

  // Brand actions — indigo lightens for links/icons/focus rings on dark.
  static const brandPrimary = _PrimitiveColors.indigo200; // #C3C0FF
  static const brandPrimaryContainer = _PrimitiveColors.indigo800; // #3323CC
  static const onBrandPrimary = _PrimitiveColors.indigo700; // #0F0069

  // Money-in / success — emerald brightens.
  static const moneyIn = _PrimitiveColors.emerald300; // #6CF8BB
  static const moneyInContainer = _PrimitiveColors.emerald700; // #006C49
  static const onMoneyIn = _PrimitiveColors.slate900; // #0B1C30
  static const onMoneyInContainer = _PrimitiveColors.emerald300; // #6CF8BB

  // Money-out / error — rose inverts to a light error tone.
  static const moneyOut = _PrimitiveColors.rose100; // #FFDAD6
  static const moneyOutContainer = _PrimitiveColors.rose800; // #BA1A1A
  static const onMoneyOut = _PrimitiveColors.rose900; // #93000A
  static const onMoneyOutContainer = _PrimitiveColors.rose100; // #FFDAD6

  // Warning / pending / KYC — amber is bright enough for dark surfaces as-is.
  static const warning = _PrimitiveColors.amber500;
  static const onWarning = _PrimitiveColors.amber900;

  // Tertiary (dangerous actions, delete).
  static const danger = _PrimitiveColors.tertiary200; // #FFB2B7
  static const dangerContainer = _PrimitiveColors.tertiary800; // #95002B

  // Surfaces — scaffold and app bar share the deepest tone; cards sit one
  // step above it via surfaceContainerLowest (= [darkSurface]).
  static const background = _PrimitiveColors.darkBackground; // #12141C
  static const onBackground = _PrimitiveColors.darkOnBackground; // #EAF1FF
  static const surface = _PrimitiveColors.darkBackground; // #12141C
  static const surfaceContainerLowest = _PrimitiveColors.darkSurface; // #1A1D28
  static const surfaceContainerLow =
      _PrimitiveColors.darkSurfaceContainerLow; // #1E2130
  static const surfaceContainer =
      _PrimitiveColors.darkSurfaceContainer; // #222533
  static const surfaceContainerHigh =
      _PrimitiveColors.darkSurfaceContainerHigh; // #2A2D3D
  static const surfaceContainerHighest =
      _PrimitiveColors.darkSurfaceContainerHighest; // #323548
  static const onSurface = _PrimitiveColors.darkOnSurface; // #EAF1FF
  static const onSurfaceVariant = _PrimitiveColors.darkOnSurfaceVariant;

  // Outlines
  static const outline = _PrimitiveColors.darkOutline; // #5F5D70
  static const outlineVariant = _PrimitiveColors.darkOutlineVariant; // #3E3C4E
}

// =============================================================================
// SPACING TOKENS — 4px baseline grid
// =============================================================================

class Spacing {
  Spacing._();

  static const unit = 4.0;

  static const xs = 4.0;
  static const sm = 8.0;
  static const md = 16.0;
  static const lg = 24.0;
  static const xl = 32.0;
  static const xxl = 40.0;
  static const xxxl = 48.0;

  // Layout-specific
  static const containerMargin = 20.0;
  static const gutter = 16.0;
}

// =============================================================================
// SHAPE TOKENS — border radii
// =============================================================================

class ShapeTokens {
  ShapeTokens._();

  static const sm = 8.0; // 0.5rem
  static const md = 16.0; // 1rem
  static const lg = 24.0; // 1.5rem
  static const xl = 32.0; // 2rem
  static const xxl = 48.0; // 3rem
  static const full = 999.0; // pill

  static const borderRadiusSm = BorderRadius.all(Radius.circular(8));
  static const borderRadiusMd = BorderRadius.all(Radius.circular(16));
  static const borderRadiusLg = BorderRadius.all(Radius.circular(24));
  static const borderRadiusXl = BorderRadius.all(Radius.circular(32));
  static const borderRadiusFull = BorderRadius.all(Radius.circular(999));

  static const stadium = RoundedRectangleBorder(
    borderRadius: BorderRadius.all(Radius.circular(999)),
  );
}

// =============================================================================
// TYPOGRAPHY TOKENS — Inter font, DESIGN.md sizes
// =============================================================================

class TypographyTokens {
  TypographyTokens._();

  static const baseFontFamily = 'Inter';

  // headline-lg: 32px / 700 / 1.25lh / -0.02em ls
  static const headlineLarge = TextStyle(
    fontSize: 32,
    fontWeight: FontWeight.w700,
    height: 1.25,
    letterSpacing: -0.02,
  );

  // headline-lg-mobile: 24px / 700 / 1.33lh / -0.01em ls
  static const headlineLargeMobile = TextStyle(
    fontSize: 24,
    fontWeight: FontWeight.w700,
    height: 1.33,
    letterSpacing: -0.01,
  );

  // headline-md: 20px / 600 / 1.4lh
  static const headlineMedium = TextStyle(
    fontSize: 20,
    fontWeight: FontWeight.w600,
    height: 1.4,
  );

  // numeric-display: 28px / 700 / 1.21lh / -0.02em ls
  static const numericDisplay = TextStyle(
    fontSize: 28,
    fontWeight: FontWeight.w700,
    height: 1.21,
    letterSpacing: -0.02,
  );

  // body-lg: 16px / 400 / 1.5lh
  static const bodyLarge = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.w400,
    height: 1.5,
  );

  // body-sm: 14px / 400 / 1.43lh
  static const bodySmall = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.w400,
    height: 1.43,
  );

  // label-caps: 12px / 600 / 1.33lh / 0.05em ls
  static const labelCaps = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.w600,
    height: 1.33,
    letterSpacing: 0.05,
  );
}

// =============================================================================
// COMPONENT TOKENS — per-widget composition of semantic + shape + spacing
// =============================================================================

/// Component-level tokens that assemble semantic colors, shapes, and spacing
/// into reusable widget configurations.
class ComponentTokens {
  ComponentTokens._();

  // PillButton
  static const pillButtonMinHeight = 56.0;
  static const pillButtonPadding = EdgeInsets.symmetric(
    horizontal: 24,
    vertical: 16,
  );
  static const pillButtonShape = StadiumBorder();
  static const pillButtonFontSize = 16.0;
  static const pillButtonFontWeight = FontWeight.w600;

  // StatusChip
  static const chipPadding = EdgeInsets.symmetric(horizontal: 10, vertical: 4);
  static const chipFontSize = 12.0;
  static const chipFontWeight = FontWeight.w600;

  // BalanceCard
  static const balanceCardBorderRadius = 24.0;
  static const balanceCardPadding = EdgeInsets.all(20);
  static const balanceCardLabelFontSize = 14.0;
  static const balanceCardAmountFontSize = 28.0;

  // TransactionTile
  static const tileBorderRadius = 16.0;
  static const tilePadding = EdgeInsets.all(16);
  static const tileIconSize = 40.0;
  static const tileIconInnerSize = 20.0;
  static const tileTitleFontSize = 14.0;
  static const tileSubtitleFontSize = 12.0;
  static const tileAmountFontSize = 14.0;

  // BottomNavigationBar
  static const navBarElevation = 8.0;
  static const navBarIconSize = 24.0;
}

// =============================================================================
// MOTION TOKENS — durations and curves
// =============================================================================

class MotionTokens {
  MotionTokens._();

  // Micro-interactions: 150-300ms
  static const durationFast = Duration(milliseconds: 150);
  static const durationNormal = Duration(milliseconds: 250);
  static const durationSlow = Duration(milliseconds: 400);

  // Easing curves
  static const easeOut = Curves.easeOut;
  static const easeInOut = Curves.easeInOut;
  static const standard = Curves.ease;

  // Spring-like (for scale press feedback)
  static const springScale = 0.96;
}

// =============================================================================
// ELEVATION TOKENS — indigo-tinted shadows
// =============================================================================

class ElevationTokens {
  ElevationTokens._();

  static const Color shadowTint = Color(0xFF3525CD);

  static const shadow1 = [
    BoxShadow(
      color: Color(0x0A3525CD), // 4% opacity
      blurRadius: 12,
      offset: Offset(0, 4),
    ),
  ];

  static const shadow2 = [
    BoxShadow(
      color: Color(0x143525CD), // 8% opacity
      blurRadius: 24,
      offset: Offset(0, 8),
    ),
  ];

  /// Balance card hero shadow
  static const shadowCard = [
    BoxShadow(
      color: Color(0x403525CD), // 25% opacity
      blurRadius: 20,
      offset: Offset(0, 8),
    ),
  ];
}
