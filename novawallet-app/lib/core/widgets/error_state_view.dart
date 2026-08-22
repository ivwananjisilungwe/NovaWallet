import 'package:flutter/material.dart';

import '../theme/nova_colors.dart';
import 'pill_button.dart';

/// Full-screen centered message used by every feature's error state.
class ErrorStateView extends StatelessWidget {
  const ErrorStateView({
    super.key,
    required this.message,
    this.onRetry,
    this.title = 'Something went wrong',
  });

  final String title;
  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 56, color: NovaColors.error),
            const SizedBox(height: 16),
            Text(
              title,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: NovaColors.onSurface,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(color: NovaColors.onSurfaceVariant),
            ),
            if (onRetry != null) ...[
              const SizedBox(height: 20),
              PillButton(
                label: 'Try again',
                expanded: false,
                onPressed: onRetry,
              ),
            ],
          ],
        ),
      ),
    );
  }
}
