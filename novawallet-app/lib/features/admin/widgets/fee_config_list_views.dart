import 'package:flutter/material.dart';

import '../../../core/theme/nova_colors.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/widgets.dart';
import '../data/admin_repository.dart';

/// Desktop-style data table of fee configurations for wide layouts: one row
/// per transaction type with percentage / flat / min / max fee, an Active
/// status chip and edit + delete actions.
class FeeConfigTable extends StatelessWidget {
  const FeeConfigTable({
    super.key,
    required this.items,
    required this.isBusy,
    required this.onEdit,
    required this.onDelete,
  });

  final List<FeeConfig> items;
  final bool Function(String id) isBusy;
  final void Function(FeeConfig item) onEdit;
  final void Function(FeeConfig item) onDelete;

  @override
  Widget build(BuildContext context) {
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
          headingRowColor: WidgetStatePropertyAll(
            NovaColors.surfaceContainerLow,
          ),
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
              DataRow(
                cells: [
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
                ],
              ),
          ],
        ),
      ),
    );
  }

  Widget _actionRow(FeeConfig item) {
    final busy = isBusy(item.id);
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        IconButton(
          tooltip: 'Edit',
          onPressed: busy ? null : () => onEdit(item),
          icon: Icon(
            Icons.edit_outlined,
            color: busy ? NovaColors.outlineVariant : NovaColors.primary,
          ),
        ),
        IconButton(
          tooltip: 'Delete',
          onPressed: busy ? null : () => onDelete(item),
          icon: Icon(
            Icons.delete_outline,
            color: busy ? NovaColors.outlineVariant : NovaColors.error,
          ),
        ),
      ],
    );
  }
}

/// Card-based representation of a single fee configuration for narrow
/// layouts (mirrors [FeeConfigTable] in content).
class FeeConfigCard extends StatelessWidget {
  const FeeConfigCard({
    super.key,
    required this.item,
    required this.isBusy,
    required this.onEdit,
    required this.onDelete,
  });

  final FeeConfig item;
  final bool Function(String id) isBusy;
  final void Function(FeeConfig item) onEdit;
  final void Function(FeeConfig item) onDelete;

  @override
  Widget build(BuildContext context) {
    final busy = isBusy(item.id);
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
                onPressed: busy ? null : () => onEdit(item),
                icon: Icon(
                  Icons.edit_outlined,
                  color: busy ? NovaColors.outlineVariant : NovaColors.primary,
                ),
              ),
              IconButton(
                tooltip: 'Delete',
                onPressed: busy ? null : () => onDelete(item),
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
