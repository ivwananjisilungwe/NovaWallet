import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/core/network/api_client.dart';
import 'package:novawallet_app/core/network/api_exception.dart';
import 'package:novawallet_app/core/storage/token_storage.dart';

class MockSecureStorage extends Mock implements FlutterSecureStorage {}

/// Fake adapter that returns canned responses so the real interceptor chain
/// (auth + error) and `_guard` unwrap behavior are exercised end to end.
class FakeAdapter implements HttpClientAdapter {
  FakeAdapter(this.handler);

  Future<ResponseBody> Function(RequestOptions) handler;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) => handler(options);

  @override
  void close({bool force = false}) {}
}

ResponseBody jsonResponse(Object? body, int status) => ResponseBody.fromString(
  jsonEncode(body),
  status,
  headers: {
    Headers.contentTypeHeader: [Headers.jsonContentType],
  },
);

void stubStorageDefaults(MockSecureStorage storage) {
  when(
    () => storage.read(key: any(named: 'key')),
  ).thenAnswer((_) async => null);
  when(
    () => storage.write(
      key: any(named: 'key'),
      value: any(named: 'value'),
    ),
  ).thenAnswer((_) async {});
  when(() => storage.delete(key: any(named: 'key'))).thenAnswer((_) async {});
}

const _okEnvelope = {'success': true, 'message': 'OK', 'data': null};

