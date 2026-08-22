import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/core/widgets/pill_button.dart';
import 'package:novawallet_app/core/widgets/widgets.dart';
import 'package:novawallet_app/features/auth/data/auth_repository.dart';
import 'package:novawallet_app/features/auth/providers/auth_provider.dart';
import 'package:novawallet_app/features/auth/screens/login_screen.dart';
import 'package:novawallet_app/features/auth/screens/onboarding_screen.dart';

class _MockAuthRepository extends Mock implements AuthRepository {}

void main() {
  group('OnboardingScreen', () {
    testWidgets('renders brand hero, feature pills and CTAs', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(home: OnboardingScreen()),
      );

      expect(find.text('Your money, simplified.'), findsOneWidget);
      expect(
        find.textContaining('Send, receive, deposit and withdraw'),
        findsOneWidget,
      );

      expect(find.text('Instant transfers'), findsOneWidget);
      expect(find.text('Bank-grade security'), findsOneWidget);
      expect(find.text('Virtual cards'), findsOneWidget);

      expect(find.widgetWithText(PillButton, 'Get started'), findsOneWidget);
      expect(
        find.widgetWithText(PillButton, 'I already have an account'),
        findsOneWidget,
      );

      expect(find.textContaining('Terms'), findsOneWidget);
      expect(find.textContaining('Privacy Policy'), findsOneWidget);
    });

    testWidgets('Get started navigates to /register', (tester) async {
      final router = GoRouter(
        routes: [
          GoRoute(
            path: '/',
            builder: (_, _) => const OnboardingScreen(),
          ),
          GoRoute(
            path: '/register',
            builder: (_, _) => const Scaffold(body: Text('register')),
          ),
        ],
      );

      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      // CTA sits below the fold on the default 800x600 test viewport; scroll
      // it into view the way a real user would before tapping.
      final getStarted = find.widgetWithText(PillButton, 'Get started');
      await tester.ensureVisible(getStarted);
      await tester.pumpAndSettle();
      await tester.tap(getStarted);
      await tester.pumpAndSettle();

      expect(find.text('register'), findsOneWidget);
    });

    testWidgets('I already have an account navigates to /login', (
      tester,
    ) async {
      final router = GoRouter(
        routes: [
          GoRoute(
            path: '/',
            builder: (_, _) => const OnboardingScreen(),
          ),
          GoRoute(
            path: '/login',
            builder: (_, _) => const Scaffold(body: Text('login')),
          ),
        ],
      );

      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      final existingAccount = find.widgetWithText(
        PillButton,
        'I already have an account',
      );
      await tester.ensureVisible(existingAccount);
      await tester.pumpAndSettle();
      await tester.tap(existingAccount);
      await tester.pumpAndSettle();

      expect(find.text('login'), findsOneWidget);
    });
  });

  group('LoginScreen', () {
    late _MockAuthRepository repository;
    late AuthNotifier notifier;

    setUp(() {
      repository = _MockAuthRepository();
      notifier = AuthNotifier(repository);
    });

    Future<void> pumpScreen(WidgetTester tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [authProvider.overrideWith((ref) => notifier)],
          child: const MaterialApp(home: LoginScreen()),
        ),
      );
    }

    testWidgets('renders brand, headings, inputs and CTAs', (tester) async {
      await pumpScreen(tester);

      expect(find.text('NovaWallet'), findsOneWidget);
      expect(find.text('Welcome back'), findsOneWidget);
      expect(find.text('Log in to your NovaWallet account'), findsOneWidget);
      expect(find.byType(TextField), findsNWidgets(2));
      expect(find.byType(OrDivider), findsOneWidget);
      expect(find.byType(GoogleSignInButton), findsOneWidget);
      expect(find.text('Forgot password?'), findsOneWidget);
      expect(find.widgetWithText(PillButton, 'Log in'), findsOneWidget);
      expect(
        find.widgetWithText(PillButton, 'Log in'),
        findsOneWidget,
        reason: 'single primary CTA; Google button handles alt sign-in',
      );
      expect(find.text("Don't have an account?"), findsOneWidget);
      expect(find.text('Sign up'), findsOneWidget);
    });

    testWidgets('password visibility toggle works', (tester) async {
      await pumpScreen(tester);

      final passwordField = tester.widget<TextField>(
        find.byType(TextField).at(1),
      );
      expect(passwordField.obscureText, isTrue);

      await tester.tap(find.byIcon(Icons.visibility_off));
      await tester.pump();

      final toggledField = tester.widget<TextField>(
        find.byType(TextField).at(1),
      );
      expect(toggledField.obscureText, isFalse);
    });

    testWidgets('empty fields show snackbar and do not call login', (
      tester,
    ) async {
      await pumpScreen(tester);

      await tester.tap(find.widgetWithText(PillButton, 'Log in'));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      expect(find.text('Enter your email and password.'), findsOneWidget);
      verifyNever(() => repository.login(
            email: any(named: 'email'),
            password: any(named: 'password'),
          ));
    });

    testWidgets('renders within small screen bounds without overflow', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(375, 667);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);

      await pumpScreen(tester);

      expect(find.text('Welcome back'), findsOneWidget);
      expect(tester.takeException(), isNull);
    });
  });
}
