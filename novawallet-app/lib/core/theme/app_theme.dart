import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import 'design_tokens.dart';
import 'nova_colors.dart';

/// Material 3 themes built from the NovaWallet design system.
///
/// Follows DESIGN.md: Inter typography (via google_fonts), pill buttons,
/// high-radius cards, indigo-tinted elevation, 4px baseline grid.
///
/// The light theme is the canonical palette; the dark theme is derived from
/// the reserved dark primitives in `design_tokens.dart` ([SemanticDarkColors])
/// and follows the system setting via `MaterialApp.themeMode`. Both themes
/// share one builder so component styling stays in lockstep — every color is
/// read from the [ColorScheme], never from a static light token.
class AppTheme {
  AppTheme._();

  static ThemeData get light {
    final scheme = ColorScheme.light(
      primary: NovaColors.primary,
      onPrimary: NovaColors.onPrimary,
      primaryContainer: NovaColors.primaryContainer,
      onPrimaryContainer: NovaColors.onPrimaryContainer,
      secondary: NovaColors.secondary,
      onSecondary: NovaColors.onSecondary,
      secondaryContainer: NovaColors.secondaryContainer,
      onSecondaryContainer: NovaColors.onSecondaryContainer,
      tertiary: NovaColors.tertiary,
      tertiaryContainer: NovaColors.tertiaryContainer,
      error: NovaColors.error,
      onError: NovaColors.onError,
      errorContainer: NovaColors.errorContainer,
      onErrorContainer: NovaColors.onErrorContainer,
      surface: NovaColors.surface,
      onSurface: NovaColors.onSurface,
      onSurfaceVariant: NovaColors.onSurfaceVariant,
      surfaceContainerLowest: NovaColors.surfaceContainerLowest,
      surfaceContainerLow: NovaColors.surfaceContainerLow,
      surfaceContainer: NovaColors.surfaceContainer,
      surfaceContainerHigh: NovaColors.surfaceContainerHigh,
      surfaceContainerHighest: NovaColors.surfaceContainerHighest,
      outline: NovaColors.outline,
      outlineVariant: NovaColors.outlineVariant,
    );
    return _buildTheme(scheme);
  }

  /// Dark theme mapped from [SemanticDarkColors].
  ///
  /// Text-bearing pairs are verified ≥ 4.5:1 (WCAG AA) in
  /// `test/core/theme/app_theme_dark_test.dart`.
  static ThemeData get dark {
    final scheme = ColorScheme.dark(
      // Brand actions — indigo lightens so links/icons keep contrast.
      primary: SemanticDarkColors.brandPrimary, // #C3C0FF
      onPrimary: SemanticDarkColors.onBrandPrimary, // #0F0069
      primaryContainer: SemanticDarkColors.brandPrimaryContainer, // #3323CC
      onPrimaryContainer: SemanticDarkColors.brandPrimary, // #C3C0FF
      // Money-in / success — emerald brightens.
      secondary: SemanticDarkColors.moneyIn, // #6CF8BB
      onSecondary: SemanticDarkColors.onMoneyIn, // #0B1C30
      secondaryContainer: SemanticDarkColors.moneyInContainer, // #006C49
      onSecondaryContainer: SemanticDarkColors.onMoneyInContainer, // #6CF8BB
      // Tertiary / danger — rose inverts to a light tone.
      tertiary: SemanticDarkColors.danger, // #FFB2B7
      tertiaryContainer: SemanticDarkColors.dangerContainer, // #95002B
      // Error roles mirror the money-out rose pair.
      error: SemanticDarkColors.moneyOut, // #FFDAD6
      onError: SemanticDarkColors.onMoneyOut, // #93000A
      errorContainer: SemanticDarkColors.moneyOutContainer, // #BA1A1A
      onErrorContainer: SemanticDarkColors.onMoneyOutContainer, // #FFDAD6
      // Surfaces — scaffold shares the deepest tone; cards sit one step up.
      surface: SemanticDarkColors.surface, // #12141C
      onSurface: SemanticDarkColors.onSurface, // #EAF1FF
      onSurfaceVariant: SemanticDarkColors.onSurfaceVariant, // #B3B0C5
      surfaceContainerLowest: SemanticDarkColors.surfaceContainerLowest,
      surfaceContainerLow: SemanticDarkColors.surfaceContainerLow,
      surfaceContainer: SemanticDarkColors.surfaceContainer,
      surfaceContainerHigh: SemanticDarkColors.surfaceContainerHigh,
      surfaceContainerHighest: SemanticDarkColors.surfaceContainerHighest,
      outline: SemanticDarkColors.outline, // #5F5D70
      outlineVariant: SemanticDarkColors.outlineVariant, // #3E3C4E
    );
    return _buildTheme(scheme);
  }

  /// Single builder shared by both themes — all colors come from [scheme] so
  /// components adapt automatically and the two themes cannot drift.
  static ThemeData _buildTheme(ColorScheme scheme) {
    // Start with a ThemeData that has NO hardcoded fontFamily —
    // GoogleFonts applies Inter per-TextStyle so tests stay deterministic
    // when runtime font fetching is disabled.
    final base = ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: scheme.surface,
    );

    final radius = BorderRadius.circular(16); // rounded-xl default for cards
    const pill = RoundedRectangleBorder(
      borderRadius: BorderRadius.all(Radius.circular(999)),
    );

