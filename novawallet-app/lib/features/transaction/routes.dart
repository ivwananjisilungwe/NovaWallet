import 'package:go_router/go_router.dart';

import '../../core/transitions/page_transitions.dart';
import '../wallet/screens/cards_screen.dart';
import '../wallet/screens/dashboard_screen.dart';
import '../wallet/screens/deposit_screen.dart';
import '../wallet/screens/send_screen.dart';
import '../wallet/screens/withdraw_screen.dart';
import 'screens/fee_estimate_screen.dart';
import 'screens/transaction_detail_screen.dart';
import 'screens/transaction_history_screen.dart';

/// Route contributions for the **wallet + transaction** feature (in the shell).
List<RouteBase> transactionRoutes = [
  GoRoute(
    path: '/wallet',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.sharedAxisHorizontal,
      key: state.pageKey,
      child: const DashboardScreen(),
    ),
  ),
  GoRoute(
    path: '/send',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.sharedAxisHorizontal,
      key: state.pageKey,
      child: const SendScreen(),
    ),
  ),
  GoRoute(
    path: '/cards',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.sharedAxisHorizontal,
      key: state.pageKey,
      child: const CardsScreen(),
    ),
  ),
  GoRoute(
    path: '/deposit',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.slideFromRight,
      key: state.pageKey,
      child: const DepositScreen(),
    ),
  ),
  GoRoute(
    path: '/withdraw',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.slideFromRight,
      key: state.pageKey,
      child: const WithdrawScreen(),
    ),
  ),
  GoRoute(
    path: '/transactions',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.slideFromRight,
      key: state.pageKey,
      child: const TransactionHistoryScreen(),
    ),
  ),
  GoRoute(
    path: '/transactions/:reference',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.slideFromRight,
      key: state.pageKey,
      child: TransactionDetailScreen(reference: state.pathParameters['reference']!),
    ),
  ),
  GoRoute(
    path: '/fees/estimate',
    pageBuilder: (context, state) => AppPageRoute(
      type: TransitionType.slideUp,
      key: state.pageKey,
      child: const FeeEstimateScreen(),
    ),
  ),
];