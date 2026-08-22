import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/core/providers.dart';
import 'package:novawallet_app/core/storage/token_storage.dart';
import 'package:novawallet_app/features/auth/data/auth_repository.dart';
import 'package:novawallet_app/features/auth/data/biometric_service.dart';
import 'package:novawallet_app/features/auth/data/biometric_settings_store.dart';
import 'package:novawallet_app/features/auth/models/user.dart';
import 'package:novawallet_app/features/auth/providers/auth_provider.dart';
import 'package:novawallet_app/features/auth/providers/biometric_provider.dart';
import 'package:novawallet_app/features/auth/screens/biometric_lock_screen.dart';

class MockBiometricService extends Mock implements BiometricService {}

class MockBiometricSettingsStore extends Mock
    implements BiometricSettingsStore {}

class MockAuthRepository extends Mock implements AuthRepository {}

class MockFlutterSecureStorage extends Mock implements FlutterSecureStorage {}

void main() {
  group('BiometricLockScreen', () {
    late MockBiometricService mockService;
    late MockBiometricSettingsStore mockStore;
    late MockAuthRepository mockRepo;
    late MockFlutterSecureStorage mockSecure;
    late AuthNotifier authNotifier;

    const testUser = User(
      id: '1',
      firstName: 'Jane',
      lastName: 'Member',
      email: 'jane@test.com',
      role: 'USER',
    );

    setUp(() {
      mockService = MockBiometricService();
      mockStore = MockBiometricSettingsStore();
      mockRepo = MockAuthRepository();
      mockSecure = MockFlutterSecureStorage();

      when(() => mockRepo.restoreSession()).thenAnswer((_) async => null);
      when(() => mockRepo.hasPin()).thenAnswer((_) async => false);
      when(() => mockRepo.logout()).thenAnswer((_) async {});
      when(
        () => mockSecure.read(key: 'auth_access_token'),
      ).thenAnswer((_) async => 'access-token-123');

      authNotifier = AuthNotifier(mockRepo);
      // NOTE: no manual addTearDown(dispose) — the ProviderScope container
      // created per testWidgets owns these notifiers and disposes them.
    });

    /// Seeds a restored, authenticated session so the lock screen proceeds
    /// to the biometric prompt instead of falling back immediately.
    Future<void> seedAuthenticatedSession() async {
      when(
        () => mockRepo.login(
          email: any(named: 'email'),
          password: any(named: 'password'),
        ),
      ).thenAnswer(
        (_) async => AuthResponse(
          accessToken: 'access-token-123',
          refreshToken: 'refresh-token-456',
          user: testUser,
        ),
      );
      await authNotifier.login(email: 'jane@test.com', password: 'pw');
    }

    Future<BiometricNotifier> readyNotifier({
      bool enabled = true,
      bool supported = true,
      bool canCheck = true,
    }) async {
      when(() => mockStore.load()).thenAnswer((_) async => enabled);
      when(
        () => mockService.isDeviceSupported(),
      ).thenAnswer((_) async => supported);
      when(
        () => mockService.canCheckBiometrics(),
      ).thenAnswer((_) async => canCheck);
      final notifier = BiometricNotifier(mockService, mockStore);
      await notifier.init();
      return notifier;
    }

    Future<void> pumpLock(WidgetTester tester, BiometricNotifier bio) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            tokenStorageProvider.overrideWithValue(
              TokenStorage(storage: mockSecure),
            ),
            authProvider.overrideWith((ref) => authNotifier),
            biometricProvider.overrideWith((ref) => bio),
          ],
          child: const MaterialApp(home: BiometricLockScreen()),
        ),
      );
      // Flush the post-frame callback and the async authenticate chain.
      await tester.pump();
      await tester.pump();
      await tester.pump();
    }

    testWidgets('success path: prompt succeeds -> cycle unlocked, no logout',
        (tester) async {
      await seedAuthenticatedSession();
      final bio = await readyNotifier();
      when(
        () => mockService.authenticate(
          reason: any(named: 'reason'),
        ),
      ).thenAnswer((_) async => true);

      await pumpLock(tester, bio);

      expect(bio.state.sessionUnlocked, isTrue);
      verifyNever(() => mockRepo.logout());
      // No retry UI while unlocked.
      expect(find.text('Try again'), findsNothing);
    });

    testWidgets('failure path: prompt fails -> retry UI; session kept until '
        'the user picks the password fallback', (tester) async {
      await seedAuthenticatedSession();
      final bio = await readyNotifier();
      when(
        () => mockService.authenticate(
          reason: any(named: 'reason'),
        ),
      ).thenAnswer((_) async => false);

      await pumpLock(tester, bio);

      // A failed prompt must not silently clear the session: the user gets
      // a retry affordance instead of a dead end.
      verifyNever(() => mockRepo.logout());
      expect(authNotifier.state.isAuthenticated, isTrue);
      expect(find.text('Try again'), findsOneWidget);
      expect(find.text('Use password instead'), findsOneWidget);

      // Explicit password fallback: clears the local session so the
      // router funnels to /login.
      await tester.tap(find.text('Use password instead'));
      await tester.pump();
      await tester.pump();

      verify(() => mockRepo.logout()).called(1);
      expect(authNotifier.state.isAuthenticated, isFalse);
    });

    testWidgets('disabled path: flag OFF -> immediate fallback, no prompt',
        (tester) async {
      final bio = await readyNotifier(enabled: false);

      await pumpLock(tester, bio);

      verifyNever(
        () => mockService.authenticate(
          reason: any(named: 'reason'),
        ),
      );
      verify(() => mockRepo.logout()).called(1);
    });

    testWidgets('unavailable path: no enrolled biometrics -> fallback',
        (tester) async {
      final bio = await readyNotifier(canCheck: false);

      await pumpLock(tester, bio);

      verifyNever(
        () => mockService.authenticate(
          reason: any(named: 'reason'),
        ),
      );
      verify(() => mockRepo.logout()).called(1);
    });

    testWidgets('no saved token -> immediate fallback without prompting',
        (tester) async {
      final bio = await readyNotifier();
      when(
        () => mockSecure.read(key: 'auth_access_token'),
      ).thenAnswer((_) async => null);

      await pumpLock(tester, bio);

      verifyNever(
        () => mockService.authenticate(
          reason: any(named: 'reason'),
        ),
      );
      verify(() => mockRepo.logout()).called(1);
    });
  });
}
