import 'package:flutter/material.dart';

import '../theme/design_tokens.dart';

/// Offline / connectivity banner shown when the app detects a network failure.
///
/// This is a lightweight Phase-C affordance that does not require
/// `connectivity_plus` — it is driven by provider error states (e.g.
/// `walletProvider.hasError`). The banner is dismissible and does not block
/// interaction with cached content below it.
class OfflineBanner extends StatelessWidget {
  const OfflineBanner({
    super.key,
    this.message = 'You appear to be offline — showing cached data.',
    this.onRetry,
    this.onDismiss,
  });

  final String message;
  final VoidCallback? onRetry;
  final VoidCallback? onDismiss;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 8, 16, 0),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        // Amber passes ≥ 4.5:1 on dark surfaces as-is, so it stays the same
        // semantic token in both themes.
        color: SemanticColors.warning.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: SemanticColors.warning.withValues(alpha: 0.3),
        ),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.wifi_off_rounded,
            size: 18,
            color: SemanticColors.warning,
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              message,
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: SemanticColors.onWarning,
              ),
            ),
          ),
          if (onRetry != null) ...[
            const SizedBox(width: 8),
            TextButton(
              onPressed: onRetry,
              style: TextButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 8),
                minimumSize: Size.zero,
                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
              child: const Text('Retry', style: TextStyle(fontSize: 12)),
            ),
          ],
          if (onDismiss != null)
            IconButton(
              icon: const Icon(Icons.close, size: 16),
              onPressed: onDismiss,
              padding: EdgeInsets.zero,
              constraints: const BoxConstraints(),
              color: scheme.onSurfaceVariant,
            ),
        ],
      ),
    );
  }
}
