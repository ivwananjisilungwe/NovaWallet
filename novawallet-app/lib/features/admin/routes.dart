import 'package:go_router/go_router.dart';

import 'screens/admin_dashboard_screen.dart';
import 'screens/fee_config_screen.dart';

/// Route contributions for the **admin console** feature.
/// Registered inside the 4-tab shell (ADMIN users only).
List<RouteBase> adminRoutes = [
  GoRoute(path: '/admin', builder: (context, state) => const AdminDashboardScreen()),
  GoRoute(path: '/admin/fees', builder: (context, state) => const FeeConfigScreen()),
];