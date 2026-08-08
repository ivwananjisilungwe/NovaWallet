import 'dart:async';

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
import 'package:novawallet_app/features/wallet/models/wallet.dart';
import 'package:novawallet_app/features/wallet/providers/wallet_provider.dart';
import 'package:novawallet_app/features/wallet/screens/withdraw_screen.dart';

class _MockWalletRepository extends Mock implements WalletRepository {}

class _MockTransactionRepository extends Mock
    implements TransactionRepository {}

const _wallet = Wallet(
  id: 'wallet-1',
  accountNumber: 'NW-100001',
  balance: '500.00',
);

const _successTxn = WalletTransaction(
  id: 'txn-1',
  reference: 'NW-REF-1',
  type: 'WITHDRAWAL_DEBIT',
  amount: '100.00',
  status: 'SUCCESSFUL',
);

Future<void> _enterPin(WidgetTester tester, String pin) async {
  tester.widget<PinPad>(find.byType(PinPad)).onComplete(pin);
  await tester.pump();
}

/// Stub `walletRepository.withdraw` so the given [failure] happens on the
/// first attempt (shown inline) and every later attempt returns [_successTxn].
void _stubWithdrawInline(
  _MockWalletRepository repo,
  List<String?> keys,
  ApiException failure,
) {
  var attempts = 0;
  when(
    () => repo.withdraw(
      walletId: any(named: 'walletId'),
      amount: any(named: 'amount'),
      description: any(named: 'description'),
      pin: any(named: 'pin'),
      idempotencyKey: any(named: 'idempotencyKey'),
    ),
  ).thenAnswer((invocation) async {
    keys.add(invocation.namedArguments[#idempotencyKey] as String?);
    attempts++;
    if (attempts == 1) throw failure;
    return _successTxn;
  });
}

void main() {
  late _MockWalletRepository walletRepository;
  late _MockTransactionRepository transactionRepository;

  setUp(() {
    walletRepository = _MockWalletRepository();
    transactionRepository = _MockTransactionRepository();
    when(() => walletRepository.getMyWallet()).thenAnswer((_) async => _wallet);
    when(() => walletRepository.getBalance(any())).thenAnswer(
      (_) async => _wallet,
    );
    when(
      () => transactionRepository.estimateFee(
        type: 'WITHDRAWAL',
        amount: '100',
      ),
    ).thenAnswer(
      (_) async => const FeeEstimate(
        transactionType: 'WITHDRAWAL',
        amount: '100',
        totalFee: '2.50',
      ),
    );
  });

  Future<void> pumpWithdraw(WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(420, 900));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final router = GoRouter(
      initialLocation: '/withdraw',
      routes: [
        GoRoute(path: '/withdraw', builder: (_, __) => const WithdrawScreen()),
        GoRoute(
          path: '/insufficient',
          builder: (_, __) => const InsufficientBalanceScreen(),
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
    await tester.enterText(find.byType(TextField).at(0), '100');
    await tester.tap(find.byType(PillButton));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 50));
  }

  testWidgets('discloses destination, fee, and total with Cancel visible '
      'before PIN authorization', (tester) async {
    await pumpWithdraw(tester);
    await openConfirmation(tester);

    expect(find.text('Confirm withdrawal'), findsOneWidget);
    expect(find.text('Linked payout account'), findsOneWidget);
    expect(find.text('ZMW 100.00'), findsWidgets);
    expect(find.text('ZMW 2.50'), findsWidgets);
    expect(find.text('ZMW 102.50'), findsWidgets);
    expect(find.textContaining('authorizes this withdrawal'), findsOneWidget);
    final cancel = tester.widget<TextButton>(
      find.widgetWithText(TextButton, 'Cancel'),
    );
    expect(cancel.onPressed, isNotNull);
  });

  testWidgets(
    '409 insufficient balance closes the PIN sheet and routes to '
    '/insufficient without retrying',
    (tester) async {
      var calls = 0;
      when(
        () => walletRepository.withdraw(
          walletId: any(named: 'walletId'),
          amount: any(named: 'amount'),
          description: any(named: 'description'),
          pin: any(named: 'pin'),
          idempotencyKey: any(named: 'idempotencyKey'),
        ),
      ).thenAnswer((_) async {
        calls++;
        throw const ApiException(
          message: 'Insufficient balance',
          statusCode: 409,
        );
      });

      await pumpWithdraw(tester);
      await openConfirmation(tester);
      await _enterPin(tester, '1234');
      await tester.pumpAndSettle();

      expect(calls, 1);
      // The sheet must be closed — no retry loop with an inline error.
      expect(find.text('Confirm withdrawal'), findsNothing);
      expect(find.byType(PinPad), findsNothing);
      // The recovery screen must be shown instead.
      expect(find.text('Insufficient balance'), findsOneWidget);
    },
  );

  testWidgets('blocks system-back dismissal while authorization is in flight',
      (tester) async {
    final completer = Completer<WalletTransaction>();
    when(
      () => walletRepository.withdraw(
        walletId: any(named: 'walletId'),
        amount: any(named: 'amount'),
        description: any(named: 'description'),
        pin: any(named: 'pin'),
        idempotencyKey: any(named: 'idempotencyKey'),
      ),
    ).thenAnswer((_) => completer.future);

    await pumpWithdraw(tester);
    await openConfirmation(tester);
    await _enterPin(tester, '1234');
    await tester.pump();

    // Authorization in flight: the sheet must survive a system back.
    expect(find.text('Confirm withdrawal'), findsOneWidget);
    await tester.binding.handlePopRoute();
    await tester.pumpAndSettle();
    expect(find.text('Confirm withdrawal'), findsOneWidget);

    completer.complete(_successTxn);
    await tester.pumpAndSettle();

    expect(find.text('Withdrawal successful'), findsOneWidget);
    expect(find.text('Ref: NW-REF-1'), findsOneWidget);
  });

  testWidgets('blocks barrier-tap dismissal while authorization is in flight',
      (tester) async {
    final completer = Completer<WalletTransaction>();
    when(
      () => walletRepository.withdraw(
        walletId: any(named: 'walletId'),
        amount: any(named: 'amount'),
        description: any(named: 'description'),
        pin: any(named: 'pin'),
        idempotencyKey: any(named: 'idempotencyKey'),
      ),
    ).thenAnswer((_) => completer.future);

    await pumpWithdraw(tester);
    await openConfirmation(tester);
    await _enterPin(tester, '1234');
    await tester.pump();

    // Tap the modal barrier above the sheet: must not dismiss.
    await tester.tapAt(const Offset(10, 30));
    await tester.pumpAndSettle();
    expect(find.text('Confirm withdrawal'), findsOneWidget);

    completer.complete(_successTxn);
    await tester.pumpAndSettle();
    expect(find.text('Withdrawal successful'), findsOneWidget);
  });

  testWidgets('blocks drag dismissal while authorization is in flight', (
    tester,
  ) async {
    final completer = Completer<WalletTransaction>();
    when(
      () => walletRepository.withdraw(
        walletId: any(named: 'walletId'),
        amount: any(named: 'amount'),
        description: any(named: 'description'),
        pin: any(named: 'pin'),
        idempotencyKey: any(named: 'idempotencyKey'),
      ),
    ).thenAnswer((_) => completer.future);

    await pumpWithdraw(tester);
    await openConfirmation(tester);
    await _enterPin(tester, '1234');
    await tester.pump();

    // Fling the sheet downwards: must not dismiss.
    await tester.drag(find.text('Confirm withdrawal'), const Offset(0, 400));
    await tester.pumpAndSettle();
    expect(find.text('Confirm withdrawal'), findsOneWidget);

    completer.complete(_successTxn);
    await tester.pumpAndSettle();
    expect(find.text('Withdrawal successful'), findsOneWidget);
  });

  testWidgets('network error keeps the operation key and rebuilds the PIN pad', (
    tester,
  ) async {
    final keys = <String?>[];
    _stubWithdrawInline(
      walletRepository,
      keys,
      const ApiException(message: 'Timed out'),
    );

    await pumpWithdraw(tester);
    await openConfirmation(tester);
    await _enterPin(tester, '1234');
    await tester.pump();

    expect(find.textContaining('check your connection'), findsOneWidget);
    expect(find.text('Confirm withdrawal'), findsOneWidget);

    await _enterPin(tester, '1234');
    await tester.pumpAndSettle();

    expect(keys, hasLength(2));
    expect(keys.first, isNotNull);
    expect(keys.first, equals(keys.last));
  });

  testWidgets('429 rate-limit keeps the operation key on retry', (
    tester,
  ) async {
    final keys = <String?>[];
    _stubWithdrawInline(
      walletRepository,
      keys,
      const ApiException(message: 'Too many requests', statusCode: 429),
    );

    await pumpWithdraw(tester);
    await openConfirmation(tester);
    await _enterPin(tester, '1234');
    await tester.pump();

    expect(find.text('Too many requests'), findsOneWidget);

    await _enterPin(tester, '1234');
    await tester.pumpAndSettle();

    expect(keys, hasLength(2));
    expect(keys.first, equals(keys.last));
  });

  testWidgets('server 5xx error keeps the operation key on retry', (
    tester,
  ) async {
    final keys = <String?>[];
    _stubWithdrawInline(
      walletRepository,
      keys,
      const ApiException(message: 'Internal error', statusCode: 500),
    );

    await pumpWithdraw(tester);
    await openConfirmation(tester);
    await _enterPin(tester, '1234');
    await tester.pump();

    expect(find.text('Internal error'), findsOneWidget);

    await _enterPin(tester, '1234');
    await tester.pumpAndSettle();

    expect(keys, hasLength(2));
    expect(keys.first, equals(keys.last));
  });

  testWidgets('definitive business failure rotates the operation key', (
    tester,
  ) async {
    final keys = <String?>[];
    _stubWithdrawInline(
      walletRepository,
      keys,
      const ApiException(message: 'Invalid PIN', statusCode: 400),
    );

    await pumpWithdraw(tester);
    await openConfirmation(tester);
    await _enterPin(tester, '1234');
    await tester.pump();

    expect(find.text('Invalid PIN'), findsOneWidget);

    await _enterPin(tester, '1234');
    await tester.pumpAndSettle();

    expect(keys, hasLength(2));
    expect(keys.first, isNotNull);
    expect(keys.first, isNot(equals(keys.last)));
  });
}