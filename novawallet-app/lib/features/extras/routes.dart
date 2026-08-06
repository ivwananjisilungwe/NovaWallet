import 'package:go_router/go_router.dart';

import '../transaction/screens/insufficient_balance_screen.dart';
import '../transaction/screens/success_screen.dart';
import 'screens/notifications_screen.dart';
import 'screens/statements_screen.dart';

/// Route contributions for **extras / edge states** (in the shell).
List<RouteBase> extrasRoutes = [
  GoRoute(
    path: '/notifications',
    builder: (context, state) => const NotificationsScreen(),
  ),
  GoRoute(
    path: '/statements',
    builder: (context, state) => const StatementsScreen(),
  ),
  GoRoute(
    path: '/success',
    builder: (context, state) => SuccessScreen(
      title: state.uri.queryParameters['title'] ?? 'Success',
      amount: state.uri.queryParameters['amount'],
      reference: state.uri.queryParameters['ref'],
    ),
  ),
  GoRoute(
    path: '/insufficient',
    builder: (context, state) => const InsufficientBalanceScreen(),
  ),
];