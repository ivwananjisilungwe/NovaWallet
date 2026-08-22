import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:local_auth/local_auth.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/features/auth/data/biometric_service.dart';

class MockLocalAuthentication extends Mock implements LocalAuthentication {}

void main() {
  group('BiometricService', () {
    late MockLocalAuthentication mockAuth;
    late BiometricService service;

    setUpAll(() {
      registerFallbackValue(
        const AuthenticationOptions(biometricOnly: true, stickyAuth: true),
      );
    });

    setUp(() {
      mockAuth = MockLocalAuthentication();
      service = BiometricService(auth: mockAuth);
    });

    group('canCheckBiometrics', () {
      test('returns true when the platform reports enrolled biometrics',
          () async {
        when(() => mockAuth.canCheckBiometrics).thenAnswer((_) async => true);

        expect(await service.canCheckBiometrics(), isTrue);
      });

      test('returns false when the platform reports none', () async {
        when(
          () => mockAuth.canCheckBiometrics,
        ).thenAnswer((_) async => false);

        expect(await service.canCheckBiometrics(), isFalse);
      });

      test('degrades to false when the platform channel throws', () async {
        when(
          () => mockAuth.canCheckBiometrics,
        ).thenThrow(MissingPluginException());

        expect(await service.canCheckBiometrics(), isFalse);
      });
    });

    group('isDeviceSupported', () {
      test('returns true when hardware supports biometrics', () async {
        when(
          () => mockAuth.isDeviceSupported(),
        ).thenAnswer((_) async => true);

        expect(await service.isDeviceSupported(), isTrue);
      });

      test('degrades to false when the platform channel throws', () async {
        when(
          () => mockAuth.isDeviceSupported(),
        ).thenThrow(MissingPluginException());

        expect(await service.isDeviceSupported(), isFalse);
      });
    });

    group('authenticate', () {
      test('returns true and passes the reason on success', () async {
        when(
          () => mockAuth.authenticate(
            localizedReason: any(named: 'localizedReason'),
            options: any(named: 'options'),
          ),
        ).thenAnswer((_) async => true);

        final result = await service.authenticate(reason: 'Unlock NovaWallet');

        expect(result, isTrue);
        verify(
          () => mockAuth.authenticate(
            localizedReason: 'Unlock NovaWallet',
            options: any(named: 'options'),
          ),
        ).called(1);
      });

      test('returns false when the user cancels or fails', () async {
        when(
          () => mockAuth.authenticate(
            localizedReason: any(named: 'localizedReason'),
            options: any(named: 'options'),
          ),
        ).thenAnswer((_) async => false);

        expect(await service.authenticate(reason: 'Unlock'), isFalse);
      });

      test('degrades to false when the platform channel throws', () async {
        when(
          () => mockAuth.authenticate(
            localizedReason: any(named: 'localizedReason'),
            options: any(named: 'options'),
          ),
        ).thenThrow(MissingPluginException());

        expect(await service.authenticate(reason: 'Unlock'), isFalse);
      });
    });
  });
}
