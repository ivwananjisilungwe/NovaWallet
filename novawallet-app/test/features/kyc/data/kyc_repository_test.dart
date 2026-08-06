import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/core/network/api_client.dart';
import 'package:novawallet_app/core/network/api_exception.dart';
import 'package:novawallet_app/features/kyc/data/kyc_repository.dart';
import 'package:novawallet_app/features/kyc/models/kyc_status.dart';

class MockApiClient extends Mock implements ApiClient {}

void main() {
  group('KycRepository', () {
    late MockApiClient mockApi;
    late KycRepository repository;

    setUp(() {
      mockApi = MockApiClient();
      repository = KycRepository(mockApi);
      registerFallbackValue(
        MultipartFile.fromBytes([1, 2, 3], filename: 'fallback.txt'),
      );
    });

    final testKycStatus = KycStatus(
      kycStatus: 'APPROVED',
      currentTier: 2,
      tierName: 'Tier 2',
      walletLimit: '10000',
      dailySendLimit: '5000',
      documents: [],
      submittedAt: DateTime.parse('2024-01-15T10:00:00Z'),
      approvedAt: DateTime.parse('2024-01-16T10:00:00Z'),
      rejectionReason: null,
    );

    group('getStatus', () {
      test('GETs /v1/kyc/status and parses as KycStatus', () async {
        when(() => mockApi.get<KycStatus>(
          '/v1/kyc/status',
          parser: any(named: 'parser'),
        )).thenAnswer((_) async => testKycStatus);

        final result = await repository.getStatus();

        expect(result, equals(testKycStatus));
        verify(() => mockApi.get<KycStatus>(
          '/v1/kyc/status',
          parser: any(named: 'parser'),
        )).called(1);
      });

      test('throws ApiException when api returns null', () async {
        when(() => mockApi.get<KycStatus>(
          '/v1/kyc/status',
          parser: any(named: 'parser'),
        )).thenAnswer((_) async => null);

        expect(
          () => repository.getStatus(),
          throwsA(isA<ApiException>().having((e) => e.message, 'message', 'KYC status unavailable')),
        );
      });
    });

    group('uploadDocument', () {
      test('sends multipart with fileFieldName "file" and form field "documentType", NO "path" query param', () async {
        when(() => mockApi.postMultipart<dynamic>(
          '/v1/kyc/documents/upload',
          file: any(named: 'file'),
          fileFieldName: 'file',
          fields: {'documentType': 'NATIONAL_ID'},
        )).thenAnswer((_) async => null);

        await repository.uploadDocument(
          documentType: 'NATIONAL_ID',
          fileName: 'id_card.jpg',
          fileBytes: [1, 2, 3, 4],
        );

        verify(() => mockApi.postMultipart<dynamic>(
          '/v1/kyc/documents/upload',
          file: any(named: 'file'),
          fileFieldName: 'file',
          fields: {'documentType': 'NATIONAL_ID'},
        )).called(1);
      });

      test('sends correct documentType for PASSPORT', () async {
        when(() => mockApi.postMultipart<dynamic>(
          '/v1/kyc/documents/upload',
          file: any(named: 'file'),
          fileFieldName: 'file',
          fields: {'documentType': 'PASSPORT'},
        )).thenAnswer((_) async => null);

        await repository.uploadDocument(
          documentType: 'PASSPORT',
          fileName: 'passport.jpg',
          fileBytes: [1, 2, 3],
        );

        verify(() => mockApi.postMultipart<dynamic>(
          '/v1/kyc/documents/upload',
          file: any(named: 'file'),
          fileFieldName: 'file',
          fields: {'documentType': 'PASSPORT'},
        )).called(1);
      });

      test('sends correct documentType for SELFIE', () async {
        when(() => mockApi.postMultipart<dynamic>(
          '/v1/kyc/documents/upload',
          file: any(named: 'file'),
          fileFieldName: 'file',
          fields: {'documentType': 'SELFIE'},
        )).thenAnswer((_) async => null);

        await repository.uploadDocument(
          documentType: 'SELFIE',
          fileName: 'selfie.jpg',
          fileBytes: [1, 2, 3],
        );

        verify(() => mockApi.postMultipart<dynamic>(
          '/v1/kyc/documents/upload',
          file: any(named: 'file'),
          fileFieldName: 'file',
          fields: {'documentType': 'SELFIE'},
        )).called(1);
      });

      test('sends correct documentType for PROOF_OF_ADDRESS', () async {
        when(() => mockApi.postMultipart<dynamic>(
          '/v1/kyc/documents/upload',
          file: any(named: 'file'),
          fileFieldName: 'file',
          fields: {'documentType': 'PROOF_OF_ADDRESS'},
        )).thenAnswer((_) async => null);

        await repository.uploadDocument(
          documentType: 'PROOF_OF_ADDRESS',
          fileName: 'utility_bill.pdf',
          fileBytes: [1, 2, 3],
        );

        verify(() => mockApi.postMultipart<dynamic>(
          '/v1/kyc/documents/upload',
          file: any(named: 'file'),
          fileFieldName: 'file',
          fields: {'documentType': 'PROOF_OF_ADDRESS'},
        )).called(1);
      });
    });

    group('submitKyc', () {
      test('POSTs to /v1/kyc/submit', () async {
        when(() => mockApi.post<dynamic>('/v1/kyc/submit'))
            .thenAnswer((_) async => null);

        await repository.submitKyc();

        verify(() => mockApi.post<dynamic>('/v1/kyc/submit')).called(1);
      });
    });
  });
}