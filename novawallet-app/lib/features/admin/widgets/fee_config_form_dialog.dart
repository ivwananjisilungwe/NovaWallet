import 'package:flutter/material.dart';

import '../data/admin_repository.dart';

/// Validated shape returned from [FeeConfigFormDialog].
class FeeConfigDraft {
  const FeeConfigDraft({
    required this.transactionType,
    required this.percentageFee,
    required this.flatFee,
    required this.active,
    this.minFee,
    this.maxFee,
  });

  final FeeType transactionType;
  final double percentageFee;
  final double flatFee;
  final double? minFee;
  final double? maxFee;
  final bool active;
}

/// Create / edit dialog for a single fee rule.
class FeeConfigFormDialog extends StatefulWidget {
  const FeeConfigFormDialog({
    super.key,
    required this.existing,
    required this.isCreating,
  });

  final FeeConfig? existing;
  final bool isCreating;

  @override
  State<FeeConfigFormDialog> createState() => _FeeConfigFormDialogState();
}

class _FeeConfigFormDialogState extends State<FeeConfigFormDialog> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _percentage;
  late final TextEditingController _flat;
  late final TextEditingController _min;
  late final TextEditingController _max;
  late FeeType _type;
  late bool _active;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    final existing = widget.existing;
    _type = existing?.transactionType ?? FeeType.deposit;
    _active = existing?.active ?? true;
    _percentage = TextEditingController(
      text: _feeText(existing?.percentageFee ?? 0),
    );
    _flat = TextEditingController(text: _feeText(existing?.flatFee ?? 0));
    _min = TextEditingController(
      text: existing?.minFee == null ? '' : _feeText(existing!.minFee!),
    );
    _max = TextEditingController(
      text: existing?.maxFee == null ? '' : _feeText(existing!.maxFee!),
    );
  }

  @override
  void dispose() {
    _percentage.dispose();
    _flat.dispose();
    _min.dispose();
    _max.dispose();
    super.dispose();
  }

  /// Pre-fill amounts without trailing zeros (e.g. `0.5`, `2`, `50.00` -> `50`).
  String _feeText(double value) =>
      value.toStringAsFixed(2).replaceFirst(RegExp(r'\.?0+$'), '');

  double? _parseAmount(String text) => double.tryParse(text.trim());

  String? _validate(String? text) {
    final value = _parseAmount(text ?? '');
    if (value == null || value < 0) {
      return 'Enter a non-negative amount';
    }
    return null;
  }

  Future<void> _save() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;

    final minFee = _parseAmount(_min.text.trim());
    final maxFee = _parseAmount(_max.text.trim());

    setState(() => _saving = true);
    final result = FeeConfigDraft(
      transactionType: _type,
      percentageFee: _parseAmount(_percentage.text.trim()) ?? 0,
      flatFee: _parseAmount(_flat.text.trim()) ?? 0,
      minFee: minFee,
      maxFee: maxFee,
      active: _active,
    );
    Navigator.pop(context, result);
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(
        widget.isCreating ? 'Add fee configuration' : 'Edit fee configuration',
      ),
      content: Form(
        key: _formKey,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              DropdownButtonFormField<FeeType>(
                initialValue: _type,
                decoration: const InputDecoration(
                  labelText: 'Transaction type',
                  border: OutlineInputBorder(),
                ),
                items: [
                  for (final type in FeeType.values)
                    DropdownMenuItem(value: type, child: Text(type.label)),
                ],
                onChanged: (value) {
                  if (value != null) setState(() => _type = value);
                },
                validator: (value) => value == null ? 'Select a type' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _percentage,
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
                decoration: const InputDecoration(
                  labelText: 'Percentage fee (%)',
                  hintText: 'e.g. 0.5 = 0.5%',
                  border: OutlineInputBorder(),
                ),
                validator: _validate,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _flat,
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
                decoration: const InputDecoration(
                  labelText: 'Flat fee (ZMW)',
                  prefixText: 'ZMW ',
                  border: OutlineInputBorder(),
                ),
                validator: _validate,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _min,
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
                decoration: const InputDecoration(
                  labelText: 'Minimum fee (ZMW)',
                  hintText: 'Optional',
                  prefixText: 'ZMW ',
                  border: OutlineInputBorder(),
                ),
                validator: (value) => (value == null || value.trim().isEmpty)
                    ? null
                    : _validate(value),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _max,
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
                decoration: const InputDecoration(
                  labelText: 'Maximum fee (ZMW)',
                  hintText: 'Optional',
                  prefixText: 'ZMW ',
                  border: OutlineInputBorder(),
                ),
                validator: (value) => (value == null || value.trim().isEmpty)
                    ? null
                    : _validate(value),
              ),
              const SizedBox(height: 8),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Active'),
                subtitle: Text(
                  widget.isCreating
                      ? 'New fee configurations start active'
                      : 'Toggles whether this fee applies',
                ),
                value: _active,
                onChanged: (value) => setState(() => _active = value),
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _saving ? null : () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: _saving ? null : _save,
          child: Text(widget.isCreating ? 'Create' : 'Save changes'),
        ),
      ],
    );
  }
}