void main() {
  group('ApiClient', () {
    late FakeAdapter adapter;
    late MockSecureStorage mockStorage;
    late ApiClient apiClient;

    setUp(() {
      adapter = FakeAdapter((options) async => jsonResponse(_okEnvelope, 200));
      mockStorage = MockSecureStorage();
      stubStorageDefaults(mockStorage);
      final dio = Dio()..httpClientAdapter = adapter;
      apiClient = ApiClient(
        dio: dio,
        storage: TokenStorage(storage: mockStorage),
      );
    });

    group('envelope unwrap', () {
      test('returns parsed data on success with parser', () async {
        adapter.handler = (options) async => jsonResponse({
          'success': true,
          'message': 'OK',
          'data': {'id': '1', 'name': 'Test'},
        }, 200);

        final result = await apiClient.get<String>(
          '/test',
          parser: (json) => (json as Map<String, dynamic>)['name'] as String,
        );

        expect(result, 'Test');
      });

      test('returns null when data is null', () async {
        final result = await apiClient.get<String>(
          '/test',
          parser: (json) => (json as Map<String, dynamic>)['name'] as String,
        );

        expect(result, isNull);
      });

      test('throws ApiException when success is false', () async {
        adapter.handler = (options) async => jsonResponse({
          'success': false,
          'message': 'Something went wrong',
          'data': null,
        }, 200);

        expect(
          () => apiClient.get('/test'),
          throwsA(
            isA<ApiException>().having(
              (e) => e.message,
              'message',
              'Something went wrong',
            ),
          ),
        );
      });

      test('throws ApiException with details when provided', () async {
        adapter.handler = (options) async => jsonResponse({
          'success': false,
          'message': 'Validation failed',
          'details': {'field': 'email', 'error': 'invalid'},
        }, 200);

        expect(
          () => apiClient.get('/test'),
          throwsA(
            isA<ApiException>()
                .having((e) => e.message, 'message', 'Validation failed')
                .having((e) => e.details, 'details', {
                  'field': 'email',
                  'error': 'invalid',
                }),
          ),
        );
      });
    });

    group('error normalization to ApiException', () {
      test('404 response -> ApiException with statusCode 404', () async {
        adapter.handler = (options) async =>
            jsonResponse({'message': 'Not found'}, 404);

        expect(
          () => apiClient.get('/test'),
          throwsA(
            isA<ApiException>().having((e) => e.statusCode, 'statusCode', 404),
          ),
        );
      });

      test('401 response -> ApiException with isUnauthorized=true', () async {
        adapter.handler = (options) async =>
            jsonResponse({'message': 'Unauthorized'}, 401);

        expect(
          () => apiClient.get('/test'),
          throwsA(
            isA<ApiException>()
                .having((e) => e.isUnauthorized, 'isUnauthorized', isTrue)
                .having((e) => e.statusCode, 'statusCode', 401),
          ),
        );
      });

      test('429 response -> ApiException with isRateLimited=true', () async {
        adapter.handler = (options) async =>
            jsonResponse({'message': 'Too many requests'}, 429);

        expect(
          () => apiClient.get('/test'),
          throwsA(
            isA<ApiException>().having(
              (e) => e.isRateLimited,
              'isRateLimited',
              isTrue,
            ),
          ),
        );
      });

      test('409 response -> ApiException with isConflict=true', () async {
        adapter.handler = (options) async =>
            jsonResponse({'message': 'Conflict'}, 409);

        expect(
          () => apiClient.get('/test'),
          throwsA(
            isA<ApiException>()
                .having((e) => e.isConflict, 'isConflict', isTrue)
                .having((e) => e.statusCode, 'statusCode', 409),
          ),
        );
      });

      test('network error -> ApiException with isNetworkError=true', () async {
        adapter.handler = (options) async {
          throw DioException(
            requestOptions: options,
            type: DioExceptionType.connectionTimeout,
            message: 'Connection timeout',
          );
        };

        expect(
          () => apiClient.get('/test'),
          throwsA(
            isA<ApiException>()
                .having((e) => e.isNetworkError, 'isNetworkError', isTrue)
                .having((e) => e.statusCode, 'statusCode', isNull)
                .having((e) => e.message, 'message', 'Connection timeout'),
          ),
        );
      });
    });

    group('refresh', () {
      test('returns true and saves new tokens on success', () async {
        when(
          () => mockStorage.read(key: 'auth_refresh_token'),
        ).thenAnswer((_) async => 'old_refresh');
        adapter.handler = (options) async => jsonResponse({
          'success': true,
          'message': 'OK',
          'data': {
            'accessToken': 'new_access',
            'refreshToken': 'new_refresh',
            'user': {'id': '1', 'email': 'test@test.com'},
          },
        }, 200);

        final result = await apiClient.refresh();

        expect(result, isTrue);
        verify(
          () =>
              mockStorage.write(key: 'auth_access_token', value: 'new_access'),
        ).called(1);
        verify(
          () => mockStorage.write(
            key: 'auth_refresh_token',
            value: 'new_refresh',
          ),
        ).called(1);
        verify(
          () => mockStorage.write(
            key: 'auth_user_json',
            value: any(named: 'value'),
          ),
        ).called(1);
        verifyNever(() => mockStorage.delete(key: 'auth_access_token'));
      });

      test(
        '401 from refresh endpoint -> returns false and clears storage',
        () async {
          when(
            () => mockStorage.read(key: 'auth_refresh_token'),
          ).thenAnswer((_) async => 'old_refresh');
          // A refresh request must never be retried: mark it as already handled
          // so the auth interceptor does not re-enter refresh() (single-flight
          // would otherwise deadlock on its own in-flight completer).
          adapter.handler = (options) async {
            options.extra['auth_retried'] = true;
            return jsonResponse({'message': 'Invalid refresh token'}, 401);
          };

          final result = await apiClient.refresh();

          expect(result, isFalse);
          verify(() => mockStorage.delete(key: 'auth_access_token')).called(1);
          verify(() => mockStorage.delete(key: 'auth_refresh_token')).called(1);
        },
      );

      test(
        'returns false on network error but does NOT clear storage',
        () async {
          when(
            () => mockStorage.read(key: 'auth_refresh_token'),
          ).thenAnswer((_) async => 'old_refresh');
          adapter.handler = (options) async {
            throw DioException(
              requestOptions: options,
              type: DioExceptionType.connectionTimeout,
              message: 'Connection timeout',
            );
          };

          final result = await apiClient.refresh();

          expect(result, isFalse);
          verifyNever(() => mockStorage.delete(key: 'auth_access_token'));
          verifyNever(() => mockStorage.delete(key: 'auth_refresh_token'));
        },
      );

      test('returns false when no refresh token stored', () async {
        when(
          () => mockStorage.read(key: 'auth_refresh_token'),
        ).thenAnswer((_) async => null);

        final result = await apiClient.refresh();

        expect(result, isFalse);
        verifyNever(() => mockStorage.delete(key: 'auth_access_token'));
      });
    });

    group('postMultipart', () {
      test('sends FormData with file and fields', () async {
        final seenRequests = <RequestOptions>[];
        adapter.handler = (options) async {
          seenRequests.add(options);
          return jsonResponse(_okEnvelope, 200);
        };

        final file = MultipartFile.fromBytes([1, 2, 3], filename: 'test.txt');
        await apiClient.postMultipart<void>(
          '/upload',
          file: file,
          fileFieldName: 'file',
          fields: {'documentType': 'NATIONAL_ID'},
        );

        expect(seenRequests, hasLength(1));
        final request = seenRequests.single;
        expect(request.method, 'POST');
        final form = request.data as FormData;
        expect(
          form.fields.any(
            (e) => e.key == 'documentType' && e.value == 'NATIONAL_ID',
          ),
          isTrue,
        );
        expect(form.files.map((e) => e.key), contains('file'));
        expect(form.files.single.value.filename, 'test.txt');
      });
    });
  });
}
