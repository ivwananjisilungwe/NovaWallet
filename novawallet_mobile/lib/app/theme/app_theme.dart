import 'package:flutter/material.dart';

/// NovaWallet 2.0 — application design system.
///
/// Brand: Midnight & Electric Blue
/// Theme behavior: adaptive light and dark modes
///
/// Responsibilities:
/// - Define brand and semantic colors.
/// - Configure typography and component shapes.
/// - Provide consistent styling for every feature.
///
/// Feature screens should consume Theme.of(context) instead of
/// hardcoding brand colors or duplicating component styles.
abstract final class AppTheme {
  AppTheme._();

  // ============================================================
  // BRAND TOKENS
  // ============================================================

  /// Primary brand color for actions and selected states.
  static const Color electricBlue = Color(0xFF356DF3);

  /// Deep navy used for the dark theme's page background.
  static const Color midnight = Color(0xFF0B1020);

  /// Pale blue for subtle highlights and dark-theme accents.
  static const Color iceBlue = Color(0xFFC5D5FF);

  /// Main light-theme background.
  static const Color cloudWhite = Color(0xFFF4F6FB);

  // ============================================================
  // LIGHT PALETTE
  // ============================================================

  static const Color _lightBackground = Color(0xFFF4F6FB);
  static const Color _lightSurface = Color(0xFFFFFFFF);
  static const Color _lightText = Color(0xFF111827);
  static const Color _lightSecondaryText = Color(0xFF667085);
  static const Color _lightBorder = Color(0xFFE3E8F2);

  // ============================================================
  // DARK PALETTE
  // ============================================================

  static const Color _darkBackground = Color(0xFF0B1020);
  static const Color _darkSurface = Color(0xFF11182B);
  static const Color _darkElevatedSurface = Color(0xFF19233A);
  static const Color _darkText = Color(0xFFF4F6FB);
  static const Color _darkSecondaryText = Color(0xFFB8C4DE);
  static const Color _darkBorder = Color(0xFF2A3651);

  // ============================================================
  // DESIGN TOKENS
  // ============================================================

  static const double _cardRadius = 20;
  static const double _controlRadius = 14;
  static const double _buttonHeight = 52;

  // ============================================================
  // PUBLIC THEMES
  // ============================================================

  /// Used when the device is set to light appearance.
  static ThemeData get lightTheme => _buildLightTheme();

  /// Used when the device is set to dark appearance.
  static ThemeData get darkTheme => _buildDarkTheme();

  // ============================================================
  // LIGHT THEME
  // ============================================================

  static ThemeData _buildLightTheme() {
    const scheme = ColorScheme(
      brightness: Brightness.light,

      primary: electricBlue,
      onPrimary: Colors.white,
      primaryContainer: Color(0xFFDCE6FF),
      onPrimaryContainer: Color(0xFF102B69),

      secondary: Color(0xFF52658B),
      onSecondary: Colors.white,
      secondaryContainer: Color(0xFFE5EBF8),
      onSecondaryContainer: Color(0xFF1D2B49),

      tertiary: Color(0xFF425B88),
      onTertiary: Colors.white,
      tertiaryContainer: Color(0xFFE0E8FA),
      onTertiaryContainer: Color(0xFF182D52),

      error: Color(0xFFBA1A1A),
      onError: Colors.white,
      errorContainer: Color(0xFFFFDAD6),
      onErrorContainer: Color(0xFF410002),

      surface: _lightSurface,
      onSurface: _lightText,

      surfaceContainerLowest: Colors.white,
      surfaceContainerLow: Color(0xFFFAFBFE),
      surfaceContainer: Color(0xFFF1F4FA),
      surfaceContainerHigh: Color(0xFFEAEFF8),
      surfaceContainerHighest: Color(0xFFE0E7F3),

      onSurfaceVariant: _lightSecondaryText,

      outline: Color(0xFF7D8AA3),
      outlineVariant: _lightBorder,

      shadow: Colors.black,
      scrim: Colors.black,

      inverseSurface: midnight,
      onInverseSurface: Colors.white,
      inversePrimary: iceBlue,
    );

    return _buildTheme(
      scheme: scheme,
      background: _lightBackground,
      appBarBackground: _lightBackground,
      inputBackground: Colors.white,
    );
  }

  // ============================================================
  // DARK THEME
  // ============================================================

