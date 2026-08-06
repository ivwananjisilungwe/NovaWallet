/// Standard backend error envelope: `ApiResponse<T>` with `success`,
/// `message`, and optional `data`. On failure only [message] is guaranteed.
class ApiException implements Exception {
  const ApiException({
    required this.message,
    this.statusCode,
    this.details,
  });

  /// Human-readable error message (from backend `message` field or fallback).
  final String message;

  /// HTTP status code, when the backend was reached.
  final int? statusCode;

  /// Optional backend `details` payload.
  final Object? details;

  bool get isNetworkError => statusCode == null;
  bool get isUnauthorized => statusCode == 401;
  bool get isRateLimited => statusCode == 429;
  bool get isConflict => statusCode == 409;

  /// Convenience for `SnackBar`/inline error text.
  String get displayMessage {
    if (isNetworkError) {
      return 'Network error — check your connection and try again.';
    }
    return message;
  }

  @override
  String toString() => 'ApiException($statusCode): $message';
}

/// Backend response envelope: `{"success": true, "message": "...", "data": {...}}`
class ApiResponse<T> {
  const ApiResponse({
    required this.success,
    required this.message,
    this.data,
  });

  final bool success;
  final String message;
  final T? data;

  factory ApiResponse.fromJson(
    Map<String, dynamic> json,
    T Function(dynamic json)? parser,
  ) {
    return ApiResponse(
      success: json['success'] as bool? ?? false,
      message: json['message'] as String? ?? '',
      data: json['data'] == null || parser == null
          ? null
          : parser(json['data']),
    );
  }
}

/// Paged backend response: `{"content": [...], "totalElements": N, ...}`.
class PagedResponse<T> {
  const PagedResponse({
    required this.items,
    required this.totalElements,
    required this.page,
    required this.size,
    required this.totalPages,
  });

  final List<T> items;
  final int totalElements;
  final int page;
  final int size;
  final int totalPages;

  bool get isLastPage => page >= totalPages - 1;
  bool get hasMore => items.isNotEmpty && !isLastPage;

  factory PagedResponse.fromJson(
    Map<String, dynamic> json,
    T Function(dynamic json) parser,
  ) {
    final content = (json['content'] as List<dynamic>? ?? const [])
        .map((e) => parser(e))
        .toList();
    return PagedResponse(
      items: content,
      totalElements: json['totalElements'] as int? ?? 0,
      page: json['number'] as int? ?? 0,
      size: json['size'] as int? ?? 10,
      totalPages: json['totalPages'] as int? ?? 0,
    );
  }
}
