import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/core/network/api_client.dart';
import 'package:novawallet_app/core/network/api_exception.dart';
import 'package:novawallet_app/features/profile/data/user_repository.dart';

class MockApiClient extends Mock implements ApiClient {}

void main() {
  group('UserRepository', () {
    late MockApiClient mockApi;
    late UserRepository repository;

    setUp(() {
      mockApi = MockApiClient();
      repository = UserRepository(mockApi);
    });

    final testProfile = UserProfile(
      id: 'user123',
      firstName: 'Jane',
      lastName: 'Doe',
      email: 'jane@example.com',
      phone: '+260000000000',
      role: 'USER',
      emailVerified: true,
      pinSet: true,
    );

    group('getProfile', () {
      test('GETs /v1/users/me and parses as UserProfile', () async {
        when(
          () => mockApi.get<UserProfile>(
            '/v1/users/me',
            parser: any(named: 'parser'),
          ),
        ).thenAnswer((_) async => testProfile);

        final result = await repository.getProfile();

        expect(result, equals(testProfile));
        verify(
          () => mockApi.get<UserProfile>(
            '/v1/users/me',
            parser: any(named: 'parser'),
          ),
        ).called(1);
      });

      test('throws ApiException when api returns null', () async {
        when(
          () => mockApi.get<UserProfile>(
            '/v1/users/me',
            parser: any(named: 'parser'),
          ),
        ).thenAnswer((_) async => null);

        expect(() => repository.getProfile(), throwsA(isA<ApiException>()));
      });
    });

    group('updateProfile', () {
      test(
        'PUTs to /v1/users/me with updatable fields and parses as UserProfile',
        () async {
          when(
            () => mockApi.put<UserProfile>(
              '/v1/users/me',
              body: any(named: 'body'),
              parser: any(named: 'parser'),
            ),
          ).thenAnswer((_) async => testProfile);

          final result = await repository.updateProfile(testProfile);

          expect(result, equals(testProfile));
          verify(
            () => mockApi.put<UserProfile>(
              '/v1/users/me',
              body: {
                'firstName': 'Jane',
                'lastName': 'Doe',
                'phone': '+260000000000',
              },
              parser: any(named: 'parser'),
            ),
          ).called(1);
        },
      );

      test('omits null optional fields from the body', () async {
        final partialProfile = UserProfile(
          id: 'user123',
          email: 'jane@example.com',
          emailVerified: true,
          pinSet: true,
        );

        when(
          () => mockApi.put<UserProfile>(
            '/v1/users/me',
            body: any(named: 'body'),
            parser: any(named: 'parser'),
          ),
        ).thenAnswer((_) async => partialProfile);

        await repository.updateProfile(partialProfile);

        verify(
          () => mockApi.put<UserProfile>(
            '/v1/users/me',
            body: const {},
            parser: any(named: 'parser'),
          ),
        ).called(1);
      });

      test('throws ApiException when api returns null', () async {
        when(
          () => mockApi.put<UserProfile>(
            '/v1/users/me',
            body: any(named: 'body'),
            parser: any(named: 'parser'),
          ),
        ).thenAnswer((_) async => null);

        expect(
          () => repository.updateProfile(testProfile),
          throwsA(isA<ApiException>()),
        );
      });
    });
  });
}
