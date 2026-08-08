import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';
import '../models/app_notification.dart';

/// Backend calls for the notification domain:
///   GET /v1/notifications              (paginated inbox, newest first)
///   GET /v1/notifications/unread-count (badge count)
///   PUT /v1/notifications/read-all     (mark all as read)
class NotificationRepository {
  const NotificationRepository(this._api);

  final ApiClient _api;

  Future<PagedResponse<AppNotification>> getNotifications({
    int page = 0,
    int size = 20,
  }) async {
    final resp = await _api.get<PagedResponse<AppNotification>>(
      '/v1/notifications',
      query: {'page': page, 'size': size},
      parser: (json) => PagedResponse.fromJson(
        json as Map<String, dynamic>,
        (item) => AppNotification.fromJson(item as Map<String, dynamic>),
      ),
    );
    if (resp == null) {
      throw const ApiException(message: 'Notifications unavailable');
    }
    return resp;
  }

  Future<int> getUnreadCount() async {
    final count = await _api.get<int>(
      '/v1/notifications/unread-count',
      parser: (json) =>
          (json as Map<String, dynamic>)['count']?.toInt() ?? 0,
    );
    return count ?? 0;
  }

  Future<void> markAllAsRead() async {
    await _api.put<void>('/v1/notifications/read-all');
  }
}
