import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/core/network/api_client.dart';
import 'package:novawallet_app/core/network/api_exception.dart';
import 'package:novawallet_app/features/statement/data/statement_repository.dart';
import 'package:novawallet_app/features/statement/models/statement_summary.dart';

class MockApiClient extends Mock implements ApiClient {}

void main() {
  group('StatementRepository', () {
    late MockApiClient mockApi;
    late StatementRepository repository;

    setUp(() {
      mockApi = MockApiClient();
      repository = StatementRepository(mockApi);
    });

    final testSummary = StatementSummary(
      month: '2026-08',
      totalDeposits: '1500.00',
      totalWithdrawals: '400.00',
      totalTransfersIn: '300.00',
      totalTransfersOut: '200.00',
      totalFees: '25.00',
      netChange: '1175.00',
      transactionCount: 12,
    );

    group('getMonthlyStatement', () {
      test(
        'GETs /v1/wallets/{id}/statements?month=yyyy-MM and parses summary',
        () async {
          when(
            () => mockApi.get<StatementSummary>(
              '/v1/wallets/wallet123/statements',
              query: {'month': '2026-08'},
              parser: any(named: 'parser'),
            ),
          ).thenAnswer((_) async => testSummary);

          final result = await repository.getMonthlyStatement(
            'wallet123',
            '2026-08',
          );

          expect(result, equals(testSummary));
          verify(
            () => mockApi.get<StatementSummary>(
              '/v1/wallets/wallet123/statements',
              query: {'month': '2026-08'},
              parser: any(named: 'parser'),
            ),
          ).called(1);
        },
      );

      test('throws ApiException when summary returns null', () async {
        when(
          () => mockApi.get<StatementSummary>(
            '/v1/wallets/wallet123/statements',
            query: {'month': '2026-08'},
            parser: any(named: 'parser'),
          ),
        ).thenAnswer((_) async => null);

        expect(
          () => repository.getMonthlyStatement('wallet123', '2026-08'),
          throwsA(isA<ApiException>()),
        );
      });
    });
  });
}