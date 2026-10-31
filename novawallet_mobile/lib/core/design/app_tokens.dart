import 'package:flutter/widgets.dart';

/// Shared design tokens for NovaWallet.
///
/// Keep repeated measurements here so layouts remain consistent
/// and a future design adjustment can be applied globally.
abstract final class AppTokens {
  AppTokens._();

  // Spacing scale: use these instead of arbitrary spacing values.
  static const double space2 = 2;
  static const double space4 = 4;
  static const double space8 = 8;
  static const double space12 = 12;
  static const double space16 = 16;
  static const double space20 = 20;
  static const double space24 = 24;
  static const double space32 = 32;
  static const double space40 = 40;
  static const double space48 = 48;

  // Shared corner radii.
  static const double radiusSmall = 10;
  static const double radiusMedium = 14;
  static const double radiusLarge = 20;
  static const double radiusExtraLarge = 28;

  // Standard control dimensions.
  static const double buttonHeight = 52;
  static const double iconButtonSize = 44;

  /// Square icon frame used by catalogue and service tiles.
  static const double iconFrameSize = 44;

  /// Icon size drawn inside [iconFrameSize].
  static const double iconFrameIconSize = 22;
  static const double bottomNavigationHeight = 76;

  /// Minimum touch height for a service-catalogue tile.
  static const double tileMinHeight = 88;

  /// Gutter between catalogue tiles.
  static const double gridGutter = space12;

  /// Content width at which single-column catalogue layouts become two columns.
  static const double tileBreakpoint = 560;

  // Prevent excessively wide content on tablets and desktop windows.
  static const double contentMaxWidth = 720;

  // Maximum width of narrow form pages such as login.
  static const double formMaxWidth = 480;

  // Standard page padding.
  static const EdgeInsets pagePadding = EdgeInsets.fromLTRB(
    space20,
    space24,
    space20,
    space32,
  );

  static const EdgeInsets cardPadding = EdgeInsets.all(space20);

  // Reusable animation durations (ms) for the transaction detail experience.
  static const int animationFastMs = 150;
  static const int animationEntranceMs = 360;
  static const int animationStaggerMs = 50;
}
