import '../../../core/utils/formatters.dart';

/// Notification mirroring backend `NotificationResponse`:
/// `{id, type, channel, subject, message, read, createdAt}`.
class AppNotification {
  const AppNotification({
    required this.id,
    required this.type,
    required this.channel,
    required this.subject,
    required this.message,
    required this.read,
    this.createdAt,
  });

  final String id;
  final String type; // EMAIL_VERIFICATION | KYC_APPROVED | TRANSACTION_DEPOSIT | ...
  final String channel; // EMAIL | SMS
  final String subject;
  final String message;
  final bool read;
  final DateTime? createdAt;

  factory AppNotification.fromJson(Map<String, dynamic> json) =>
      AppNotification(
        id: (json['id'] ?? '').toString(),
        type: (json['type'] ?? '').toString(),
        channel: (json['channel'] ?? 'EMAIL').toString(),
        subject: (json['subject'] ?? '').toString(),
        message: (json['message'] ?? '').toString(),
        read: json['read'] as bool? ?? false,
        createdAt: parseBackendDate(json['createdAt'] as String?),
      );
}
