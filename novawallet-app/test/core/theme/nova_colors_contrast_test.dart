import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:novawallet_app/core/theme/nova_colors.dart';
import 'package:novawallet_app/core/widgets/widgets.dart';
import 'package:novawallet_app/features/wallet/screens/cards_screen.dart';

void main() {
  group('semantic foreground contrast', () {
    test('success text meets WCAG AA on light surfaces', () {
      expect(
        _contrastRatio(NovaColors.success, NovaColors.surfaceContainerLowest),
        greaterThanOrEqualTo(4.5),
      );
    });

    test('warning foreground meets WCAG AA on warning backgrounds', () {
      expect(
        _contrastRatio(NovaColors.onWarning, NovaColors.warning),
        greaterThanOrEqualTo(4.5),
      );
    });

    test('amount input hint meets the large-text contrast threshold', () {
      expect(
        _contrastRatio(
          NovaColors.amountInputHint,
          NovaColors.surfaceContainerLowest,
        ),
        greaterThanOrEqualTo(3.0),
      );
    });
  });

  group('warning foreground token use', () {
    testWidgets('balance card uses the warning foreground for FROZEN', (
      tester,
    ) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: BalanceCard(
              balanceLabel: 'Available balance',
              balanceAmount: 'ZMW 100.00',
              frozen: true,
            ),
          ),
        ),
      );

      final frozenText = tester.widget<Text>(find.text('FROZEN'));
      expect(frozenText.style?.color, NovaColors.onWarning);
    });

    testWidgets('card freeze badge uses the warning foreground for FROZEN', (
      tester,
    ) async {
      await tester.pumpWidget(
        const ProviderScope(child: MaterialApp(home: CardsScreen())),
      );

      await tester.tap(find.text('Freeze'));
      await tester.pump();

      final frozenText = tester.widget<Text>(find.text('FROZEN'));
      expect(frozenText.style?.color, NovaColors.onWarning);
    });
  });
}

double _contrastRatio(Color foreground, Color background) {
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
