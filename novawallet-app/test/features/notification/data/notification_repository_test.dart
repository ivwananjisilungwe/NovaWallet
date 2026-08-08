import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/core/network/api_client.dart';
import 'package:novawallet_app/core/network/api_exception.dart';
import 'package:novawallet_app/features/notification/data/notification_repository.dart';
import 'package:novawallet_app/features/notification/models/app_notification.dart';

class MockApiClient extends Mock implements ApiClient {}

void main() {
  group('NotificationRepository', () {
    late MockApiClient mockApi;
    late NotificationRepository repository;

    setUp(() {
      mockApi = MockApiClient();
      repository = NotificationRepository(mockApi);
    });

    final testNotification = AppNotification(
      id: 'notif-1',
      type: 'TRANSACTION_DEPOSIT',
      channel: 'EMAIL',
      subject: 'Deposit received',
      message: 'Your deposit of ZMW 100.00 was successful',
      read: false,
      createdAt: null,
    );

    group('getNotifications', () {
      final testPage = PagedResponse<AppNotification>(
        items: [testNotification],
        totalElements: 1,
        page: 0,
        size: 20,
        totalPages: 1,
      );

      test('GETs /v1/notifications with page/size and parses items', () async {
        when(
          () => mockApi.get<PagedResponse<AppNotification>>(
            '/v1/notifications',
            query: any(named: 'query'),
            parser: any(named: 'parser'),
          ),
        ).thenAnswer((_) async => testPage);

        final result = await repository.getNotifications();

        expect(result, equals(testPage));
        expect(result.items, equals([testNotification]));
        verify(
          () => mockApi.get<PagedResponse<AppNotification>>(
            '/v1/notifications',
            query: {'page': 0, 'size': 20},
            parser: any(named: 'parser'),
          ),
        ).called(1);
      });

      test('sends custom page and size query params', () async {
        when(
          () => mockApi.get<PagedResponse<AppNotification>>(
            '/v1/notifications',
            query: {'page': 2, 'size': 50},
            parser: any(named: 'parser'),
          ),
        ).thenAnswer((_) async => testPage);

        await repository.getNotifications(page: 2, size: 50);

        verify(
          () => mockApi.get<PagedResponse<AppNotification>>(
            '/v1/notifications',
            query: {'page': 2, 'size': 50},
            parser: any(named: 'parser'),
          ),
        ).called(1);
      });

      test('throws ApiException when page is null', () async {
        when(
          () => mockApi.get<PagedResponse<AppNotification>>(
            '/v1/notifications',
            query: any(named: 'query'),
            parser: any(named: 'parser'),
          ),
        ).thenAnswer((_) async => null);

        expect(
          () => repository.getNotifications(),
          throwsA(isA<ApiException>()),
        );
      });
    });

    group('getUnreadCount', () {
      test('GETs /v1/notifications/unread-count and parses count', () async {
        when(
          () => mockApi.get<int>(
            '/v1/notifications/unread-count',
            parser: any(named: 'parser'),
          ),
        ).thenAnswer((_) async => 3);

        final result = await repository.getUnreadCount();

        expect(result, equals(3));
        verify(
          () => mockApi.get<int>(
            '/v1/notifications/unread-count',
            parser: any(named: 'parser'),
          ),
        ).called(1);
      });

      test('parser extracts count from nested envelope when invoked', () {
        when(
          () => mockApi.get<int>(
            '/v1/notifications/unread-count',
            parser: any(named: 'parser'),
          ),
        ).thenAnswer((invocation) async {
          final parser = invocation.namedArguments[#parser]
              as int Function(dynamic);
          return parser({'count': 5});
        });

        final result = repository.getUnreadCount();

        expect(result, completion(5));
      });

      test('returns 0 when api returns null', () async {
        when(
          () => mockApi.get<int>(
            '/v1/notifications/unread-count',
            parser: any(named: 'parser'),
          ),
        ).thenAnswer((_) async => null);

        final result = await repository.getUnreadCount();

        expect(result, equals(0));
      });
    });

    group('markAllAsRead', () {
      test('PUTs to /v1/notifications/read-all', () async {
        when(() => mockApi.put<void>('/v1/notifications/read-all'))
            .thenAnswer((_) async {});

        await repository.markAllAsRead();

        verify(
          () => mockApi.put<void>('/v1/notifications/read-all'),
        ).called(1);
      });
    });
  });
}