import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/widgets.dart';
import '../data/admin_repository.dart';
import '../widgets/fee_config_form_dialog.dart';
import '../widgets/fee_config_list_views.dart';

/// Desktop-style admin table of fee configurations (per design). Lists one
/// row per transaction type with percentage / flat / min / max fee, an
/// Active status chip and edit + delete actions.
///
/// The form dialog lives in `../widgets/fee_config_form_dialog.dart`; the
/// wide-layout table and narrow-layout cards live in
/// `../widgets/fee_config_list_views.dart`.
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
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
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
      _showSnack(
        'Fee configuration for ${data.transactionType.label} created.',
      );
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
            style: FilledButton.styleFrom(backgroundColor: NovaColors.error),
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
  Future<FeeConfigDraft?> _showConfigDialog({required FeeConfig? existing}) {
    return showDialog<FeeConfigDraft>(
      context: context,
      barrierDismissible: false,
      builder: (context) =>
          FeeConfigFormDialog(existing: existing, isCreating: existing == null),
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
                  padding: const EdgeInsets.symmetric(
                    horizontal: 20,
                    vertical: 12,
                  ),
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
                return const LoadingView(
                  message: 'Loading fee configurations…',
                );
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
                      child: FeeConfigTable(
                        items: items,
                        isBusy: _isBusy,
                        onEdit: _edit,
                        onDelete: _delete,
                      ),
                    );
                  }
                  return ListView.separated(
                    padding: const EdgeInsets.all(20),
                    itemCount: items.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 8),
                    itemBuilder: (context, i) => FeeConfigCard(
                      item: items[i],
                      isBusy: _isBusy,
                      onEdit: _edit,
                      onDelete: _delete,
                    ),
                  );
                },
              );
            },
          ),
        ),
      ),
    );
  }
}
