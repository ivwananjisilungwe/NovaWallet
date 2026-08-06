import 'package:flutter/material.dart';

import 'nova_colors.dart';

/// Material 3 theme built from the NovaWallet design system.
///
/// Follows DESIGN.md: light-only, Inter typography, pill buttons,
/// high-radius cards, indigo-tinted elevation, 4px baseline grid.
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

    final base = ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: NovaColors.background,
      fontFamily: 'Inter',
    );

    final radius = BorderRadius.circular(16); // rounded-xl default for cards
    const pill = RoundedRectangleBorder(
      borderRadius: BorderRadius.all(Radius.circular(999)),
    );

    return base.copyWith(
      textTheme: _textTheme(base.textTheme),
      appBarTheme: base.appBarTheme.copyWith(
        backgroundColor: NovaColors.surface,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0.5,
        centerTitle: false,
        foregroundColor: NovaColors.onSurface,
        titleTextStyle: base.textTheme.titleLarge?.copyWith(
          fontWeight: FontWeight.w700,
          color: NovaColors.onSurface,
          letterSpacing: -0.01,
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          shape: pill,
          backgroundColor: NovaColors.primary,
          foregroundColor: NovaColors.onPrimary,
          minimumSize: const Size.fromHeight(56),
          textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
          elevation: 0,
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          shape: pill,
          side: const BorderSide(color: NovaColors.outlineVariant, width: 2),
          foregroundColor: NovaColors.onSurface,
          minimumSize: const Size.fromHeight(56),
          textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: NovaColors.primary,
          textStyle: const TextStyle(fontWeight: FontWeight.w600),
          shape: pill,
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: NovaColors.surfaceContainerLowest,
        contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: NovaColors.outlineVariant),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: NovaColors.outlineVariant),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: NovaColors.primary, width: 2),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: NovaColors.error),
        ),
        hintStyle: const TextStyle(color: NovaColors.onSurfaceVariant),
      ),
      cardTheme: base.cardTheme.copyWith(
        color: NovaColors.surfaceContainerLowest,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: radius,
          side: const BorderSide(color: NovaColors.surfaceContainerLow),
        ),
      ),
      chipTheme: base.chipTheme.copyWith(
        shape: pill,
        side: BorderSide.none,
        backgroundColor: NovaColors.surfaceContainer,
        labelStyle: const TextStyle(
          color: NovaColors.onSurfaceVariant,
          fontSize: 12,
          fontWeight: FontWeight.w600,
        ),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      ),
      bottomNavigationBarTheme: const BottomNavigationBarThemeData(
        backgroundColor: NovaColors.surfaceContainerLowest,
        selectedItemColor: NovaColors.primary,
        unselectedItemColor: NovaColors.onSurfaceVariant,
        type: BottomNavigationBarType.fixed,
        elevation: 8,
        showUnselectedLabels: true,
      ),
      dividerTheme: const DividerThemeData(
        color: NovaColors.outlineVariant,
        thickness: 1,
        space: 1,
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        backgroundColor: NovaColors.onSurface,
      ),
      dialogTheme: DialogThemeData(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        backgroundColor: NovaColors.surfaceContainerLowest,
      ),
      bottomSheetTheme: const BottomSheetThemeData(
        backgroundColor: NovaColors.surfaceContainerLowest,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
      ),
      progressIndicatorTheme: const ProgressIndicatorThemeData(
        color: NovaColors.primary,
        linearTrackColor: NovaColors.surfaceContainerHighest,
      ),
    );
  }

  static TextTheme _textTheme(TextTheme base) {
    // DESIGN.md headline/body sizes mapped onto Material roles.
    return base.copyWith(
      headlineLarge: const TextStyle(
        fontSize: 32,
        fontWeight: FontWeight.w700,
        height: 1.25,
        letterSpacing: -0.02,
        color: NovaColors.onBackground,
      ),
      headlineMedium: const TextStyle(
        fontSize: 24,
        fontWeight: FontWeight.w700,
        height: 1.33,
        letterSpacing: -0.01,
        color: NovaColors.onBackground,
      ),
      headlineSmall: const TextStyle(
        fontSize: 20,
        fontWeight: FontWeight.w600,
        height: 1.4,
        color: NovaColors.onBackground,
      ),
      titleLarge: const TextStyle(
        fontSize: 20,
        fontWeight: FontWeight.w600,
        height: 1.4,
        color: NovaColors.onSurface,
      ),
      titleMedium: const TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.w600,
        height: 1.4,
        color: NovaColors.onSurface,
      ),
      bodyLarge: const TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.w400,
        height: 1.5,
        color: NovaColors.onBackground,
      ),
      bodyMedium: const TextStyle(
        fontSize: 14,
        fontWeight: FontWeight.w400,
        height: 1.43,
        color: NovaColors.onSurface,
      ),
      bodySmall: const TextStyle(
        fontSize: 12,
        fontWeight: FontWeight.w400,
        height: 1.33,
        color: NovaColors.onSurfaceVariant,
      ),
      labelLarge: const TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.w600,
        color: NovaColors.primary,
      ),
      labelMedium: const TextStyle(
        fontSize: 12,
        fontWeight: FontWeight.w600,
        letterSpacing: 0.05,
        color: NovaColors.onSurfaceVariant,
      ),
    );
  }

  /// Numeric display style for currency amounts (28px / 700 / -0.02em).
  static const TextStyle numericDisplay = TextStyle(
    fontSize: 28,
    fontWeight: FontWeight.w700,
    height: 1.21,
    letterSpacing: -0.02,
    color: NovaColors.onPrimary,
  );
}