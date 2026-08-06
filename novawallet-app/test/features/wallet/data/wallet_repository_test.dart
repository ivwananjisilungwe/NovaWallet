import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/core/network/api_client.dart';
import 'package:novawallet_app/features/transaction/models/transaction.dart';
import 'package:novawallet_app/features/wallet/data/wallet_repository.dart';
import 'package:novawallet_app/features/wallet/models/wallet.dart';

class MockApiClient extends Mock implements ApiClient {}

void main() {
  group('WalletRepository', () {
    late MockApiClient mockApi;
    late WalletRepository repository;

    setUp(() {
      mockApi = MockApiClient();
      repository = WalletRepository(mockApi);
    });

    final testWallet = Wallet(
      id: 'wallet123',
      accountNumber: 'NW-100001',
      balance: '1000.00',
      currency: 'ZMW',
      status: 'ACTIVE',
    );

    final testTransaction = WalletTransaction(
      id: 'txn123',
      reference: 'REF-123',
      type: 'DEPOSIT',
      amount: '500.00',
      status: 'SUCCESSFUL',
      balanceBefore: '500.00',
      balanceAfter: '1000.00',
      description: 'Test deposit',
      senderWalletId: null,
      receiverWalletId: 'wallet123',
    );

    group('getMyWallet', () {
      test('GETs /v1/wallets/me and parses as Wallet', () async {
        when(() => mockApi.get<Wallet>(
          '/v1/wallets/me',
          parser: any(named: 'parser'),
        )).thenAnswer((_) async => testWallet);

        final result = await repository.getMyWallet();

        expect(result, equals(testWallet));
        verify(() => mockApi.get<Wallet>(
          '/v1/wallets/me',
          parser: any(named: 'parser'),
        )).called(1);
      });

      test('throws when api returns null', () async {
        when(() => mockApi.get<Wallet>(
          '/v1/wallets/me',
          parser: any(named: 'parser'),
        )).thenAnswer((_) async => null);

        expect(() => repository.getMyWallet(), throwsA(isA<Exception>()));
      });
    });

    group('getBalance', () {
      test('GETs /v1/wallets/{id}/balance and parses as Wallet', () async {
        when(() => mockApi.get<Wallet>(
          '/v1/wallets/wallet123/balance',
          parser: any(named: 'parser'),
        )).thenAnswer((_) async => testWallet);

        final result = await repository.getBalance('wallet123');

        expect(result, equals(testWallet));
        verify(() => mockApi.get<Wallet>(
          '/v1/wallets/wallet123/balance',
          parser: any(named: 'parser'),
        )).called(1);
      });

      test('throws when api returns null', () async {
        when(() => mockApi.get<Wallet>(
          '/v1/wallets/wallet123/balance',
          parser: any(named: 'parser'),
        )).thenAnswer((_) async => null);

        expect(() => repository.getBalance('wallet123'), throwsA(isA<Exception>()));
      });
    });

    group('deposit', () {
      test('POSTs to /v1/wallets/{walletId}/deposit and parses response as WalletTransaction (NOT Wallet)', () async {
        when(() => mockApi.post<WalletTransaction>(
          '/v1/wallets/wallet123/deposit',
          body: any(named: 'body'),
          parser: any(named: 'parser'),
          idempotencyKey: any(named: 'idempotencyKey'),
        )).thenAnswer((_) async => testTransaction);

        final result = await repository.deposit(
          walletId: 'wallet123',
          amount: '500.00',
          description: 'Test deposit',
        );

        expect(result, equals(testTransaction));
        expect(result, isA<WalletTransaction>());
        expect(result, isNot(isA<Wallet>()));
        verify(() => mockApi.post<WalletTransaction>(
          '/v1/wallets/wallet123/deposit',
          body: any(named: 'body'),
          parser: any(named: 'parser'),
          idempotencyKey: any(named: 'idempotencyKey'),
        )).called(1);
      });

      test('includes idempotencyKey when provided', () async {
        when(() => mockApi.post<WalletTransaction>(
          '/v1/wallets/wallet123/deposit',
          body: any(named: 'body'),
          parser: any(named: 'parser'),
          idempotencyKey: 'idem-123',
        )).thenAnswer((_) async => testTransaction);

        await repository.deposit(
          walletId: 'wallet123',
          amount: '500.00',
          description: 'Test deposit',
          idempotencyKey: 'idem-123',
        );

        verify(() => mockApi.post<WalletTransaction>(
          '/v1/wallets/wallet123/deposit',
          body: any(named: 'body'),
          parser: any(named: 'parser'),
          idempotencyKey: 'idem-123',
        )).called(1);
      });

      test('throws when api returns null', () async {
        when(() => mockApi.post<WalletTransaction>(
          '/v1/wallets/wallet123/deposit',
          body: any(named: 'body'),
          parser: any(named: 'parser'),
          idempotencyKey: any(named: 'idempotencyKey'),
        )).thenAnswer((_) async => null);

        expect(
          () => repository.deposit(
            walletId: 'wallet123',
            amount: '500.00',
            description: 'Test deposit',
          ),
          throwsA(isA<Exception>()),
        );
      });
    });

    group('withdraw', () {
      test('POSTs to /v1/wallets/{walletId}/withdraw with pin and parses as WalletTransaction', () async {
        when(() => mockApi.post<WalletTransaction>(
          '/v1/wallets/wallet123/withdraw',
          body: {
            'amount': '200.00',
            'description': 'Test withdrawal',
            'pin': '1234',
          },
          parser: any(named: 'parser'),
          idempotencyKey: any(named: 'idempotencyKey'),
        )).thenAnswer((_) async => testTransaction);

        final result = await repository.withdraw(
          walletId: 'wallet123',
          amount: '200.00',
          description: 'Test withdrawal',
          pin: '1234',
        );

        expect(result, equals(testTransaction));
        expect(result, isA<WalletTransaction>());
        expect(result, isNot(isA<Wallet>()));
        verify(() => mockApi.post<WalletTransaction>(
          '/v1/wallets/wallet123/withdraw',
          body: {
            'amount': '200.00',
            'description': 'Test withdrawal',
            'pin': '1234',
          },
          parser: any(named: 'parser'),
          idempotencyKey: any(named: 'idempotencyKey'),
        )).called(1);
      });

      test('throws when api returns null', () async {
        when(() => mockApi.post<WalletTransaction>(
          '/v1/wallets/wallet123/withdraw',
          body: any(named: 'body'),
          parser: any(named: 'parser'),
          idempotencyKey: any(named: 'idempotencyKey'),
        )).thenAnswer((_) async => null);

        expect(
          () => repository.withdraw(
            walletId: 'wallet123',
            amount: '200.00',
            description: 'Test withdrawal',
            pin: '1234',
          ),
          throwsA(isA<Exception>()),
        );
      });
    });

    group('transfer', () {
      test('POSTs to /v1/transfers with receiverWalletId, amount, pin, description and parses as WalletTransaction', () async {
        when(() => mockApi.post<WalletTransaction>(
          '/v1/transfers',
          body: {
            'receiverWalletId': 'wallet456',
            'amount': '300.00',
            'pin': '1234',
            'description': 'Test transfer',
          },
          parser: any(named: 'parser'),
          idempotencyKey: any(named: 'idempotencyKey'),
        )).thenAnswer((_) async => testTransaction);

        final result = await repository.transfer(
          receiverWalletId: 'wallet456',
          amount: '300.00',
          description: 'Test transfer',
          pin: '1234',
        );

        expect(result, equals(testTransaction));
        expect(result, isA<WalletTransaction>());
        expect(result, isNot(isA<Wallet>()));
        verify(() => mockApi.post<WalletTransaction>(
          '/v1/transfers',
          body: {
            'receiverWalletId': 'wallet456',
            'amount': '300.00',
            'pin': '1234',
            'description': 'Test transfer',
          },
          parser: any(named: 'parser'),
          idempotencyKey: any(named: 'idempotencyKey'),
        )).called(1);
      });

      test('throws when api returns null', () async {
        when(() => mockApi.post<WalletTransaction>(
          '/v1/transfers',
          body: any(named: 'body'),
          parser: any(named: 'parser'),
          idempotencyKey: any(named: 'idempotencyKey'),
        )).thenAnswer((_) async => null);

        expect(
          () => repository.transfer(
            receiverWalletId: 'wallet456',
            amount: '300.00',
            description: 'Test transfer',
            pin: '1234',
          ),
          throwsA(isA<Exception>()),
        );
      });
    });
  });
}