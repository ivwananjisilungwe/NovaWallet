/// Formatting helpers for currency and dates (Zambian/design-system aware).
library;

import 'package:intl/intl.dart';

/// Formats a ZMW amount from its raw decimal string, e.g. `1500.5` -> `ZMW 1,500.50`.
String formatZmw(dynamic raw, {bool includeSymbol = true}) {
  final value = raw is num ? raw.toDouble() : double.tryParse(raw?.toString() ?? '') ?? 0.0;
  final body = NumberFormat.currency(
    locale: 'en_ZM',
    symbol: '',
    decimalDigits: 2,
  ).format(value);
  return includeSymbol ? 'ZMW $body'.trim() : body.trim();
}

/// Formats a plain number (bytes, counts) with thousands separators.
String formatNumber(dynamic raw) {
  final n = raw is num ? raw : (int.tryParse(raw?.toString() ?? '') ?? 0);
  return NumberFormat.decimalPattern('en').format(n);
}

/// Human-friendly relative date ("Today", "Yesterday", "Mon, 09:00").
String formatRelativeDate(DateTime? dt) {
  if (dt == null) return '';
  final now = DateTime.now();
  final local = dt.toLocal();
  final today = DateTime(now.year, now.month, now.day);
  final thatDay = DateTime(local.year, local.month, local.day);
  final diff = today.difference(thatDay).inDays;
  if (diff == 0) return 'Today, ${DateFormat.jm().format(local)}';
  if (diff == 1) return 'Yesterday, ${DateFormat.jm().format(local)}';
  return DateFormat('EEE, HH:mm').format(local);
}

/// Absolute date/time label for detail screens.
String formatDateTime(DateTime? dt) {
  if (dt == null) return '';
  return DateFormat('dd MMM yyyy, HH:mm').format(dt.toLocal());
}

/// Parser tolerant of backend `LocalDateTime` strings (`2026-07-10T09:00:00`).
DateTime? parseBackendDate(String? value) {
  if (value == null || value.isEmpty) return null;
  return DateTime.tryParse(value);
}
