import 'package:flutter/material.dart';

import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/widgets.dart';

/// Notification inbox (per design 26). Static sample list for now; the backend
/// notification inbox is returned by the notification service.
class NotificationsScreen extends StatelessWidget {
  const NotificationsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final samples = [
      _note(Icons.savings, NovaColors.success, 'Your deposit of ZMW 100.00 was successful', 'Today, 09:05'),
      _note(Icons.verified_outlined, NovaColors.primary, 'Your identity documents are under review', 'Yesterday, 15:20'),
      _note(Icons.info_outline, NovaColors.warning, 'New security tip: keep your PIN private', 'Mon'),
    ];

    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(
        title: const Text('Notifications'),
        actions: [
          TextButton(onPressed: () {}, child: const Text('Mark all read')),
        ],
      ),
      body: SafeArea(
        child: samples.isEmpty
            ? const EmptyStateView(
                icon: Icons.notifications_none,
                title: 'Nothing here yet',
                message: 'We\'ll notify you of activity on your wallet.',
              )
            : ListView.separated(
                padding: const EdgeInsets.all(20),
                itemCount: samples.length,
                separatorBuilder: (_, _) => const SizedBox(height: 8),
                itemBuilder: (context, i) => samples[i],
              ),
      ),
    );
  }

  Widget _note(IconData icon, Color color, String text, String time) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: NovaColors.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          Container(
            width: 42,
            height: 42,
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.12),
              shape: BoxShape.circle,
            ),
            child: Icon(icon, color: color, size: 22),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(text, style: const TextStyle(fontWeight: FontWeight.w600, height: 1.3)),
                const SizedBox(height: 4),
                Text(time, style: const TextStyle(color: NovaColors.onSurfaceVariant, fontSize: 12)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
