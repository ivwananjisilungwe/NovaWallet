import 'package:go_router/go_router.dart';

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
  GoRoute(path: '/wallet', builder: (context, state) => const DashboardScreen()),
  GoRoute(path: '/send', builder: (context, state) => const SendScreen()),
  GoRoute(path: '/cards', builder: (context, state) => const CardsScreen()),
  GoRoute(path: '/deposit', builder: (context, state) => const DepositScreen()),
  GoRoute(path: '/withdraw', builder: (context, state) => const WithdrawScreen()),
  GoRoute(path: '/transactions', builder: (context, state) => const TransactionHistoryScreen()),
  GoRoute(
    path: '/transactions/:reference',
    builder: (context, state) =>
        TransactionDetailScreen(reference: state.pathParameters['reference']!),
  ),
  GoRoute(path: '/fees/estimate', builder: (context, state) => const FeeEstimateScreen()),
];