  static ThemeData _buildDarkTheme() {
    const scheme = ColorScheme(
      brightness: Brightness.dark,

      primary: Color(0xFF7EA2FF),
      onPrimary: Color(0xFF07183F),
      primaryContainer: Color(0xFF244BAA),
      onPrimaryContainer: Color(0xFFDCE6FF),

      secondary: iceBlue,
      onSecondary: Color(0xFF15284D),
      secondaryContainer: Color(0xFF283957),
      onSecondaryContainer: Color(0xFFDCE6FF),

      tertiary: Color(0xFFB7C8F4),
      onTertiary: Color(0xFF1C3157),
      tertiaryContainer: Color(0xFF30466E),
      onTertiaryContainer: Color(0xFFDCE6FF),

      error: Color(0xFFFFB4AB),
      onError: Color(0xFF690005),
      errorContainer: Color(0xFF93000A),
      onErrorContainer: Color(0xFFFFDAD6),

      surface: _darkSurface,
      onSurface: _darkText,

      surfaceContainerLowest: Color(0xFF080C17),
      surfaceContainerLow: Color(0xFF0E1424),
      surfaceContainer: _darkSurface,
      surfaceContainerHigh: _darkElevatedSurface,
      surfaceContainerHighest: Color(0xFF222E49),

      onSurfaceVariant: _darkSecondaryText,

      outline: Color(0xFF8C9BB8),
      outlineVariant: _darkBorder,

      shadow: Colors.black,
      scrim: Colors.black,

      inverseSurface: _lightSurface,
      onInverseSurface: _lightText,
      inversePrimary: electricBlue,
    );

    return _buildTheme(
      scheme: scheme,
      background: _darkBackground,
      appBarBackground: _darkBackground,
      inputBackground: _darkElevatedSurface,
    );
  }

  // ============================================================
  // SHARED COMPONENT THEMING
  // ============================================================

  /// Builds shared widget styles while respecting the selected palette.
  static ThemeData _buildTheme({
    required ColorScheme scheme,
    required Color background,
    required Color appBarBackground,
    required Color inputBackground,
  }) {
    final base = ThemeData(
      useMaterial3: true,
      brightness: scheme.brightness,
      colorScheme: scheme,
    );

    return base.copyWith(
      scaffoldBackgroundColor: background,

      // Typography: strong hierarchy for balances and page headings.
      textTheme: base.textTheme.copyWith(
        headlineLarge: TextStyle(
          fontSize: 32,
          fontWeight: FontWeight.w700,
          letterSpacing: -1.0,
          color: scheme.onSurface,
        ),
        headlineMedium: TextStyle(
          fontSize: 26,
          fontWeight: FontWeight.w700,
          letterSpacing: -0.6,
          color: scheme.onSurface,
        ),
        titleLarge: TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.w700,
          color: scheme.onSurface,
        ),
        titleMedium: TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w600,
          color: scheme.onSurface,
        ),
        bodyLarge: TextStyle(
          fontSize: 16,
          height: 1.5,
          color: scheme.onSurface,
        ),
        bodyMedium: TextStyle(
          fontSize: 14,
          height: 1.45,
          color: scheme.onSurfaceVariant,
        ),
      ),

      // Flat app bars create a clean, uninterrupted page layout.
      appBarTheme: AppBarTheme(
        backgroundColor: appBarBackground,
        foregroundColor: scheme.onSurface,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        titleTextStyle: TextStyle(
          color: scheme.onSurface,
          fontSize: 20,
          fontWeight: FontWeight.w700,
        ),
      ),

      // Rounded cards with subtle borders instead of excessive shadows.
      cardTheme: CardThemeData(
        color: scheme.surface,
        elevation: 0,
        margin: EdgeInsets.zero,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(_cardRadius),
          side: BorderSide(color: scheme.outlineVariant),
        ),
      ),

      // Inputs have a neutral fill and a clear electric-blue focus state.
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: inputBackground,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 16,
          vertical: 16,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(_controlRadius),
          borderSide: BorderSide(color: scheme.outlineVariant),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(_controlRadius),
          borderSide: BorderSide(color: scheme.outlineVariant),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(_controlRadius),
          borderSide: BorderSide(color: scheme.primary, width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(_controlRadius),
          borderSide: BorderSide(color: scheme.error),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(_controlRadius),
          borderSide: BorderSide(color: scheme.error, width: 1.5),
        ),
      ),

      // Filled buttons represent the primary action on a screen.
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: scheme.primary,
          foregroundColor: scheme.onPrimary,
          minimumSize: const Size(0, _buttonHeight),
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(_controlRadius),
          ),
          textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
        ),
      ),

      // Outlined buttons are secondary actions, not competing primaries.
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: scheme.onSurface,
          minimumSize: const Size(0, _buttonHeight),
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(_controlRadius),
          ),
          side: BorderSide(color: scheme.outlineVariant),
          textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
        ),
      ),

      // Brief feedback should be readable on either theme.
      snackBarTheme: SnackBarThemeData(
        backgroundColor: scheme.inverseSurface,
        contentTextStyle: TextStyle(
          color: scheme.onInverseSurface,
          fontSize: 14,
        ),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(_controlRadius),
        ),
      ),

      dividerTheme: DividerThemeData(
        color: scheme.outlineVariant,
        thickness: 1,
        space: 1,
      ),

      // Icons inherit the current theme's secondary text color.
      iconTheme: IconThemeData(color: scheme.onSurfaceVariant),

      // Text selection and cursor styling follow the active brand palette.
      textSelectionTheme: TextSelectionThemeData(
        cursorColor: scheme.primary,
        selectionColor: scheme.primary.withValues(alpha: 0.18),
        selectionHandleColor: scheme.primary,
      ),
    );
  }
}