    return base.copyWith(
      textTheme: _textTheme(base.textTheme, scheme),
      appBarTheme: base.appBarTheme.copyWith(
        backgroundColor: scheme.surface,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0.5,
        centerTitle: false,
        foregroundColor: scheme.onSurface,
        titleTextStyle: _textTheme(base.textTheme, scheme).titleLarge?.copyWith(
          fontWeight: FontWeight.w700,
          color: scheme.onSurface,
          letterSpacing: -0.01,
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          shape: pill,
          backgroundColor: scheme.primary,
          foregroundColor: scheme.onPrimary,
          minimumSize: const Size.fromHeight(56),
          textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
          elevation: 0,
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          shape: pill,
          side: BorderSide(color: scheme.outlineVariant, width: 2),
          foregroundColor: scheme.onSurface,
          minimumSize: const Size.fromHeight(56),
          textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: scheme.primary,
          textStyle: const TextStyle(fontWeight: FontWeight.w600),
          shape: pill,
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: scheme.surfaceContainerLowest,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 20,
          vertical: 18,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: scheme.outlineVariant),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: scheme.outlineVariant),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: scheme.primary, width: 2),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: scheme.error),
        ),
        hintStyle: TextStyle(color: scheme.onSurfaceVariant),
      ),
      cardTheme: base.cardTheme.copyWith(
        color: scheme.surfaceContainerLowest,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: radius,
          side: BorderSide(color: scheme.surfaceContainerLow),
        ),
      ),
      chipTheme: base.chipTheme.copyWith(
        shape: pill,
        side: BorderSide.none,
        backgroundColor: scheme.surfaceContainer,
        labelStyle: TextStyle(
          color: scheme.onSurfaceVariant,
          fontSize: 12,
          fontWeight: FontWeight.w600,
        ),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      ),
      bottomNavigationBarTheme: BottomNavigationBarThemeData(
        backgroundColor: scheme.surfaceContainerLowest,
        selectedItemColor: scheme.primary,
        unselectedItemColor: scheme.onSurfaceVariant,
        type: BottomNavigationBarType.fixed,
        elevation: 8,
        showUnselectedLabels: true,
      ),
      dividerTheme: DividerThemeData(
        color: scheme.outlineVariant,
        thickness: 1,
        space: 1,
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        // Intentionally theme-invariant: floating snackbars stay dark-navy
        // pills in both modes (matches the light design verbatim).
        backgroundColor: NovaColors.onSurface,
      ),
      dialogTheme: DialogThemeData(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        backgroundColor: scheme.surfaceContainerLowest,
      ),
      bottomSheetTheme: BottomSheetThemeData(
        backgroundColor: scheme.surfaceContainerLowest,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
      ),
      progressIndicatorTheme: ProgressIndicatorThemeData(
        color: scheme.primary,
        linearTrackColor: scheme.surfaceContainerHighest,
      ),
    );
  }

  /// Build the Inter text theme via GoogleFonts, then override sizes/weights
  /// to match DESIGN.md typography scale. Text colors come from [scheme] so
  /// both themes render correct ink without per-screen conditionals.
  static TextTheme _textTheme(TextTheme base, ColorScheme scheme) {
    // GoogleFonts provides the fontFamily on every TextStyle;
    // in tests with allowRuntimeFetching = false it falls back to
    // the system font automatically — no hardcoded 'Inter' string.
    final inter = GoogleFonts.interTextTheme(base);

    return inter.copyWith(
      headlineLarge: inter.headlineLarge?.copyWith(
        fontSize: 32,
        fontWeight: FontWeight.w700,
        height: 1.25,
        letterSpacing: -0.02,
        color: scheme.onSurface,
      ),
      headlineMedium: inter.headlineMedium?.copyWith(
        fontSize: 24,
        fontWeight: FontWeight.w700,
        height: 1.33,
        letterSpacing: -0.01,
        color: scheme.onSurface,
      ),
      headlineSmall: inter.headlineSmall?.copyWith(
        fontSize: 20,
        fontWeight: FontWeight.w600,
        height: 1.4,
        color: scheme.onSurface,
      ),
      titleLarge: inter.titleLarge?.copyWith(
        fontSize: 20,
        fontWeight: FontWeight.w600,
        height: 1.4,
        color: scheme.onSurface,
      ),
      titleMedium: inter.titleMedium?.copyWith(
        fontSize: 16,
        fontWeight: FontWeight.w600,
        height: 1.4,
        color: scheme.onSurface,
      ),
      bodyLarge: inter.bodyLarge?.copyWith(
        fontSize: 16,
        fontWeight: FontWeight.w400,
        height: 1.5,
        color: scheme.onSurface,
      ),
      bodyMedium: inter.bodyMedium?.copyWith(
        fontSize: 14,
        fontWeight: FontWeight.w400,
        height: 1.43,
        color: scheme.onSurface,
      ),
      bodySmall: inter.bodySmall?.copyWith(
        fontSize: 12,
        fontWeight: FontWeight.w400,
        height: 1.33,
        color: scheme.onSurfaceVariant,
      ),
      labelLarge: inter.labelLarge?.copyWith(
        fontSize: 16,
        fontWeight: FontWeight.w600,
        color: scheme.primary,
      ),
      labelMedium: inter.labelMedium?.copyWith(
        fontSize: 12,
        fontWeight: FontWeight.w600,
        letterSpacing: 0.05,
        color: scheme.onSurfaceVariant,
      ),
    );
  }

  /// Numeric display style for currency amounts (28px / 700 / -0.02em).
  ///
  /// Used on the indigo hero balance card, which keeps its brand gradient in
  /// both themes — hence the fixed white ink.
  static TextStyle get numericDisplay => GoogleFonts.inter(
    fontSize: 28,
    fontWeight: FontWeight.w700,
    height: 1.21,
    letterSpacing: -0.02,
    color: NovaColors.onPrimary,
  );
}
