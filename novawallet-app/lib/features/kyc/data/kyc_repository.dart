import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';
import '../models/kyc_status.dart';

/// Backend calls for the KYC domain:
///   GET  /v1/kyc/status
///   POST /v1/kyc/documents/upload   (multipart)
///   POST /v1/kyc/submit
class KycRepository {
  const KycRepository(this._api);

  final ApiClient _api;

  Future<KycStatus> getStatus() async {
    final status = await _api.get<KycStatus>(
      '/v1/kyc/status',
      parser: (json) => KycStatus.fromJson(json as Map<String, dynamic>),
    );
    if (status == null)
      throw const ApiException(message: 'KYC status unavailable');
    return status;
  }

  /// Uploads a single KYC document. [fileBytes] carries the raw bytes.
  Future<void> uploadDocument({
    required String documentType,
    required String fileName,
    required List<int> fileBytes,
  }) async {
    final file = MultipartFile.fromBytes(fileBytes, filename: fileName);
    await _api.postMultipart<dynamic>(
      '/v1/kyc/documents/upload',
      file: file,
      fileFieldName: 'file',
      fields: {'documentType': documentType},
    );
  }

  Future<void> submitKyc() async {
    final res = await _api.post<dynamic>('/v1/kyc/submit');
    if (res == null) {
      // Empty success body is fine.
    }
  }
}
