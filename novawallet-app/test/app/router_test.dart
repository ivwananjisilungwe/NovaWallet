import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/app/router.dart';
import 'package:novawallet_app/features/auth/data/auth_repository.dart';
import 'package:novawallet_app/features/auth/models/user.dart';
import 'package:novawallet_app/features/auth/providers/auth_provider.dart';

class MockAuthRepository extends Mock implements AuthRepository {}

/// Test double for [AuthNotifier] whose [AuthNotifier.state] is driven
/// directly, so redirect guards can be exercised without touching storage.
class TestAuthNotifier extends AuthNotifier {
  TestAuthNotifier({AuthState state = const AuthState()})
    : super(MockAuthRepository()) {
    _state = state;
  }

  AuthState _state = const AuthState();

  @override
  AuthState get state => _state;

  void setState(AuthState value) {
    _state = value;
    notifyListeners();
  }
}

/// Builds [GoRouterForApp] with a controllable [TestAuthNotifier] and resolves
/// `target` through go_router's redirect logic.
///
/// This drives the internal `RouteInformationParser` directly and never renders
/// feature screens, so the app's network-backed providers stay untouched.
Future<String> resolveLocation(
  WidgetTester tester, {
  required TestAuthNotifier notifier,
  required String target,
}) async {
  final container = ProviderContainer(
    overrides: [authProvider.overrideWith((ref) => notifier)],
  );
  addTearDown(container.dispose);

  await tester.pumpWidget(const MaterialApp(home: Placeholder()));
  final context = tester.element(find.byType(Placeholder));

  final router = container.read(routerProvider).router;
  router.routeInformationProvider.go(target);
  final matchesFuture = router.routeInformationParser
      .parseRouteInformationWithDependencies(
        router.routeInformationProvider.value,
        context,
      );
  await tester.pump();
  final matches = await matchesFuture;

  return matches.uri.toString();
}

void main() {
  const member = User(
    id: '1',
    firstName: 'Jane',
    lastName: 'Member',
    email: 'jane@test.com',
    role: 'USER',
  );

  const admin = User(
    id: '2',
    firstName: 'Sam',
    lastName: 'Admin',
    email: 'sam@test.com',
    role: 'ADMIN',
  );

  group('GoRouterForApp redirect guards', () {
    testWidgets('while initializing, any non-splash target parks on /splash', (
      tester,
    ) async {
      final notifier = TestAuthNotifier(
        state: const AuthState(status: AuthStatus.unknown),
      );

      expect(
        await resolveLocation(tester, notifier: notifier, target: '/wallet'),
        '/splash',
      );
      final splashNotifier = TestAuthNotifier(
        state: const AuthState(status: AuthStatus.unknown),
      );
      expect(
        await resolveLocation(
          tester,
          notifier: splashNotifier,
          target: '/splash',
        ),
        '/splash',
      );
    });

    group('unauthenticated users', () {
      TestAuthNotifier notifier() => TestAuthNotifier(
        state: const AuthState(status: AuthStatus.unauthenticated),
      );

      testWidgets('are redirected to /login from protected routes', (
        tester,
      ) async {
        expect(
          await resolveLocation(
            tester,
            notifier: notifier(),
            target: '/wallet',
          ),
          '/login',
        );
      });

      testWidgets('are redirected to /login from the admin console', (
        tester,
      ) async {
        expect(
          await resolveLocation(tester, notifier: notifier(), target: '/admin'),
          '/login',
        );
      });

      testWidgets('stay on the public flow', (tester) async {
        expect(
          await resolveLocation(tester, notifier: notifier(), target: '/login'),
          '/login',
        );
        expect(
          await resolveLocation(
            tester,
            notifier: notifier(),
            target: '/register',
          ),
          '/register',
        );
      });
    });

    group('authenticated users', () {
      testWidgets('without a PIN are sent to /pin/set first', (tester) async {
        final notifier = TestAuthNotifier(
          state: AuthState(
            status: AuthStatus.authenticated,
            user: member,
            hasPin: false,
          ),
        );

        expect(
          await resolveLocation(tester, notifier: notifier, target: '/wallet'),
          '/pin/set',
        );
      });
    });

    group('authenticated member', () {
      TestAuthNotifier notifier() => TestAuthNotifier(
        state: AuthState(
          status: AuthStatus.authenticated,
          user: member,
          hasPin: true,
        ),
      );

      testWidgets('is kept out of the admin console', (tester) async {
        expect(
          await resolveLocation(tester, notifier: notifier(), target: '/admin'),
          '/wallet',
        );
      });

      testWidgets('is sent back to /wallet from public routes', (tester) async {
        expect(
          await resolveLocation(
            tester,
            notifier: notifier(),
            target: '/splash',
          ),
          '/wallet',
        );
        expect(
          await resolveLocation(tester, notifier: notifier(), target: '/login'),
          '/wallet',
        );
      });

      testWidgets('stays on wallet routes', (tester) async {
        expect(
          await resolveLocation(
            tester,
            notifier: notifier(),
            target: '/wallet',
          ),
          '/wallet',
        );
      });
    });

    group('authenticated admin', () {
      testWidgets('is allowed to open the admin console', (tester) async {
        final notifier = TestAuthNotifier(
          state: AuthState(
            status: AuthStatus.authenticated,
            user: admin,
            hasPin: true,
          ),
        );

        expect(
          await resolveLocation(tester, notifier: notifier, target: '/admin'),
          '/admin',
        );
      });
    });
  });
}
