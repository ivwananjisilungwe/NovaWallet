import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/widgets.dart';
import '../../notification/models/app_notification.dart';
import '../../notification/providers/notification_provider.dart';

/// Notification inbox (per design 26). Driven by the backend notification
/// service via [notificationsProvider].
class NotificationsScreen extends ConsumerStatefulWidget {
  const NotificationsScreen({super.key});

  @override
  ConsumerState<NotificationsScreen> createState() =>
      _NotificationsScreenState();
}

class _NotificationsScreenState extends ConsumerState<NotificationsScreen> {
  bool _markingAll = false;

  Future<void> _markAllAsRead() async {
    if (_markingAll) return;
    setState(() => _markingAll = true);
    try {
      await markAllNotificationsAsRead(ref);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('All notifications marked as read')),
      );
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.displayMessage)),
      );
    } finally {
      if (mounted) setState(() => _markingAll = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final notificationsAsync = ref.watch(notificationsProvider);
    final page = notificationsAsync.valueOrNull;
    final hasUnread = page?.items.any((n) => !n.read) ?? false;

    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(
        title: const Text('Notifications'),
        actions: [
          if (hasUnread)
            _markingAll
                ? const Padding(
                    padding: EdgeInsets.all(16),
                    child: SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
                  )
                : TextButton(
                    onPressed: _markAllAsRead,
                    child: const Text('Mark all read'),
                  ),
        ],
      ),
      body: SafeArea(
        child: notificationsAsync.when(
          loading: () => const LoadingView(message: 'Loading notifications…'),
          error: (e, _) => ErrorStateView(
            message: e is ApiException ? e.displayMessage : '$e',
            onRetry: () => ref.invalidate(notificationsProvider),
          ),
          data: (page) => page.items.isEmpty
              ? const EmptyStateView(
                  icon: Icons.notifications_none,
                  title: 'Nothing here yet',
                  message: 'We\'ll notify you of activity on your wallet.',
                )
              : RefreshIndicator(
                  onRefresh: () async =>
                      ref.invalidate(notificationsProvider),
                  child: ListView.separated(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.all(20),
                    itemCount: page.items.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 8),
                    itemBuilder: (context, i) =>
                        _NotificationTile(notification: page.items[i]),
                  ),
                ),
        ),
      ),
    );
  }
}

class _NotificationTile extends StatelessWidget {
  const _NotificationTile({required this.notification});

  final AppNotification notification;

  @override
  Widget build(BuildContext context) {
    final (icon, color) = _styleFor(notification.type);
    final unread = !notification.read;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: unread
            ? NovaColors.surfaceContainerLow
            : NovaColors.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: unread
              ? NovaColors.primary.withValues(alpha: 0.3)
              : NovaColors.surfaceContainerLow,
        ),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 42,
            height: 42,
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.12),
              shape: BoxShape.circle,
            ),
            child: Icon(icon, color: color, size: 22),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  notification.subject.isEmpty
                      ? notification.message
                      : notification.subject,
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: unread ? FontWeight.w700 : FontWeight.w600,
                    height: 1.3,
                    color: NovaColors.onSurface,
                  ),
                ),
                if (notification.message.isNotEmpty &&
                    notification.subject.isNotEmpty) ...[
                  const SizedBox(height: 2),
                  Text(
                    notification.message,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 12,
                      height: 1.35,
                      color: NovaColors.onSurfaceVariant,
                    ),
                  ),
                ],
                const SizedBox(height: 4),
                Text(
                  formatRelativeDate(notification.createdAt),
                  style: const TextStyle(
                    fontSize: 11,
                    color: NovaColors.outline,
                  ),
                ),
              ],
            ),
          ),
          if (unread) ...[
            const SizedBox(width: 8),
            Container(
              width: 8,
              height: 8,
              margin: const EdgeInsets.only(top: 4),
              decoration: const BoxDecoration(
                color: NovaColors.primary,
                shape: BoxShape.circle,
              ),
            ),
          ],
        ],
      ),
    );
  }

  (IconData, Color) _styleFor(String type) {
    switch (type) {
      case 'TRANSACTION_DEPOSIT':
      case 'TRANSACTION_TRANSFER_RECEIVED':
        return (Icons.south_west, NovaColors.success);
      case 'TRANSACTION_WITHDRAWAL':
      case 'TRANSACTION_TRANSFER_SENT':
        return (Icons.north_east, NovaColors.error);
      case 'KYC_APPROVED':
        return (Icons.verified_outlined, NovaColors.success);
      case 'KYC_REJECTED':
        return (Icons.gpp_bad_outlined, NovaColors.error);
      case 'WALLET_FROZEN':
        return (Icons.ac_unit, NovaColors.warning);
      case 'WALLET_UNFROZEN':
        return (Icons.lock_open, NovaColors.success);
      case 'EMAIL_VERIFICATION':
        return (Icons.mark_email_read_outlined, NovaColors.primary);
      case 'PASSWORD_RESET':
        return (Icons.lock_reset, NovaColors.warning);
      case 'ADMIN_ACTION':
        return (Icons.admin_panel_settings_outlined, NovaColors.primary);
      default:
        return (Icons.notifications_outlined, NovaColors.primary);
    }
  }
}