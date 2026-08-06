import 'package:go_router/go_router.dart';

import '../profile/screens/edit_profile_screen.dart';
import '../profile/screens/profile_screen.dart';
import '../profile/screens/security_screen.dart';
import 'screens/kyc_status_screen.dart';
import 'screens/kyc_upload_screen.dart';

/// Route contributions for the **profile + KYC** feature (in the shell).
List<RouteBase> kycRoutes = [
  GoRoute(path: '/profile', builder: (context, state) => const ProfileScreen()),
  GoRoute(path: '/profile/edit', builder: (context, state) => const EditProfileScreen()),
  GoRoute(path: '/security', builder: (context, state) => const SecurityScreen()),
  GoRoute(path: '/kyc/status', builder: (context, state) => const KycStatusScreen()),
  GoRoute(path: '/kyc/upload', builder: (context, state) => const KycUploadScreen()),
];