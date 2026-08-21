import 'package:flutter/material.dart';

import '../theme/nova_colors.dart';

/// Branded pull-to-refresh with NovaWallet colors and consistent displacement.
class BrandedRefreshIndicator extends StatelessWidget {
  const BrandedRefreshIndicator({
    super.key,
    required this.onRefresh,
    required this.child,
  });

  final Future<void> Function() onRefresh;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: onRefresh,
      color: NovaColors.primary,
      backgroundColor: NovaColors.surfaceContainerLowest,
      displacement: 40,
      edgeOffset: 8,
      strokeWidth: 2.5,
      child: child,
    );
  }
}
