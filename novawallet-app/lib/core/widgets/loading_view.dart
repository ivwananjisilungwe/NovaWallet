import 'package:flutter/material.dart';

import '../theme/nova_colors.dart';

/// Small centered spinner used inside screens while async data loads.
class LoadingView extends StatelessWidget {
  const LoadingView({super.key, this.message});

  final String? message;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const CircularProgressIndicator(),
          if (message != null) ...[
            const SizedBox(height: 16),
            Text(
              message!,
              style: const TextStyle(color: NovaColors.onSurfaceVariant),
            ),
          ],
        ],
      ),
    );
  }
}
