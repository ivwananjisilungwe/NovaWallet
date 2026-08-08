import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:novawallet_app/core/widgets/pin_pad.dart';

Future<void> _enterPin(WidgetTester tester, String pin) async {
  for (final digit in pin.split('')) {
    await tester.tap(find.text(digit));
    await tester.pump();
  }
  await tester.pump();
}

void main() {
  testWidgets('reset token clears a filled PIN and accepts a new PIN', (
    tester,
  ) async {
    final completedPins = <String>[];
    var resetToken = 0;
    late StateSetter setHostState;

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: StatefulBuilder(
            builder: (context, setState) {
              setHostState = setState;
              return PinPad(
                length: 4,
                resetToken: resetToken,
                onComplete: completedPins.add,
              );
            },
          ),
        ),
      ),
    );

    await _enterPin(tester, '1234');
    expect(completedPins, ['1234']);

    setHostState(() => resetToken++);
    await tester.pump();
    await _enterPin(tester, '5678');

    expect(completedPins, ['1234', '5678']);
  });

  testWidgets('backspace has a meaningful accessibility label', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(body: PinPad(length: 4, onComplete: (_) {})),
      ),
    );

    expect(find.bySemanticsLabel('Backspace'), findsOneWidget);
  });
}
