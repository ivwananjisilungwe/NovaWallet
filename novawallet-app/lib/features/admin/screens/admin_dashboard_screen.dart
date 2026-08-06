import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/widgets.dart';
import '../data/admin_repository.dart';

/// Admin dashboard (per design 21). Shows the pending KYC queue with quick
/// approve / reject actions.
class AdminDashboardScreen extends ConsumerStatefulWidget {
  const AdminDashboardScreen({super.key});

  @override
  ConsumerState<AdminDashboardScreen> createState() => _AdminDashboardScreenState();
}

class _AdminDashboardScreenState extends ConsumerState<AdminDashboardScreen> {
  late Future<List<KycQueueItem>> _queue;
  final Set<String> _loadingIds = {};

  @override
  void initState() {
    super.initState();
    _queue = ref.read(adminRepositoryProvider).pendingKyc();
  }

  void _reload() => setState(() {
        _queue = ref.read(adminRepositoryProvider).pendingKyc();
      });

  bool _isLoading(String userId) => _loadingIds.contains(userId);

  void _setLoading(String userId, bool loading) {
    setState(() {
      if (loading) {
        _loadingIds.add(userId);
      } else {
        _loadingIds.remove(userId);
      }
    });
  }

  Future<void> _approve(KycQueueItem item) async {
    final tier = await _showTierDialog(item);
    if (tier == null || !mounted) return;

    _setLoading(item.userId, true);
    try {
      final repo = ref.read(adminRepositoryProvider);
      await repo.approveKyc(item.userId, tier: tier);
      _reload();
    } on ApiException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(e.displayMessage)));
      }
    } finally {
      if (mounted) _setLoading(item.userId, false);
    }
  }

  Future<void> _reject(KycQueueItem item) async {
    final reason = await _showRejectDialog();
    if (reason == null || reason.trim().isEmpty || !mounted) return;

    _setLoading(item.userId, true);
    try {
      final repo = ref.read(adminRepositoryProvider);
      await repo.rejectKyc(item.userId, reason: reason.trim());
      _reload();
    } on ApiException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(e.displayMessage)));
      }
    } finally {
      if (mounted) _setLoading(item.userId, false);
    }
  }

  Future<int?> _showTierDialog(KycQueueItem item) async {
    int selectedTier = 1;
    final parsed = int.tryParse(item.tier);
    if (parsed != null && parsed >= 1 && parsed <= 3) {
      selectedTier = parsed;
    }

    return showDialog<int>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setState) => SimpleDialog(
          title: const Text('Select KYC Tier'),
          children: [
            for (final tier in [1, 2, 3])
              RadioListTile<int>(
                title: Text('Tier $tier'),
                value: tier,
                groupValue: selectedTier,
                onChanged: (value) => setState(() => selectedTier = value!),
              ),
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 8, 24, 16),
              child: FilledButton(
                onPressed: () => Navigator.pop(context, selectedTier),
                child: const Text('Confirm'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<String?> _showRejectDialog() async {
    final controller = TextEditingController();
    String? result;

    await showDialog<void>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setState) => AlertDialog(
          title: const Text('Reject KYC'),
          content: TextField(
            controller: controller,
            maxLines: 4,
            maxLength: 200,
            decoration: const InputDecoration(
              hintText: 'Enter rejection reason…',
              border: OutlineInputBorder(),
            ),
            onChanged: (_) => setState(() {}),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: controller.text.trim().isEmpty
                  ? null
                  : () {
                      result = controller.text.trim();
                      Navigator.pop(context);
                    },
              child: const Text('Confirm'),
            ),
          ],
        ),
      ),
    );

    return result;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('Admin console')),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () async => _reload(),
          child: FutureBuilder<List<KycQueueItem>>(
            future: _queue,
            builder: (context, snap) {
              if (snap.connectionState != ConnectionState.done) {
                return const LoadingView(message: 'Loading…');
              }
              if (snap.hasError) {
                return ErrorStateView(message: '${snap.error}', onRetry: _reload);
              }
              final items = snap.data ?? const [];
              if (items.isEmpty) {
                return ListView(
                  children: [
                    const SizedBox(height: 120),
                    const EmptyStateView(
                      icon: Icons.task_alt,
                      title: 'Queue clear',
                      message: 'No pending KYC reviews.',
                    ),
                  ],
                );
              }
              return ListView.separated(
                padding: const EdgeInsets.all(20),
                itemCount: items.length,
                separatorBuilder: (_, _) => const SizedBox(height: 8),
                itemBuilder: (context, i) => _queueCard(items[i]),
              );
            },
          ),
        ),
      ),
    );
  }

  Widget _queueCard(KycQueueItem item) {
    final loading = _isLoading(item.userId);
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: NovaColors.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(item.name, style: const TextStyle(fontWeight: FontWeight.w700)),
                    Text(item.email,
                        style: const TextStyle(color: NovaColors.onSurfaceVariant, fontSize: 13)),
                  ],
                ),
              ),
              StatusChip(
                label: 'Tier ${item.tier.isEmpty ? '?' : item.tier}',
                color: NovaColors.warning,
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: PillButton(
                  label: 'Approve',
                  expanded: false,
                  loading: loading,
                  onPressed: loading ? null : () => _approve(item),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: PillButton(
                  label: 'Reject',
                  filled: false,
                  errorColor: true,
                  expanded: false,
                  loading: loading,
                  onPressed: loading ? null : () => _reject(item),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}