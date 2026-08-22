import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/core/network/api_exception.dart';
import 'package:novawallet_app/core/widgets/pin_pad.dart';
import 'package:novawallet_app/features/transaction/data/transaction_repository.dart';
import 'package:novawallet_app/features/transaction/models/fee_estimate.dart';
import 'package:novawallet_app/features/transaction/models/transaction.dart';
import 'package:novawallet_app/features/transaction/providers/transaction_provider.dart';
import 'package:novawallet_app/features/transaction/screens/insufficient_balance_screen.dart';
import 'package:novawallet_app/features/transaction/screens/success_screen.dart';
import 'package:novawallet_app/features/wallet/data/wallet_repository.dart';
import 'package:novawallet_app/features/wallet/providers/wallet_provider.dart';
import 'package:novawallet_app/features/wallet/screens/send_screen.dart';

class _MockWalletRepository extends Mock implements WalletRepository {}

class _MockTransactionRepository extends Mock
    implements TransactionRepository {}

const _successTxn = WalletTransaction(
  id: 'txn-1',
  reference: 'NW-REF-1',
  type: 'TRANSFER_DEBIT',
  amount: '100.00',
  status: 'SUCCESSFUL',
);

Future<void> _enterPin(WidgetTester tester, String pin) async {
  tester.widget<PinPad>(find.byType(PinPad)).onComplete(pin);
  await tester.pump();
}

void main() {
  late _MockWalletRepository walletRepository;
  late _MockTransactionRepository transactionRepository;

  setUp(() {
    walletRepository = _MockWalletRepository();
    transactionRepository = _MockTransactionRepository();
    when(
      () => transactionRepository.estimateFee(type: 'TRANSFER', amount: '100'),
    ).thenAnswer(
      (_) async => const FeeEstimate(
        transactionType: 'TRANSFER',
        amount: '100',
        totalFee: '2.50',
      ),
    );
  });

  Future<void> pumpSend(WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(420, 900));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final router = GoRouter(
      initialLocation: '/send',
      routes: [
        GoRoute(path: '/send', builder: (_, _) => const SendScreen()),
        GoRoute(
          path: '/insufficient',
          builder: (_, _) => const InsufficientBalanceScreen(),
        ),
        GoRoute(
          path: '/success',
          builder: (_, state) => SuccessScreen(
            title: state.uri.queryParameters['title'] ?? 'Success',
            amount: state.uri.queryParameters['amount'],
            reference: state.uri.queryParameters['ref'],
          ),
        ),
      ],
    );
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          walletRepositoryProvider.overrideWithValue(walletRepository),
          transactionRepositoryProvider.overrideWithValue(
            transactionRepository,
          ),
        ],
        child: MaterialApp.router(routerConfig: router),
      ),
    );
  }

  Future<void> openConfirmation(WidgetTester tester) async {
    await tester.enterText(
      find.widgetWithText(TextField, 'Recipient wallet ID'),
      'wallet-456',
    );
    await tester.enterText(find.byType(TextField).at(1), '100');
    await tester.tap(find.text('Continue'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 50));
  }

  testWidgets('discloses recipient, fee, and total before PIN authorization', (
    tester,
  ) async {
    await pumpSend(tester);
    await openConfirmation(tester);

    expect(find.text('Confirm transfer'), findsOneWidget);
    expect(find.text('wallet-456').last, findsOneWidget);
    expect(find.text('ZMW 100.00'), findsWidgets);
    expect(find.text('ZMW 2.50'), findsWidgets);
    expect(find.text('ZMW 102.50'), findsWidgets);
    expect(find.textContaining('authorizes this transfer'), findsOneWidget);
    expect(find.text('Cancel'), findsOneWidget);
  });

  testWidgets('network retry keeps operation key and rebuilds the PIN pad', (
    tester,
  ) async {
    final keys = <String?>[];
    var attempts = 0;
    when(
      () => walletRepository.transfer(
        receiverWalletId: any(named: 'receiverWalletId'),
        amount: any(named: 'amount'),
        description: any(named: 'description'),
        pin: any(named: 'pin'),
        idempotencyKey: any(named: 'idempotencyKey'),
      ),
    ).thenAnswer((invocation) async {
      keys.add(invocation.namedArguments[#idempotencyKey] as String?);
      attempts++;
      if (attempts == 1) {
        throw const ApiException(message: 'Timed out');
      }
      return const WalletTransaction(
        id: 'txn-1',
        reference: 'NW-REF-1',
        type: 'TRANSFER_DEBIT',
        amount: '100.00',
        status: 'SUCCESSFUL',
      );
    });

    await pumpSend(tester);
    await openConfirmation(tester);
    await _enterPin(tester, '1234');
    await tester.pump();

    expect(find.textContaining('check your connection'), findsOneWidget);
    expect(find.text('Confirm transfer'), findsOneWidget);

    await _enterPin(tester, '1234');
    await tester.pump();

    expect(keys, hasLength(2));
    expect(keys.first, isNotNull);
    expect(keys.first, equals(keys.last));
  });

  testWidgets(
    '409 idempotency-in-flight keeps the confirmation open and preserves '
    'the operation key across retry',
    (tester) async {
      final keys = <String?>[];
      var attempts = 0;
      when(
        () => walletRepository.transfer(
          receiverWalletId: any(named: 'receiverWalletId'),
          amount: any(named: 'amount'),
          description: any(named: 'description'),
          pin: any(named: 'pin'),
          idempotencyKey: any(named: 'idempotencyKey'),
        ),
      ).thenAnswer((invocation) async {
        keys.add(invocation.namedArguments[#idempotencyKey] as String?);
        attempts++;
        if (attempts == 1) {
          throw const ApiException(
            message: 'Request is already being processed. Try again shortly.',
            statusCode: 409,
          );
        }
        return _successTxn;
      });

      await pumpSend(tester);
      await openConfirmation(tester);
      await _enterPin(tester, '1234');
      await tester.pump();

      // In-flight means the transfer may still execute: the sheet stays open
      // with a retry-later message — never the /insufficient recovery route.
      expect(
        find.text('Request is already being processed. Try again shortly.'),
        findsOneWidget,
      );
      expect(find.text('Confirm transfer'), findsOneWidget);
      expect(find.byType(PinPad), findsOneWidget);
      expect(find.text('Insufficient balance'), findsNothing);

      // Retrying with the PIN reuses the same idempotency key.
      await _enterPin(tester, '1234');
      await tester.pumpAndSettle();

      expect(keys, hasLength(2));
      expect(keys.first, isNotNull);
      expect(keys.first, equals(keys.last));
    },
  );

  testWidgets(
    '400 insufficient balance closes the sheet and routes to /insufficient',
    (tester) async {
      var calls = 0;
      when(
        () => walletRepository.transfer(
          receiverWalletId: any(named: 'receiverWalletId'),
          amount: any(named: 'amount'),
          description: any(named: 'description'),
          pin: any(named: 'pin'),
          idempotencyKey: any(named: 'idempotencyKey'),
        ),
      ).thenAnswer((_) async {
        calls++;
        throw const ApiException(
          message: 'Insufficient balance (amount + fee = 102.50)',
          statusCode: 400,
        );
      });

      await pumpSend(tester);
      await openConfirmation(tester);
      await _enterPin(tester, '1234');
      await tester.pumpAndSettle();

      expect(calls, 1);
      // Sheet closed — no retry loop with an inline error.
      expect(find.text('Confirm transfer'), findsNothing);
      expect(find.byType(PinPad), findsNothing);
      // Recovery screen shown instead.
      expect(find.text('Insufficient balance'), findsOneWidget);
    },
  );
}
