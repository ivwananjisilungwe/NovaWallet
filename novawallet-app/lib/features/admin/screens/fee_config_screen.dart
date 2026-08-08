import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/widgets.dart';
import '../data/admin_repository.dart';

/// Desktop-style admin table of fee configurations (per design). Lists one
/// row per transaction type with percentage / flat / min / max fee, an
/// Active status chip and edit + delete actions.
class FeeConfigScreen extends ConsumerStatefulWidget {
  const FeeConfigScreen({super.key});

  @override
  ConsumerState<FeeConfigScreen> createState() => _FeeConfigScreenState();
}

class _FeeConfigScreenState extends ConsumerState<FeeConfigScreen> {
  late Future<List<FeeConfig>> _items;
  final Set<String> _busyIds = {};

  @override
  void initState() {
    super.initState();
    _items = ref.read(adminRepositoryProvider).getFeeConfigurations();
  }

  void _reload() => setState(() {
        _items = ref.read(adminRepositoryProvider).getFeeConfigurations();
      });

  bool _isBusy(String id) => _busyIds.contains(id);

  void _setBusy(String id, bool busy) {
    setState(() {
      if (busy) {
        _busyIds.add(id);
      } else {
        _busyIds.remove(id);
      }
    });
  }

  void _showSnack(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _add() async {
    final data = await _showConfigDialog(existing: null);
    if (data == null || !mounted) return;

    final repo = ref.read(adminRepositoryProvider);
    try {
      await repo.createFeeConfiguration(
        transactionType: data.transactionType,
        percentageFee: data.percentageFee,
        flatFee: data.flatFee,
        minFee: data.minFee,
        maxFee: data.maxFee,
        active: data.active,
      );
      _reload();
      _showSnack('Fee configuration for ${data.transactionType.label} created.');
    } on ApiException catch (e) {
      _showSnack(e.displayMessage);
    }
  }

  Future<void> _edit(FeeConfig item) async {
    final data = await _showConfigDialog(existing: item);
    if (data == null || !mounted) return;

    _setBusy(item.id, true);
    try {
      final repo = ref.read(adminRepositoryProvider);
      await repo.updateFeeConfiguration(
        item.id,
        transactionType: data.transactionType,
        percentageFee: data.percentageFee,
        flatFee: data.flatFee,
        minFee: data.minFee,
        maxFee: data.maxFee,
        active: data.active,
      );
      _reload();
      _showSnack('Fee configuration updated.');
    } on ApiException catch (e) {
      _showSnack(e.displayMessage);
    } finally {
      if (mounted) _setBusy(item.id, false);
    }
  }

  Future<void> _delete(FeeConfig item) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete fee configuration?'),
        content: Text(
          'This removes the ${item.transactionType.label} fee rule. '
          'Transactions will fall back to a zero fee until a new rule is '
          'configured. This action cannot be undone.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: NovaColors.error,
            ),
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Delete'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    _setBusy(item.id, true);
    try {
      await ref.read(adminRepositoryProvider).deleteFeeConfiguration(item.id);
      _reload();
      _showSnack('${item.transactionType.label} fee configuration deleted.');
    } on ApiException catch (e) {
      _showSnack(e.displayMessage);
    } finally {
      if (mounted) _setBusy(item.id, false);
    }
  }

