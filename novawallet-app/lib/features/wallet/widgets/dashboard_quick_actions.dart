import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../core/theme/design_tokens.dart';

/// Quick actions grid.
class DashboardQuickActions extends StatelessWidget {
  const DashboardQuickActions({
    super.key,
    required this.onSend,
    required this.onRequest,
    required this.onAirtime,
    required this.onBills,
  });

  final VoidCallback onSend;
  final VoidCallback onRequest;
  final VoidCallback onAirtime;
  final VoidCallback onBills;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final actions = [
      _ActionData('Send', Icons.send_outlined, scheme.primary, onSend),
      _ActionData(
        'Request',
        Icons.call_received_outlined,
        scheme.secondary,
        onRequest,
      ),
      // Amber passes ≥ 4.5:1 on dark surfaces as-is, so it stays the same
      // semantic token in both themes.
      _ActionData(
        'Airtime',
        Icons.phone_android_outlined,
        SemanticColors.warning,
        onAirtime,
      ),
      _ActionData(
        'Bills',
        Icons.receipt_long_outlined,
        scheme.secondary,
        onBills,
      ),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Quick Actions',
          style: Theme.of(
            context,
          ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 16),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: actions.map((action) {
            return _AnimatedAction(action: action);
          }).toList(),
        ),
      ],
    );
  }
}

class _ActionData {
  const _ActionData(this.label, this.icon, this.color, this.onTap);

  final String label;
  final IconData icon;
  final Color color;
  final VoidCallback onTap;
}

class _AnimatedAction extends StatefulWidget {
  const _AnimatedAction({required this.action});

  final _ActionData action;

  @override
  State<_AnimatedAction> createState() => _AnimatedActionState();
}

class _AnimatedActionState extends State<_AnimatedAction>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final Animation<double> _scaleAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(milliseconds: 150),
      vsync: this,
    );
    _scaleAnimation = Tween<double>(begin: 1.0, end: 0.85).animate(
      CurvedAnimation(
        parent: _controller,
        curve: Curves.easeOutCubic,
        reverseCurve: Curves.easeOutCubic,
      ),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _handleTapDown(TapDownDetails details) {
    _controller.forward();
  }

  void _handleTapUp(TapUpDetails details) {
    _controller.reverse();
    widget.action.onTap();
    HapticFeedback.lightImpact();
  }

  void _handleTapCancel() {
    _controller.reverse();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _scaleAnimation,
      builder: (context, child) {
        return Transform.scale(
          scale: _scaleAnimation.value,
          child: GestureDetector(
            onTapDown: _handleTapDown,
            onTapUp: _handleTapUp,
            onTapCancel: _handleTapCancel,
            child: child!,
          ),
        );
      },
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(
              color: widget.action.color.withValues(alpha: 0.15),
              shape: BoxShape.circle,
            ),
            child: Icon(
              widget.action.icon,
              color: widget.action.color,
              size: 24,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            widget.action.label,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: Theme.of(context).colorScheme.onSurface,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }
}
