import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/core/network/api_exception.dart';
import 'package:novawallet_app/features/auth/data/auth_repository.dart';
import 'package:novawallet_app/features/auth/models/user.dart';
import 'package:novawallet_app/features/auth/providers/auth_provider.dart';

class MockAuthRepository extends Mock implements AuthRepository {}

void main() {
  group('AuthNotifier', () {
    late MockAuthRepository mockRepo;
    late AuthNotifier notifier;

    setUp(() {
      mockRepo = MockAuthRepository();
      notifier = AuthNotifier(mockRepo);
      addTearDown(notifier.dispose);
    });

    final testUser = User(
      id: '1',
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@test.com',
      role: 'USER',
    );

    final testAuth = AuthResponse(
      accessToken: 'access123',
      refreshToken: 'refresh456',
      user: testUser,
    );

    test('initial state is unknown and unauthenticated', () {
      expect(notifier.state.status, AuthStatus.unknown);
      expect(notifier.state.isAuthenticated, isFalse);
      expect(notifier.state.isInitializing, isTrue);
      expect(notifier.state.user, isNull);
      expect(notifier.state.hasPin, isFalse);
    });

    test('login success sets authenticated state with user', () async {
      when(
        () => mockRepo.login(email: 'john@test.com', password: 'pass123'),
      ).thenAnswer((_) async => testAuth);

      final result = await notifier.login(
        email: 'john@test.com',
        password: 'pass123',
      );

      expect(result, testAuth);
      expect(notifier.state.status, AuthStatus.authenticated);
      expect(notifier.state.isAuthenticated, isTrue);
      expect(notifier.state.user, testUser);
      verify(
        () => mockRepo.login(email: 'john@test.com', password: 'pass123'),
      ).called(1);
    });

    test('login failure leaves state unchanged and propagates error', () async {
      when(
        () => mockRepo.login(email: 'john@test.com', password: 'pass123'),
      ).thenAnswer((_) async => testAuth);
      await notifier.login(email: 'john@test.com', password: 'pass123');

      when(
        () => mockRepo.login(email: 'john@test.com', password: 'wrong'),
      ).thenThrow(const ApiException(message: 'Login failed.'));

      await expectLater(
        notifier.login(email: 'john@test.com', password: 'wrong'),
        throwsA(isA<ApiException>()),
      );

      expect(notifier.state.isAuthenticated, isTrue);
      expect(notifier.state.user, testUser);
    });

    test('logout resets state and clears pin flag', () async {
      when(
        () => mockRepo.login(email: 'john@test.com', password: 'pass123'),
      ).thenAnswer((_) async => testAuth);
      await notifier.login(email: 'john@test.com', password: 'pass123');

      when(() => mockRepo.logout()).thenAnswer((_) async {});

      await notifier.logout();

      expect(notifier.state.status, AuthStatus.unauthenticated);
      expect(notifier.state.isAuthenticated, isFalse);
      expect(notifier.state.user, isNull);
      expect(notifier.state.hasPin, isFalse);
      verify(() => mockRepo.logout()).called(1);
    });

    test('restore with valid session is authenticated with pin flag', () async {
      when(() => mockRepo.restoreSession()).thenAnswer((_) async => testUser);
      when(() => mockRepo.hasPin()).thenAnswer((_) async => true);

      await notifier.restore();

      expect(notifier.state.status, AuthStatus.authenticated);
      expect(notifier.state.isAuthenticated, isTrue);
      expect(notifier.state.user, testUser);
      expect(notifier.state.hasPin, isTrue);
      verify(() => mockRepo.restoreSession()).called(1);
      verify(() => mockRepo.hasPin()).called(1);
    });

    test('restore with no session is unauthenticated', () async {
      when(() => mockRepo.restoreSession()).thenAnswer((_) async => null);
      when(() => mockRepo.hasPin()).thenAnswer((_) async => false);

      await notifier.restore();

      expect(notifier.state.status, AuthStatus.unauthenticated);
      expect(notifier.state.isAuthenticated, isFalse);
      expect(notifier.state.user, isNull);
      expect(notifier.state.hasPin, isFalse);
    });

    test('setPin marks hasPin true and preserves user', () async {
      when(
        () => mockRepo.login(email: 'john@test.com', password: 'pass123'),
      ).thenAnswer((_) async => testAuth);
      await notifier.login(email: 'john@test.com', password: 'pass123');

      when(() => mockRepo.setPin(any())).thenAnswer((_) async {});
      when(() => mockRepo.markPinSet()).thenAnswer((_) async {});

      await notifier.setPin('1234');

      expect(notifier.state.hasPin, isTrue);
      expect(notifier.state.isAuthenticated, isTrue);
      expect(notifier.state.user, testUser);
      verify(() => mockRepo.setPin('1234')).called(1);
      verify(() => mockRepo.markPinSet()).called(1);
    });

    test('changePassword delegates to repository', () async {
      when(
        () => mockRepo.changePassword(
          currentPassword: 'oldpass',
          newPassword: 'newpass',
        ),
      ).thenAnswer((_) async {});

      await notifier.changePassword(
        currentPassword: 'oldpass',
        newPassword: 'newpass',
      );

      verify(
        () => mockRepo.changePassword(
          currentPassword: 'oldpass',
          newPassword: 'newpass',
        ),
      ).called(1);
    });
  });
}
