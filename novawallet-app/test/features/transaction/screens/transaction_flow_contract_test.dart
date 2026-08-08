import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:novawallet_app/features/transaction/screens/success_screen.dart';

void main() {
  testWidgets(
    'success receipt formats amount with two ZMW decimals and reference',
    (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: SuccessScreen(
            title: 'Transfer sent',
            amount: '10',
            reference: 'NW-20260808-ABC12',
          ),
        ),
      );

      expect(find.text('ZMW 10.00'), findsOneWidget);
      expect(find.text('Ref: NW-20260808-ABC12'), findsOneWidget);
    },
  );

  testWidgets('success receipt does not display a fabricated reference', (
    tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: SuccessScreen(title: 'Deposit successful', amount: '25.5'),
      ),
    );

    expect(find.text('ZMW 25.50'), findsOneWidget);
    expect(find.textContaining('Ref:'), findsNothing);
  });
}
