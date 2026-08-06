import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/core/network/api_client.dart';
import 'package:novawallet_app/core/network/api_exception.dart';
import 'package:novawallet_app/features/transaction/data/transaction_repository.dart';
import 'package:novawallet_app/features/transaction/models/fee_estimate.dart';
import 'package:novawallet_app/features/transaction/models/transaction.dart';

class MockApiClient extends Mock implements ApiClient {}

void main() {
  group('TransactionRepository', () {
    late MockApiClient mockApi;
    late TransactionRepository repository;

    setUp(() {
      mockApi = MockApiClient();
      repository = TransactionRepository(mockApi);
    });

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
      createdAt: null,
    );

    group('getHistory', () {
      final testPage = PagedResponse<WalletTransaction>(
        items: [testTransaction],
        totalElements: 1,
        page: 0,
        size: 10,
        totalPages: 1,
      );

      test('GETs /v1/wallets/{id}/transactions and parses items', () async {
        when(
          () => mockApi.get<PagedResponse<WalletTransaction>>(
            '/v1/wallets/wallet123/transactions',
            parser: any(named: 'parser'),
          ),
        ).thenAnswer((_) async => testPage);

        final result = await repository.getHistory('wallet123');

        expect(result, equals([testTransaction]));
        verify(
          () => mockApi.get<PagedResponse<WalletTransaction>>(
            '/v1/wallets/wallet123/transactions',
            parser: any(named: 'parser'),
          ),
        ).called(1);
      });

      test('returns empty list when api returns null', () async {
        when(
          () => mockApi.get<PagedResponse<WalletTransaction>>(
            '/v1/wallets/wallet123/transactions',
            parser: any(named: 'parser'),
          ),
        ).thenAnswer((_) async => null);

        final result = await repository.getHistory('wallet123');

        expect(result, isEmpty);
        verify(
          () => mockApi.get<PagedResponse<WalletTransaction>>(
            '/v1/wallets/wallet123/transactions',
            parser: any(named: 'parser'),
          ),
        ).called(1);
      });

      test('returns empty list when page has no items', () async {
        final emptyPage = PagedResponse<WalletTransaction>(
          items: const [],
          totalElements: 0,
          page: 0,
          size: 10,
          totalPages: 0,
        );

        when(
          () => mockApi.get<PagedResponse<WalletTransaction>>(
            '/v1/wallets/wallet123/transactions',
            parser: any(named: 'parser'),
          ),
        ).thenAnswer((_) async => emptyPage);

        final result = await repository.getHistory('wallet123');

        expect(result, isEmpty);
      });
    });

    group('getByReference', () {
      test(
        'GETs /v1/transactions/{reference} and parses as WalletTransaction',
        () async {
          when(
            () => mockApi.get<WalletTransaction>(
              '/v1/transactions/REF-123',
              parser: any(named: 'parser'),
            ),
          ).thenAnswer((_) async => testTransaction);

          final result = await repository.getByReference('REF-123');

          expect(result, equals(testTransaction));
          verify(
            () => mockApi.get<WalletTransaction>(
              '/v1/transactions/REF-123',
              parser: any(named: 'parser'),
            ),
          ).called(1);
        },
      );

      test('throws ApiException when transaction returns null', () async {
        when(
          () => mockApi.get<WalletTransaction>(
            '/v1/transactions/REF-123',
            parser: any(named: 'parser'),
          ),
        ).thenAnswer((_) async => null);

        expect(
          () => repository.getByReference('REF-123'),
          throwsA(isA<ApiException>()),
        );
      });
    });

    group('estimateFee', () {
      final testFee = FeeEstimate(
        transactionType: 'TRANSFER',
        amount: '500.00',
        percentageFee: '2%',
        flatFee: '5.00',
        minFee: '2.00',
        maxFee: '50.00',
        totalFee: '15.00',
      );

      test(
        'GETs /v1/fees/estimate with query params and parses as FeeEstimate',
        () async {
          when(
            () => mockApi.get<FeeEstimate>(
              '/v1/fees/estimate',
              query: {'type': 'TRANSFER', 'amount': '500.00'},
              parser: any(named: 'parser'),
            ),
          ).thenAnswer((_) async => testFee);

          final result = await repository.estimateFee(
            type: 'TRANSFER',
            amount: '500.00',
          );

          expect(result, equals(testFee));
          verify(
            () => mockApi.get<FeeEstimate>(
              '/v1/fees/estimate',
              query: {'type': 'TRANSFER', 'amount': '500.00'},
              parser: any(named: 'parser'),
            ),
          ).called(1);
        },
      );

      test('throws ApiException when fee estimate returns null', () async {
        when(
          () => mockApi.get<FeeEstimate>(
            '/v1/fees/estimate',
            query: {'type': 'TRANSFER', 'amount': '500.00'},
            parser: any(named: 'parser'),
          ),
        ).thenAnswer((_) async => null);

        expect(
          () => repository.estimateFee(type: 'TRANSFER', amount: '500.00'),
          throwsA(isA<ApiException>()),
        );
      });
    });
  });
}
