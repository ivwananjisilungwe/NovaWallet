import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/core/network/api_client.dart';
import 'package:novawallet_app/features/admin/data/admin_repository.dart';

class MockApiClient extends Mock implements ApiClient {}

void main() {
  group('AdminRepository', () {
    late MockApiClient mockApi;
    late AdminRepository repository;

    setUp(() {
      mockApi = MockApiClient();
      repository = AdminRepository(mockApi);
    });

    group('pendingKyc', () {
      test('parses response data as a bare List (NOT PagedResponse)', () async {
        final responseData = [
          KycQueueItem(userId: '1', name: 'John Doe', email: 'john@test.com', tier: '1'),
          KycQueueItem(userId: '2', name: 'Jane Smith', email: 'jane@test.com', tier: '2'),
        ];
        when(() => mockApi.get<List<KycQueueItem>>(
          '/v1/admin/kyc/pending',
          parser: any(named: 'parser'),
        )).thenAnswer((_) async => responseData);

        final result = await repository.pendingKyc();

        expect(result, hasLength(2));
        expect(result[0].userId, '1');
        expect(result[0].name, 'John Doe');
        expect(result[0].email, 'john@test.com');
        expect(result[0].tier, '1');
        expect(result[1].userId, '2');
        expect(result[1].name, 'Jane Smith');
        expect(result[1].email, 'jane@test.com');
        expect(result[1].tier, '2');
      });

      test('returns empty list when api returns null', () async {
        when(() => mockApi.get<List<KycQueueItem>>(
          '/v1/admin/kyc/pending',
          parser: any(named: 'parser'),
        )).thenAnswer((_) async => null);

        final result = await repository.pendingKyc();

        expect(result, isEmpty);
      });

      test('handles missing fields gracefully', () async {
        final responseData = [
          KycQueueItem(userId: '3', name: 'Bob', email: 'bob@test.com', tier: '3'),
        ];
        when(() => mockApi.get<List<KycQueueItem>>(
          '/v1/admin/kyc/pending',
          parser: any(named: 'parser'),
        )).thenAnswer((_) async => responseData);

        final result = await repository.pendingKyc();

        expect(result, hasLength(1));
        expect(result[0].userId, '3');
        expect(result[0].name, 'Bob');
        expect(result[0].email, 'bob@test.com');
        expect(result[0].tier, '3');
      });
    });

    group('approveKyc', () {
      test('POSTs to /v1/admin/kyc/{userId}/approve with body {tier: tier}', () async {
        when(() => mockApi.post<dynamic>(
          '/v1/admin/kyc/user123/approve',
          body: {'tier': 2},
        )).thenAnswer((_) async => null);

        await repository.approveKyc('user123', tier: 2);

        verify(() => mockApi.post<dynamic>(
          '/v1/admin/kyc/user123/approve',
          body: {'tier': 2},
        )).called(1);
      });

      test('POSTs with tier 1', () async {
        when(() => mockApi.post<dynamic>(
          '/v1/admin/kyc/user456/approve',
          body: {'tier': 1},
        )).thenAnswer((_) async => null);

        await repository.approveKyc('user456', tier: 1);

        verify(() => mockApi.post<dynamic>(
          '/v1/admin/kyc/user456/approve',
          body: {'tier': 1},
        )).called(1);
      });
    });

    group('rejectKyc', () {
      test('POSTs to /v1/admin/kyc/{userId}/reject with body {reason: reason}', () async {
        when(() => mockApi.post<dynamic>(
          '/v1/admin/kyc/user123/reject',
          body: {'reason': 'Invalid document'},
        )).thenAnswer((_) async => null);

        await repository.rejectKyc('user123', reason: 'Invalid document');

        verify(() => mockApi.post<dynamic>(
          '/v1/admin/kyc/user123/reject',
          body: {'reason': 'Invalid document'},
        )).called(1);
      });

      test('POSTs with different reason', () async {
        when(() => mockApi.post<dynamic>(
          '/v1/admin/kyc/user789/reject',
          body: {'reason': 'Expired document'},
        )).thenAnswer((_) async => null);

        await repository.rejectKyc('user789', reason: 'Expired document');

        verify(() => mockApi.post<dynamic>(
          '/v1/admin/kyc/user789/reject',
          body: {'reason': 'Expired document'},
        )).called(1);
      });
    });
  });
}