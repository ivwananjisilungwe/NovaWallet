import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/widgets.dart';
import '../providers/kyc_provider.dart';

/// KYC document upload (per design 17). Pick document type, attach file, then submit.
class KycUploadScreen extends ConsumerStatefulWidget {
  const KycUploadScreen({super.key});

  @override
  ConsumerState<KycUploadScreen> createState() => _KycUploadScreenState();
}

class _KycUploadScreenState extends ConsumerState<KycUploadScreen> {
  String? _type;
  XFile? _file;
  bool _submitting = false;

  static const _types = [
    ('NATIONAL_ID', 'National ID'),
    ('PASSPORT', 'Passport'),
    ('SELFIE', 'Selfie'),
    ('PROOF_OF_ADDRESS', 'Proof of address'),
  ];

  Future<void> _pickFile() async {
    final source = await showModalBottomSheet<ImageSource>(
      context: context,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.photo_library_outlined),
              title: const Text('Gallery'),
              onTap: () => Navigator.pop(context, ImageSource.gallery),
            ),
            ListTile(
              leading: const Icon(Icons.camera_alt_outlined),
              title: const Text('Camera'),
              onTap: () => Navigator.pop(context, ImageSource.camera),
            ),
          ],
        ),
      ),
    );
    if (source == null) return;
    final picked = await ImagePicker().pickImage(source: source);
    if (picked != null && mounted) {
      setState(() => _file = picked);
    }
  }

  Future<void> _submit() async {
    final type = _type;
    final file = _file;
    if (type == null) {
      _toast('Select a document type.');
      return;
    }
    if (file == null) {
      _toast('Attach a document photo.');
      return;
    }
    setState(() => _submitting = true);
    try {
      final bytes = await file.readAsBytes();
      await ref
          .read(kycRepositoryProvider)
          .uploadDocument(
            documentType: type,
            fileName: file.name,
            fileBytes: bytes,
          );
      await ref.read(kycRepositoryProvider).submitKyc();
      await ref.read(kycStatusProvider.notifier).refresh();
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Submitted for review')));
      context.go('/kyc/status');
    } on ApiException catch (e) {
      if (mounted) _toast(e.displayMessage);
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  void _toast(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('Identity verification')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Choose a document',
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w700,
                  color: NovaColors.onBackground,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Upload a clear photo of your document for verification.',
                style: TextStyle(color: NovaColors.onSurfaceVariant),
              ),
              const SizedBox(height: 20),
              RadioGroup<String>(
                groupValue: _type,
                onChanged: (v) => setState(() => _type = v),
                child: Column(
                  children: [
                    for (final (value, label) in _types)
                      RadioListTile<String>(
                        value: value,
                        title: Text(label),
                        activeColor: NovaColors.primary,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(16),
                        ),
                      ),
                  ],
                ),
              ),
              const SizedBox(height: 8),
              InkWell(
                onTap: _pickFile,
                borderRadius: BorderRadius.circular(16),
                child: Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: NovaColors.surfaceContainerLow,
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: _file == null
                      ? const Row(
                          children: [
                            Icon(
                              Icons.upload_file_outlined,
                              color: NovaColors.primary,
                            ),
                            SizedBox(width: 12),
                            Expanded(
                              child: Text(
                                'Tap to attach a document photo',
                                style: TextStyle(color: NovaColors.onSurface),
                              ),
                            ),
                          ],
                        )
                      : Row(
                          children: [
                            ClipRRect(
                              borderRadius: BorderRadius.circular(8),
                              child: Image.file(
                                File(_file!.path),
                                width: 48,
                                height: 48,
                                fit: BoxFit.cover,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    _file!.name,
                                    style: const TextStyle(
                                      color: NovaColors.onSurface,
                                      fontWeight: FontWeight.w500,
                                    ),
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                  const Text(
                                    'Tap to change',
                                    style: TextStyle(
                                      color: NovaColors.onSurfaceVariant,
                                      fontSize: 12,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            const Icon(
                              Icons.edit_outlined,
                              color: NovaColors.onSurfaceVariant,
                            ),
                          ],
                        ),
                ),
              ),
              const SizedBox(height: 28),
              PillButton(
                label: 'Submit for review',
                loading: _submitting,
                onPressed: _submit,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
