import 'package:go_router/go_router.dart';

import '../../core/transitions/page_transitions.dart';
import 'screens/change_password_screen.dart';
import 'screens/change_pin_screen.dart';
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
  GoRoute(
    path: '/splash',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.fadeIn,
      key: state.pageKey,
      child: const SplashScreen(),
    ),
  ),
  GoRoute(
    path: '/onboarding',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.fadeIn,
      key: state.pageKey,
      child: const OnboardingScreen(),
    ),
  ),
  GoRoute(
    path: '/login',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.slideUp,
      key: state.pageKey,
      child: const LoginScreen(),
    ),
  ),
  GoRoute(
    path: '/register',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.slideUp,
      key: state.pageKey,
      child: const RegisterScreen(),
    ),
  ),
  GoRoute(
    path: '/forgot-password',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.slideUp,
      key: state.pageKey,
      child: const ForgotPasswordScreen(),
    ),
  ),
  GoRoute(
    path: '/reset-password',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.slideUp,
      key: state.pageKey,
      child: const ResetPasswordScreen(),
    ),
  ),
  GoRoute(
    path: '/reset-password/confirm',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.slideUp,
      key: state.pageKey,
      child: ResetPasswordScreen(token: state.uri.queryParameters['token']),
    ),
  ),
  GoRoute(
    path: '/verify-email',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.slideUp,
      key: state.pageKey,
      child: VerifyEmailScreen(token: state.uri.queryParameters['token']),
    ),
  ),
  GoRoute(
    path: '/pin/set',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.slideUp,
      key: state.pageKey,
      child: const SetPinScreen(),
    ),
  ),
  GoRoute(
    path: '/pin/change',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.slideUp,
      key: state.pageKey,
      child: const ChangePinScreen(),
    ),
  ),
  GoRoute(
    path: '/change-password',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.slideUp,
      key: state.pageKey,
      child: const ChangePasswordScreen(),
    ),
  ),
];
