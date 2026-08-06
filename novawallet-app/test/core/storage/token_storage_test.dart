import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import 'package:novawallet_app/core/storage/token_storage.dart';

class MockFlutterSecureStorage extends Mock implements FlutterSecureStorage {}

void main() {
  group('TokenStorage', () {
    late MockFlutterSecureStorage mockStorage;
    late TokenStorage tokenStorage;

    setUp(() {
      mockStorage = MockFlutterSecureStorage();
      tokenStorage = TokenStorage(storage: mockStorage);
    });

    group('saveSession', () {
      test('persists accessToken, refreshToken, and userJson', () async {
        when(
          () => mockStorage.write(key: 'auth_access_token', value: 'access123'),
        ).thenAnswer((_) async {});
        when(
          () =>
              mockStorage.write(key: 'auth_refresh_token', value: 'refresh456'),
        ).thenAnswer((_) async {});
        when(
          () => mockStorage.write(key: 'auth_user_json', value: '{"id":"1"}'),
        ).thenAnswer((_) async {});

        await tokenStorage.saveSession(
          accessToken: 'access123',
          refreshToken: 'refresh456',
          userJson: '{"id":"1"}',
        );

        verify(
          () => mockStorage.write(key: 'auth_access_token', value: 'access123'),
        ).called(1);
        verify(
          () =>
              mockStorage.write(key: 'auth_refresh_token', value: 'refresh456'),
        ).called(1);
        verify(
          () => mockStorage.write(key: 'auth_user_json', value: '{"id":"1"}'),
        ).called(1);
      });

      test(
        'persists accessToken and refreshToken without userJson when null',
        () async {
          when(
            () =>
                mockStorage.write(key: 'auth_access_token', value: 'access123'),
          ).thenAnswer((_) async {});
          when(
            () => mockStorage.write(
              key: 'auth_refresh_token',
              value: 'refresh456',
            ),
          ).thenAnswer((_) async {});

          await tokenStorage.saveSession(
            accessToken: 'access123',
            refreshToken: 'refresh456',
            userJson: null,
          );

          verify(
            () =>
                mockStorage.write(key: 'auth_access_token', value: 'access123'),
          ).called(1);
          verify(
            () => mockStorage.write(
              key: 'auth_refresh_token',
              value: 'refresh456',
            ),
          ).called(1);
          verifyNever(
            () => mockStorage.write(
              key: 'auth_user_json',
              value: any(named: 'value'),
            ),
          );
        },
      );
    });

    group('saveUser', () {
      test('writes userJson to storage', () async {
        when(
          () => mockStorage.write(key: 'auth_user_json', value: '{"id":"1"}'),
        ).thenAnswer((_) async {});

        await tokenStorage.saveUser('{"id":"1"}');

        verify(
          () => mockStorage.write(key: 'auth_user_json', value: '{"id":"1"}'),
        ).called(1);
      });
    });

    group('setPinSet', () {
      test('writes true as "true" string', () async {
        when(
          () => mockStorage.write(key: 'auth_pin_set', value: 'true'),
        ).thenAnswer((_) async {});

        await tokenStorage.setPinSet(true);

        verify(
          () => mockStorage.write(key: 'auth_pin_set', value: 'true'),
        ).called(1);
      });

      test('writes false as "false" string', () async {
        when(
          () => mockStorage.write(key: 'auth_pin_set', value: 'false'),
        ).thenAnswer((_) async {});

        await tokenStorage.setPinSet(false);

        verify(
          () => mockStorage.write(key: 'auth_pin_set', value: 'false'),
        ).called(1);
      });
    });

    group('getters', () {
      test('accessToken returns stored value', () async {
        when(
          () => mockStorage.read(key: 'auth_access_token'),
        ).thenAnswer((_) async => 'access123');

        final result = await tokenStorage.accessToken;

        expect(result, 'access123');
      });

      test('refreshToken returns stored value', () async {
        when(
          () => mockStorage.read(key: 'auth_refresh_token'),
        ).thenAnswer((_) async => 'refresh456');

        final result = await tokenStorage.refreshToken;

        expect(result, 'refresh456');
      });

      test('userJson returns stored value', () async {
        when(
          () => mockStorage.read(key: 'auth_user_json'),
        ).thenAnswer((_) async => '{"id":"1"}');

        final result = await tokenStorage.userJson;

        expect(result, '{"id":"1"}');
      });

      test('pinSetRaw returns stored value', () async {
        when(
          () => mockStorage.read(key: 'auth_pin_set'),
        ).thenAnswer((_) async => 'true');

        final result = await tokenStorage.pinSetRaw;

        expect(result, 'true');
      });
    });

    group('clear', () {
      test(
        'removes accessToken, refreshToken, userJson, AND pin flag key',
        () async {
          when(
            () => mockStorage.delete(key: 'auth_access_token'),
          ).thenAnswer((_) async {});
          when(
            () => mockStorage.delete(key: 'auth_refresh_token'),
          ).thenAnswer((_) async {});
          when(
            () => mockStorage.delete(key: 'auth_user_json'),
          ).thenAnswer((_) async {});
          when(
            () => mockStorage.delete(key: 'auth_pin_set'),
          ).thenAnswer((_) async {});

          await tokenStorage.clear();

          verify(() => mockStorage.delete(key: 'auth_access_token')).called(1);
          verify(() => mockStorage.delete(key: 'auth_refresh_token')).called(1);
          verify(() => mockStorage.delete(key: 'auth_user_json')).called(1);
          verify(() => mockStorage.delete(key: 'auth_pin_set')).called(1);
        },
      );
    });
  });
}