  /// Shared edit dialog; [existing] == null means "create a new rule".
  Future<_FeeConfigDraft?> _showConfigDialog({required FeeConfig? existing}) {
    return showDialog<_FeeConfigDraft>(
      context: context,
      barrierDismissible: false,
      builder: (context) =>
          _FeeFormDialog(existing: existing, isCreating: existing == null),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(
        title: const Text('Fee configurations'),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: Center(
              child: FilledButton.icon(
                onPressed: _add,
                style: FilledButton.styleFrom(
                  shape: const StadiumBorder(),
                  padding:
                      const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                ),
                icon: const Icon(Icons.add, size: 20),
                label: const Text('Add fee configuration'),
              ),
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () async => _reload(),
          child: FutureBuilder<List<FeeConfig>>(
            future: _items,
            builder: (context, snap) {
              if (snap.connectionState != ConnectionState.done) {
                return const LoadingView(message: 'Loading fee configurations…');
              }
              if (snap.hasError) {
                return ErrorStateView(
                  message: '${snap.error}',
                  onRetry: _reload,
                );
              }
              final items = snap.data ?? const [];
              if (items.isEmpty) {
                return ListView(
                  children: const [
                    SizedBox(height: 120),
                    EmptyStateView(
                      icon: Icons.receipt_long_outlined,
                      title: 'No fee configurations',
                      message:
                          'Add your first fee rule to start charging fees.',
                    ),
                  ],
                );
              }
              // Wide layouts get the desktop data table; narrow stays card-based.
              return LayoutBuilder(
                builder: (context, constraints) {
                  if (constraints.maxWidth >= 900) {
                    return SingleChildScrollView(
                      padding: const EdgeInsets.all(24),
                      child: _buildTable(items),
                    );
                  }
                  return ListView.separated(
                    padding: const EdgeInsets.all(20),
                    itemCount: items.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 8),
                    itemBuilder: (context, i) => _feeCard(items[i]),
                  );
                },
              );
            },
          ),
        ),
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // Table (wide layouts)
  // ---------------------------------------------------------------------------

  Widget _buildTable(List<FeeConfig> items) {
    return Container(
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: NovaColors.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: NovaColors.surfaceContainerLow),
      ),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: DataTable(
          headingRowColor: WidgetStatePropertyAll(NovaColors.surfaceContainerLow),
          dataRowMinHeight: 56,
          dataRowMaxHeight: 64,
          columns: const [
            DataColumn(label: Text('Transaction type')),
            DataColumn(label: Text('%')),
            DataColumn(label: Text('Flat')),
            DataColumn(label: Text('Min')),
            DataColumn(label: Text('Max')),
            DataColumn(label: Text('Status')),
            DataColumn(label: Text('Actions')),
          ],
          rows: [
            for (final item in items)
              DataRow(cells: [
                DataCell(
                  Text(
                    item.transactionType.label,
                    style: const TextStyle(fontWeight: FontWeight.w600),
                  ),
                ),
                DataCell(Text(_percentText(item.percentageFee))),
                DataCell(Text(formatZmw(item.flatFee))),
                DataCell(Text(_moneyOrDash(item.minFee))),
                DataCell(Text(_moneyOrDash(item.maxFee))),
                DataCell(_statusChip(item.active)),
                DataCell(_actionRow(item)),
              ]),
          ],
        ),
      ),
    );
  }

  Widget _actionRow(FeeConfig item) {
    final busy = _isBusy(item.id);
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        IconButton(
          tooltip: 'Edit',
          onPressed: busy ? null : () => _edit(item),
          icon: Icon(
            Icons.edit_outlined,
            color: busy ? NovaColors.outlineVariant : NovaColors.primary,
          ),
        ),
        IconButton(
          tooltip: 'Delete',
          onPressed: busy ? null : () => _delete(item),
          icon: Icon(
            Icons.delete_outline,
            color: busy ? NovaColors.outlineVariant : NovaColors.error,
          ),
        ),
      ],
    );
  }

  // ---------------------------------------------------------------------------
  // Narrow card list
  // ---------------------------------------------------------------------------

  Widget _feeCard(FeeConfig item) {
    final busy = _isBusy(item.id);
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: NovaColors.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: NovaColors.surfaceContainerLow),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  item.transactionType.label,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              _statusChip(item.active),
            ],
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 16,
            runSpacing: 8,
            children: [
              _kvLine('Percentage', _percentText(item.percentageFee)),
              _kvLine('Flat fee', formatZmw(item.flatFee)),
              _kvLine('Min fee', _moneyOrDash(item.minFee)),
              _kvLine('Max fee', _moneyOrDash(item.maxFee)),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              IconButton(
                tooltip: 'Edit',
                onPressed: busy ? null : () => _edit(item),
                icon: Icon(
                  Icons.edit_outlined,
                  color: busy ? NovaColors.outlineVariant : NovaColors.primary,
                ),
              ),
              IconButton(
                tooltip: 'Delete',
                onPressed: busy ? null : () => _delete(item),
                icon: Icon(
                  Icons.delete_outline,
                  color: busy ? NovaColors.outlineVariant : NovaColors.error,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _kvLine(String label, String value) => Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(label, style: const TextStyle(color: NovaColors.onSurfaceVariant)),
          const SizedBox(width: 6),
          Text(value, style: const TextStyle(fontWeight: FontWeight.w600)),
        ],
      );

  Widget _statusChip(bool active) => StatusChip(
        label: active ? 'Active' : 'Inactive',
        color: active ? NovaColors.success : NovaColors.outline,
      );

  String _percentText(double value) {
    final s = value.toStringAsFixed(4).replaceFirst(RegExp(r'\.?0+$'), '');
    return '$s%';
  }

  String _moneyOrDash(double? value) => value == null ? '—' : formatZmw(value);
}

/// Validated shape returned from [_FeeFormDialog].
class _FeeConfigDraft {
  const _FeeConfigDraft({
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
class _FeeFormDialog extends StatefulWidget {
  const _FeeFormDialog({required this.existing, required this.isCreating});

  final FeeConfig? existing;
  final bool isCreating;

  @override
  State<_FeeFormDialog> createState() => _FeeFormDialogState();
}

class _FeeFormDialogState extends State<_FeeFormDialog> {
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
    _percentage = TextEditingController(text: _feeText(existing?.percentageFee ?? 0));
    _flat = TextEditingController(text: _feeText(existing?.flatFee ?? 0));
    _min = TextEditingController(
        text: existing?.minFee == null ? '' : _feeText(existing!.minFee!));
    _max = TextEditingController(
        text: existing?.maxFee == null ? '' : _feeText(existing!.maxFee!));
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
    final result = _FeeConfigDraft(
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
      title: Text(widget.isCreating ? 'Add fee configuration' : 'Edit fee configuration'),
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
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
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
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
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
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(
                  labelText: 'Minimum fee (ZMW)',
                  hintText: 'Optional',
                  prefixText: 'ZMW ',
                  border: OutlineInputBorder(),
                ),
                validator: (value) =>
                    (value == null || value.trim().isEmpty)
                        ? null
                        : _validate(value),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _max,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(
                  labelText: 'Maximum fee (ZMW)',
                  hintText: 'Optional',
                  prefixText: 'ZMW ',
                  border: OutlineInputBorder(),
                ),
                validator: (value) =>
                    (value == null || value.trim().isEmpty)
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