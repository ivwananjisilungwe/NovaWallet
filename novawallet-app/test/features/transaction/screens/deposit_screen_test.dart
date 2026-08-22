import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/core/network/api_exception.dart';
import 'package:novawallet_app/core/widgets/widgets.dart';
import 'package:novawallet_app/features/transaction/models/transaction.dart';
import 'package:novawallet_app/features/transaction/screens/success_screen.dart';
import 'package:novawallet_app/features/wallet/data/wallet_repository.dart';
import 'package:novawallet_app/features/wallet/models/wallet.dart';
import 'package:novawallet_app/features/wallet/providers/wallet_provider.dart';
import 'package:novawallet_app/features/wallet/screens/deposit_screen.dart';

class _MockWalletRepository extends Mock implements WalletRepository {}

const _wallet = Wallet(
  id: 'wallet-1',
  accountNumber: 'NW-100001',
  balance: '500.00',
);

const _successTxn = WalletTransaction(
  id: 'txn-1',
  reference: 'NW-REF-1',
  type: 'DEPOSIT_CREDIT',
  amount: '100.00',
  status: 'SUCCESSFUL',
);

/// Stub `walletRepository.deposit` so the given [failure] happens on the
/// first attempt and every later attempt returns [_successTxn].
void _stubDepositWithFailure(
  _MockWalletRepository repo,
  List<String?> keys,
  ApiException failure,
) {
  var attempts = 0;
  when(
    () => repo.deposit(
      walletId: any(named: 'walletId'),
      amount: any(named: 'amount'),
      description: any(named: 'description'),
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

  setUp(() {
    walletRepository = _MockWalletRepository();
    when(() => walletRepository.getMyWallet()).thenAnswer((_) async => _wallet);
    when(
      () => walletRepository.getBalance(any()),
    ).thenAnswer((_) async => _wallet);
  });

  Future<void> pumpDeposit(WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(420, 900));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final router = GoRouter(
      initialLocation: '/deposit',
      routes: [
        GoRoute(path: '/deposit', builder: (_, _) => const DepositScreen()),
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
        ],
        child: MaterialApp.router(routerConfig: router),
      ),
    );
  }

  Future<void> submitDeposit(WidgetTester tester) async {
    await tester.enterText(find.byType(TextField).at(0), '100');
    await tester.tap(find.byType(PillButton));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 50));
  }

  /// Waits for the error SnackBar to auto-dismiss so it cannot swallow the
  /// next submit tap.
  Future<void> waitForSnackBarToDismiss(WidgetTester tester) async {
    await tester.pump(const Duration(seconds: 5));
    await tester.pumpAndSettle();
  }

  testWidgets('network error keeps the operation key on retry', (tester) async {
    final keys = <String?>[];
    _stubDepositWithFailure(
      walletRepository,
      keys,
      const ApiException(message: 'Timed out'),
    );

    await pumpDeposit(tester);
    await submitDeposit(tester);

    expect(find.textContaining('Network error'), findsOneWidget);

    await waitForSnackBarToDismiss(tester);
    await submitDeposit(tester);
    await tester.pumpAndSettle();

    expect(find.text('Deposit successful'), findsOneWidget);
    expect(keys, hasLength(2));
    expect(keys.first, isNotNull);
    expect(keys.first, equals(keys.last));
  });

  testWidgets('409 idempotency-in-flight keeps the operation key on retry', (
    tester,
  ) async {
    final keys = <String?>[];
    _stubDepositWithFailure(
      walletRepository,
      keys,
      const ApiException(
        message: 'Request is already being processed. Try again shortly.',
        statusCode: 409,
      ),
    );

    await pumpDeposit(tester);
    await submitDeposit(tester);

    // In-flight deposit may still execute: money-in stays in the flow with a
    // retry-later message, and the key must survive for dedupe.
    expect(find.textContaining('already being processed'), findsOneWidget);

    await waitForSnackBarToDismiss(tester);
    await submitDeposit(tester);
    await tester.pumpAndSettle();

    expect(keys, hasLength(2));
    expect(keys.first, isNotNull);
    expect(keys.first, equals(keys.last));
  });

  testWidgets('429 rate-limit keeps the operation key on retry', (
    tester,
  ) async {
    final keys = <String?>[];
    _stubDepositWithFailure(
      walletRepository,
      keys,
      const ApiException(message: 'Too many requests', statusCode: 429),
    );

    await pumpDeposit(tester);
    await submitDeposit(tester);

    expect(find.text('Too many requests'), findsOneWidget);

    await waitForSnackBarToDismiss(tester);
    await submitDeposit(tester);
    await tester.pumpAndSettle();

    expect(keys, hasLength(2));
    expect(keys.first, equals(keys.last));
  });

  testWidgets('server 5xx error keeps the operation key on retry', (
    tester,
  ) async {
    final keys = <String?>[];
    _stubDepositWithFailure(
      walletRepository,
      keys,
      const ApiException(message: 'Internal error', statusCode: 500),
    );

    await pumpDeposit(tester);
    await submitDeposit(tester);

    expect(find.text('Internal error'), findsOneWidget);

    await waitForSnackBarToDismiss(tester);
    await submitDeposit(tester);
    await tester.pumpAndSettle();

    expect(keys, hasLength(2));
    expect(keys.first, equals(keys.last));
  });

  testWidgets('definitive business error clears the operation key', (
    tester,
  ) async {
    final keys = <String?>[];
    _stubDepositWithFailure(
      walletRepository,
      keys,
      const ApiException(message: 'Invalid amount', statusCode: 400),
    );

    await pumpDeposit(tester);
    await submitDeposit(tester);

    expect(find.text('Invalid amount'), findsOneWidget);

    await waitForSnackBarToDismiss(tester);
    await submitDeposit(tester);
    await tester.pumpAndSettle();

    expect(keys, hasLength(2));
    expect(keys.first, isNotNull);
    expect(keys.first, isNot(equals(keys.last)));
  });

  testWidgets(
    'success clears the operation and a balance-refresh error never shows a '
    'deposit failure message',
    (tester) async {
      when(
        () => walletRepository.deposit(
          walletId: any(named: 'walletId'),
          amount: any(named: 'amount'),
          description: any(named: 'description'),
          idempotencyKey: any(named: 'idempotencyKey'),
        ),
      ).thenAnswer((_) async => _successTxn);
      // Post-success balance refresh must be best-effort: even when it fails,
      // the successful deposit must complete without an error message.
      when(() => walletRepository.getBalance(any())).thenThrow(
        const ApiException(message: 'Balance refresh failed', statusCode: 500),
      );

      await pumpDeposit(tester);
      await submitDeposit(tester);
      await tester.pumpAndSettle();

      expect(find.text('Deposit successful'), findsOneWidget);
      expect(find.text('Ref: NW-REF-1'), findsOneWidget);
      expect(find.text('ZMW 100.00'), findsOneWidget);
      expect(find.textContaining('uncertain'), findsNothing);
      expect(find.byType(SnackBar), findsNothing);
    },
  );
}
