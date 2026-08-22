// Dark-theme contract: palette anchors + WCAG AA text contrast.
//
// Mirrors the helper in nova_colors_contrast_test.dart so both suites
// compute ratios identically.
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:novawallet_app/core/theme/app_theme.dart';
import 'package:novawallet_app/core/theme/design_tokens.dart';

void main() {
  final theme = AppTheme.dark;
  final scheme = theme.colorScheme;

  double contrastRatio(Color foreground, Color background) {
    final foregroundLuminance = foreground.computeLuminance();
    final backgroundLuminance = background.computeLuminance();
    final lighter = foregroundLuminance > backgroundLuminance
        ? foregroundLuminance
        : backgroundLuminance;
    final darker = foregroundLuminance > backgroundLuminance
        ? backgroundLuminance
        : foregroundLuminance;

    return (lighter + 0.05) / (darker + 0.05);
  }

  test('dark theme is dark and built from the reserved dark primitives', () {
    expect(theme.brightness, Brightness.dark);
    expect(scheme.primary.toARGB32(), SemanticDarkColors.brandPrimary.toARGB32());
    expect(scheme.surface.toARGB32(), 0xFF12141C); // darkBackground
    expect(scheme.onSurface.toARGB32(), 0xFFEAF1FF); // darkOnSurface
  });

  test('body text meets WCAG AA on every dark surface tier', () {
    final surfaces = [
      scheme.surface,
      scheme.surfaceContainerLowest,
      scheme.surfaceContainerLow,
      scheme.surfaceContainer,
      scheme.surfaceContainerHigh,
      scheme.surfaceContainerHighest,
    ];
    for (final surface in surfaces) {
      final name = surface.toARGB32().toRadixString(16);
      expect(
        contrastRatio(scheme.onSurface, surface),
        greaterThanOrEqualTo(4.5),
        reason: 'onSurface on #$name',
      );
      expect(
        contrastRatio(scheme.onSurfaceVariant, surface),
        greaterThanOrEqualTo(4.5),
        reason: 'onSurfaceVariant on #$name',
      );
    }
  });

  test('accent foregrounds meet WCAG AA on dark card surfaces', () {
    final card = scheme.surfaceContainerLowest;
    expect(
      contrastRatio(scheme.primary, card),
      greaterThanOrEqualTo(4.5),
      reason: 'primary links/icons on cards',
    );
    expect(
      contrastRatio(SemanticDarkColors.moneyIn, card),
      greaterThanOrEqualTo(4.5),
      reason: 'money-in accent on cards',
    );
    expect(
      contrastRatio(SemanticDarkColors.warning, card),
      greaterThanOrEqualTo(4.5),
      reason: 'warning accent on cards',
    );
    expect(
      contrastRatio(scheme.error, card),
      greaterThanOrEqualTo(4.5),
      reason: 'error tone on cards',
    );
  });

  test('paired containers meet WCAG AA for their foregrounds', () {
    expect(
      contrastRatio(scheme.onPrimary, scheme.primary),
      greaterThanOrEqualTo(4.5),
    );
    expect(
      contrastRatio(scheme.onSecondaryContainer, scheme.secondaryContainer),
      greaterThanOrEqualTo(4.5),
    );
    expect(
      contrastRatio(scheme.onError, scheme.error),
      greaterThanOrEqualTo(4.5),
    );
  });

  test('light theme is untouched by the dark palette', () {
    final light = AppTheme.light;
    expect(light.brightness, Brightness.light);
    expect(light.colorScheme.primary.toARGB32(), 0xFF3525CD);
    expect(light.colorScheme.surface.toARGB32(), 0xFFF8F9FF);
  });
}
