import 'package:flutter_test/flutter_test.dart';

import 'package:novawallet_app/features/notification/models/app_notification.dart';

void main() {
  group('AppNotification', () {
    test('fromJson parses all fields', () {
      final notification = AppNotification.fromJson({
        'id': 'notif-1',
        'type': 'TRANSACTION_DEPOSIT',
        'channel': 'EMAIL',
        'subject': 'Deposit received',
        'message': 'Your deposit of ZMW 100.00 was successful',
        'read': false,
        'createdAt': '2026-08-07T09:05:00',
      });

      expect(notification.id, 'notif-1');
      expect(notification.type, 'TRANSACTION_DEPOSIT');
      expect(notification.channel, 'EMAIL');
      expect(notification.subject, 'Deposit received');
      expect(notification.message, 'Your deposit of ZMW 100.00 was successful');
      expect(notification.read, isFalse);
      expect(notification.createdAt, isNotNull);
    });

    test('fromJson parses read notification', () {
      final notification = AppNotification.fromJson({
        'id': 'notif-2',
        'type': 'KYC_APPROVED',
        'channel': 'SMS',
        'subject': 'KYC approved',
        'message': 'Your identity was verified.',
        'read': true,
        'createdAt': '2026-08-05T15:20:00',
      });

      expect(notification.read, isTrue);
      expect(notification.channel, 'SMS');
    });

    test('fromJson applies defaults for missing optional fields', () {
      final notification = AppNotification.fromJson(const {});

      expect(notification.id, '');
      expect(notification.type, '');
      expect(notification.channel, 'EMAIL');
      expect(notification.subject, '');
      expect(notification.message, '');
      expect(notification.read, isFalse);
      expect(notification.createdAt, isNull);
    });
  });
}