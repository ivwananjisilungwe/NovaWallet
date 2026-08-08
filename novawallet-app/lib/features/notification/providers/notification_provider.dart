import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/providers.dart';
import '../data/notification_repository.dart';
import '../models/app_notification.dart';

/// Repository singleton for the notification domain.
final notificationRepositoryProvider = Provider<NotificationRepository>(
  (ref) => NotificationRepository(ref.watch(apiClientProvider)),
);

/// Notification inbox (first page, newest first).
final notificationsProvider =
    FutureProvider<PagedResponse<AppNotification>>(
  (ref) => ref.watch(notificationRepositoryProvider).getNotifications(),
);

/// Unread notification count — drives the dashboard bell badge.
final unreadCountProvider = FutureProvider<int>(
  (ref) => ref.watch(notificationRepositoryProvider).getUnreadCount(),
);

/// Marks every notification as read, then invalidates [notificationsProvider]
/// and [unreadCountProvider] so both refetch on their next watch.
Future<void> markAllNotificationsAsRead(WidgetRef ref) async {
  await ref.read(notificationRepositoryProvider).markAllAsRead();
  ref.invalidate(notificationsProvider);
  ref.invalidate(unreadCountProvider);
}
