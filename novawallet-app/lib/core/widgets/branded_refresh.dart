import 'package:flutter/material.dart';

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
    final scheme = Theme.of(context).colorScheme;
    return RefreshIndicator(
      onRefresh: onRefresh,
      color: scheme.primary,
      backgroundColor: scheme.surfaceContainerLowest,
      displacement: 40,
      edgeOffset: 8,
      strokeWidth: 2.5,
      child: child,
    );
  }
}
