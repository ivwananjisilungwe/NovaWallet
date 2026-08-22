import 'package:flutter/material.dart';

import '../../../core/theme/design_tokens.dart';

/// Top app bar with greeting, notification bell, and profile avatar.
class DashboardTopBar extends StatelessWidget {
  const DashboardTopBar({
    super.key,
    required this.name,
    required this.unreadCount,
    required this.onNotifications,
    required this.onProfile,
  });

  final String name;
  final int unreadCount;
  final VoidCallback onNotifications;
  final VoidCallback onProfile;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Hi, $name',
              style: Theme.of(
                context,
              ).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w600),
            ),
            Text(
              'Good to see you!',
              style: Theme.of(
                context,
              ).textTheme.bodyMedium?.copyWith(color: scheme.onSurfaceVariant),
            ),
          ],
        ),
        Row(
          children: [
            // Notification bell
            Stack(
              clipBehavior: Clip.none,
              children: [
                IconButton(
                  onPressed: onNotifications,
                  icon: const Icon(Icons.notifications_outlined),
                  style: IconButton.styleFrom(
                    backgroundColor: scheme.surfaceContainerLow,
                    foregroundColor: scheme.onSurface,
                  ),
                ),
                if (unreadCount > 0)
                  Positioned(
                    right: 4,
                    top: 4,
                    child: Container(
                      padding: const EdgeInsets.all(4),
                      decoration: BoxDecoration(
                        color: scheme.error,
                        shape: BoxShape.circle,
                      ),
                      constraints: const BoxConstraints(
                        minWidth: 16,
                        minHeight: 16,
                      ),
                      child: Text(
                        unreadCount > 9 ? '9+' : '$unreadCount',
                        style: TextStyle(
                          color: scheme.onError,
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ),
              ],
            ),
            const SizedBox(width: 12),
            // Profile avatar
            GestureDetector(
              onTap: onProfile,
              child: Container(
                width: 40,
                height: 40,
                decoration: const BoxDecoration(
                  gradient: LinearGradient(
                    colors: [
                      SemanticColors.brandPrimary,
                      SemanticColors.moneyIn,
                    ],
                  ),
                  shape: BoxShape.circle,
                ),
                child: Center(
                  child: Text(
                    name.isNotEmpty ? name[0].toUpperCase() : '?',
                    style: const TextStyle(
                      color: SemanticColors.onBrandPrimary,
                      fontWeight: FontWeight.bold,
                      fontSize: 16,
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }
}
