import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/core/network/api_client.dart';
import 'package:novawallet_app/core/network/api_exception.dart';
import 'package:novawallet_app/core/storage/token_storage.dart';
import 'package:novawallet_app/features/auth/data/auth_repository.dart';
import 'package:novawallet_app/features/auth/models/user.dart';

class MockApiClient extends Mock implements ApiClient {}
class MockTokenStorage extends Mock implements TokenStorage {}

void main() {
  group('AuthRepository', () {
    late MockApiClient mockApi;
    late MockTokenStorage mockStorage;
    late AuthRepository repository;

    setUp(() {
      mockApi = MockApiClient();
      mockStorage = MockTokenStorage();
      repository = AuthRepository(mockApi, mockStorage);
    });

    final testAuthResponse = AuthResponse(
      accessToken: 'access123',
      refreshToken: 'refresh456',
      user: User(
        id: '1',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@test.com',
        phone: '+260971234567',
        role: 'USER',
      ),
    );

    group('register', () {
      test('posts to /v1/auth/register with correct body and persists session', () async {
        when(() => mockApi.post<AuthResponse>(
          '/v1/auth/register',
          body: any(named: 'body'),
          parser: any(named: 'parser'),
        )).thenAnswer((_) async => testAuthResponse);

        when(() => mockStorage.saveSession(
          accessToken: 'access123',
          refreshToken: 'refresh456',
          userJson: any(named: 'userJson'),
        )).thenAnswer((_) async {});

        final result = await repository.register(
          firstName: 'John',
          lastName: 'Doe',
          email: 'john@test.com',
          phone: '+260971234567',
          password: 'password123',
        );

        expect(result, equals(testAuthResponse));
        verify(() => mockApi.post<AuthResponse>(
          '/v1/auth/register',
          body: {
            'firstName': 'John',
            'lastName': 'Doe',
            'email': 'john@test.com',
            'phone': '+260971234567',
            'password': 'password123',
          },
          parser: any(named: 'parser'),
        )).called(1);
        verify(() => mockStorage.saveSession(
          accessToken: 'access123',
          refreshToken: 'refresh456',
          userJson: any(named: 'userJson'),
        )).called(1);
      });

      test('throws ApiException when api returns null', () async {
        when(() => mockApi.post<AuthResponse>(
          '/v1/auth/register',
          body: any(named: 'body'),
          parser: any(named: 'parser'),
        )).thenAnswer((_) async => null);

        expect(
          () => repository.register(
            firstName: 'John',
            lastName: 'Doe',
            email: 'john@test.com',
            phone: '+260971234567',
            password: 'password123',
          ),
          throwsA(isA<ApiException>().having((e) => e.message, 'message', 'Registration failed.')),
        );
      });
    });

    group('login', () {
      test('posts to /v1/auth/login with correct body and persists session', () async {
        when(() => mockApi.post<AuthResponse>(
          '/v1/auth/login',
          body: any(named: 'body'),
          parser: any(named: 'parser'),
        )).thenAnswer((_) async => testAuthResponse);

        when(() => mockStorage.saveSession(
          accessToken: 'access123',
          refreshToken: 'refresh456',
          userJson: any(named: 'userJson'),
        )).thenAnswer((_) async {});

        final result = await repository.login(
          email: 'john@test.com',
          password: 'password123',
        );

        expect(result, equals(testAuthResponse));
        verify(() => mockApi.post<AuthResponse>(
          '/v1/auth/login',
          body: {'email': 'john@test.com', 'password': 'password123'},
          parser: any(named: 'parser'),
        )).called(1);
        verify(() => mockStorage.saveSession(
          accessToken: 'access123',
          refreshToken: 'refresh456',
          userJson: any(named: 'userJson'),
        )).called(1);
      });

      test('throws ApiException when api returns null', () async {
        when(() => mockApi.post<AuthResponse>(
          '/v1/auth/login',
          body: any(named: 'body'),
          parser: any(named: 'parser'),
        )).thenAnswer((_) async => null);

        expect(
          () => repository.login(email: 'john@test.com', password: 'password123'),
          throwsA(isA<ApiException>().having((e) => e.message, 'message', 'Login failed.')),
        );
      });
    });

    group('forgotPassword', () {
      test('posts to /v1/password/forgot with email', () async {
        when(() => mockApi.post<Never>(
          '/v1/password/forgot',
          body: {'email': 'john@test.com'},
        )).thenAnswer((_) async => null);

        await repository.forgotPassword('john@test.com');

        verify(() => mockApi.post<Never>(
          '/v1/password/forgot',
          body: {'email': 'john@test.com'},
        )).called(1);
      });
    });

    group('resetPassword', () {
      test('posts to /v1/password/reset with token and newPassword', () async {
        when(() => mockApi.post<Never>(
          '/v1/password/reset',
          body: {'token': 'reset_token_123', 'newPassword': 'newpass123'},
        )).thenAnswer((_) async => null);

        await repository.resetPassword(token: 'reset_token_123', newPassword: 'newpass123');

        verify(() => mockApi.post<Never>(
          '/v1/password/reset',
          body: {'token': 'reset_token_123', 'newPassword': 'newpass123'},
        )).called(1);
      });
    });

    group('changePassword', () {
      test('POSTs to /v1/users/me/change-password with currentPassword and newPassword', () async {
        when(() => mockApi.post<Never>(
          '/v1/users/me/change-password',
          body: {'currentPassword': 'oldpass', 'newPassword': 'newpass'},
        )).thenAnswer((_) async => null);

        await repository.changePassword(
          currentPassword: 'oldpass',
          newPassword: 'newpass',
        );

        verify(() => mockApi.post<Never>(
          '/v1/users/me/change-password',
          body: {'currentPassword': 'oldpass', 'newPassword': 'newpass'},
        )).called(1);
      });
    });

    group('verifyEmail', () {
      test('posts to /v1/email/verify with token query param', () async {
        when(() => mockApi.post<Never>(
          '/v1/email/verify',
          query: {'token': 'verify_token_123'},
        )).thenAnswer((_) async => null);

        await repository.verifyEmail('verify_token_123');

        verify(() => mockApi.post<Never>(
          '/v1/email/verify',
          query: {'token': 'verify_token_123'},
        )).called(1);
      });
    });

    group('setPin', () {
      test('posts to /v1/pin with pin', () async {
        when(() => mockApi.post<Never>(
          '/v1/pin',
          body: {'pin': '1234'},
        )).thenAnswer((_) async => null);

        await repository.setPin('1234');

        verify(() => mockApi.post<Never>(
          '/v1/pin',
          body: {'pin': '1234'},
        )).called(1);
      });
    });

    group('logout', () {
      test('clears storage', () async {
        when(() => mockStorage.clear()).thenAnswer((_) async {});

        await repository.logout();

        verify(() => mockStorage.clear()).called(1);
      });
    });

    group('restoreSession', () {
      test('returns User when valid userJson stored', () async {
        when(() => mockStorage.userJson).thenAnswer((_) async => '{"id":"1","firstName":"John","lastName":"Doe","email":"john@test.com","phone":"+260971234567","role":"USER"}');

        final result = await repository.restoreSession();

        expect(result, isNotNull);
        expect(result!.id, '1');
        expect(result.firstName, 'John');
        expect(result.email, 'john@test.com');
      });

      test('returns null when no userJson stored', () async {
        when(() => mockStorage.userJson).thenAnswer((_) async => null);

        final result = await repository.restoreSession();

        expect(result, isNull);
      });

      test('returns null and clears storage when userJson is invalid JSON', () async {
        when(() => mockStorage.userJson).thenAnswer((_) async => 'invalid json');
        when(() => mockStorage.clear()).thenAnswer((_) async {});

        final result = await repository.restoreSession();

        expect(result, isNull);
        verify(() => mockStorage.clear()).called(1);
      });
    });

    group('hasPin', () {
      test('returns true when pinSetRaw is "true"', () async {
        when(() => mockStorage.pinSetRaw).thenAnswer((_) async => 'true');

        final result = await repository.hasPin();

        expect(result, isTrue);
      });

      test('returns false when pinSetRaw is "false"', () async {
        when(() => mockStorage.pinSetRaw).thenAnswer((_) async => 'false');

        final result = await repository.hasPin();

        expect(result, isFalse);
      });

      test('returns false when pinSetRaw is null', () async {
        when(() => mockStorage.pinSetRaw).thenAnswer((_) async => null);

        final result = await repository.hasPin();

        expect(result, isFalse);
      });
    });

    group('markPinSet', () {
      test('calls storage.setPinSet(true)', () async {
        when(() => mockStorage.setPinSet(true)).thenAnswer((_) async {});

        await repository.markPinSet();

        verify(() => mockStorage.setPinSet(true)).called(1);
      });
    });
  });
}