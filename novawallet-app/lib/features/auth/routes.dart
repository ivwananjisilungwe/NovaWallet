import 'package:go_router/go_router.dart';

import 'screens/change_password_screen.dart';
import 'screens/forgot_password_screen.dart';
import 'screens/login_screen.dart';
import 'screens/onboarding_screen.dart';
import 'screens/register_screen.dart';
import 'screens/reset_password_screen.dart';
import 'screens/set_pin_screen.dart';
import 'screens/splash_screen.dart';
import 'screens/verify_email_screen.dart';

/// Route contributions for the **auth** feature.
/// Registered at the ROOT (full-screen, outside the tab shell).
List<RouteBase> authRoutes = [
  GoRoute(path: '/splash', builder: (context, state) => const SplashScreen()),
  GoRoute(path: '/onboarding', builder: (context, state) => const OnboardingScreen()),
  GoRoute(path: '/login', builder: (context, state) => const LoginScreen()),
  GoRoute(path: '/register', builder: (context, state) => const RegisterScreen()),
  GoRoute(path: '/forgot-password', builder: (context, state) => const ForgotPasswordScreen()),
  GoRoute(
    path: '/reset-password',
    builder: (context, state) =>
        ResetPasswordScreen(token: state.uri.queryParameters['token']),
  ),
  GoRoute(
    path: '/verify-email',
    builder: (context, state) =>
        VerifyEmailScreen(token: state.uri.queryParameters['token']),
  ),
  GoRoute(path: '/pin/set', builder: (context, state) => const SetPinScreen()),
  GoRoute(path: '/change-password', builder: (context, state) => const ChangePasswordScreen()),
];
