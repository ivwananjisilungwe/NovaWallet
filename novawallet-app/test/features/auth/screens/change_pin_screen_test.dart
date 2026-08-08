import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/core/network/api_exception.dart';
import 'package:novawallet_app/features/auth/data/auth_repository.dart';
import 'package:novawallet_app/features/auth/providers/auth_provider.dart';
import 'package:novawallet_app/features/auth/screens/change_pin_screen.dart';

class _MockAuthRepository extends Mock implements AuthRepository {}

Future<void> _enterPin(WidgetTester tester, String pin) async {
  for (final digit in pin.split('')) {
    await tester.tap(find.text(digit));
    await tester.pump();
  }
  await tester.pump();
}

void main() {
  late _MockAuthRepository repository;
  late AuthNotifier notifier;

  setUp(() {
    repository = _MockAuthRepository();
    notifier = AuthNotifier(repository);
  });

  Future<void> pumpScreen(WidgetTester tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [authProvider.overrideWith((ref) => notifier)],
        child: const MaterialApp(home: ChangePinScreen()),
      ),
    );
  }

  testWidgets(
    'confirmation mismatch clears dots and permits immediate re-entry',
    (tester) async {
      await pumpScreen(tester);

      await _enterPin(tester, '1234');
      expect(find.text('Enter new PIN'), findsOneWidget);
      await _enterPin(tester, '5678');
      expect(find.text('Confirm new PIN'), findsOneWidget);

      await _enterPin(tester, '0000');

      expect(
        find.text('PINs do not match. Enter the new PIN again.'),
        findsOneWidget,
      );
      expect(find.text('Enter new PIN'), findsOneWidget);

      await _enterPin(tester, '2468');
      expect(find.text('Confirm new PIN'), findsOneWidget);
    },
  );

  testWidgets('API error resets to current PIN with persistent recovery text', (
    tester,
  ) async {
    when(
      () => repository.changePin(currentPin: '1234', newPin: '5678'),
    ).thenThrow(
      const ApiException(message: 'Current PIN is incorrect.', statusCode: 400),
    );
    await pumpScreen(tester);

    await _enterPin(tester, '1234');
    await _enterPin(tester, '5678');
    await _enterPin(tester, '5678');
    await tester.pump();

    expect(find.text('Current PIN is incorrect.'), findsOneWidget);
    expect(find.text('Enter current PIN'), findsOneWidget);

    await _enterPin(tester, '2468');
    expect(find.text('Enter new PIN'), findsOneWidget);
  });
}
