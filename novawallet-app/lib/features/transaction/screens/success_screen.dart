import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../core/theme/nova_colors.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/widgets.dart';

/// Success state (per design 15) — shown after a deposit, transfer or withdraw.
/// Features an animated checkmark that draws itself on entry.
class SuccessScreen extends StatefulWidget {
  const SuccessScreen({
    super.key,
    this.title = 'Done!',
    this.amount,
    this.reference,
    this.onDone,
  });

  final String title;
  final String? amount;
  final String? reference;

  /// Optional custom callback when "Done" is tapped. Falls back to popping
  /// all routes to the first screen if null.
  final VoidCallback? onDone;

  @override
  State<SuccessScreen> createState() => _SuccessScreenState();
}

class _SuccessScreenState extends State<SuccessScreen>
    with SingleTickerProviderStateMixin {
  late final AnimationController _drawController;
  late final Animation<double> _drawProgress;
  late final Animation<double> _scaleProgress;
  late final Animation<double> _fadeProgress;

  @override
  void initState() {
    super.initState();

    _drawController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    );

    // Checkmark stroke draw: 0 → 1 over first 600ms.
    _drawProgress = Tween<double>(begin: 0, end: 1).animate(
      CurvedAnimation(
        parent: _drawController,
        curve: const Interval(0.0, 0.5, curve: Curves.easeOutCubic),
      ),
    );

    // Circle scale: bounces in from 0 → 1 with overshoot.
    _scaleProgress = Tween<double>(begin: 0, end: 1).animate(
      CurvedAnimation(
        parent: _drawController,
        curve: const Interval(0.0, 0.5, curve: Curves.easeOutBack),
      ),
    );

    // Content fade: appears after circle.
    _fadeProgress = Tween<double>(begin: 0, end: 1).animate(
      CurvedAnimation(
        parent: _drawController,
        curve: const Interval(0.35, 0.7, curve: Curves.easeOut),
      ),
    );

    // Fire haptic + start animation on first frame.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      HapticFeedback.mediumImpact();
      _drawController.forward();
    });
  }

  @override
  void dispose() {
    _drawController.dispose();
    super.dispose();
  }

  void _handleDone() {
    HapticFeedback.lightImpact();
    if (widget.onDone != null) {
      widget.onDone!();
    } else {
      Navigator.of(context).popUntil((route) => route.isFirst);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NovaColors.background,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Animated checkmark circle.
              Center(
                child: AnimatedBuilder(
                  animation: _drawController,
                  builder: (context, _) {
                    return Transform.scale(
                      scale: _scaleProgress.value,
                      child: SizedBox(
                        width: 96,
                        height: 96,
                        child: CustomPaint(
                          painter: _CheckmarkPainter(
                            progress: _drawProgress.value,
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ),
              const SizedBox(height: 24),

              // Title + amount + reference fade in after circle.
              FadeTransition(
                opacity: _fadeProgress,
                child: Column(
                  children: [
                    Text(
                      widget.title,
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontSize: 26,
                        fontWeight: FontWeight.w700,
                        color: NovaColors.onBackground,
                      ),
                    ),
                    if (widget.amount != null) ...[
                      const SizedBox(height: 12),
                      Text(
                        formatZmw(widget.amount),
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          fontSize: 32,
                          fontWeight: FontWeight.w700,
                          color: NovaColors.success,
                        ),
                      ),
                    ],
                    if (widget.reference != null) ...[
                      const SizedBox(height: 8),
                      Text(
                        'Ref: ${widget.reference}',
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          color: NovaColors.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(height: 40),

              // Action buttons.
              FadeTransition(
                opacity: _fadeProgress,
                child: PillButton(
                  label: 'Back to Home',
                  onPressed: _handleDone,
                ),
              ),
              const SizedBox(height: 12),
              FadeTransition(
                opacity: _fadeProgress,
                child: PillButton(
                  label: 'Done',
                  variant: PillButtonVariant.outlined,
                  onPressed: _handleDone,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Custom painter that draws a circle + animated checkmark stroke.
class _CheckmarkPainter extends CustomPainter {
  _CheckmarkPainter({required this.progress});

  /// 0 = nothing drawn, 1 = fully drawn circle + checkmark.
  final double progress;

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = size.width / 2;

    // --- Background circle (fills first 40% of progress) ---
    final circlePaint = Paint()
      ..color = NovaColors.secondaryContainer
      ..style = PaintingStyle.fill;

    final circleRadius = radius * _easeOut(_stagger(progress, 0, 0.4));
    canvas.drawCircle(center, circleRadius, circlePaint);

    // --- Checkmark stroke (draws from 30% to 100% of progress) ---
    final checkProgress = _clampUnit((progress - 0.3) / 0.7);
    if (checkProgress <= 0) return;

    final strokePaint = Paint()
      ..color = NovaColors.onSecondaryContainer
      ..strokeWidth = 5.0
      ..strokeCap = StrokeCap.round
      ..style = PaintingStyle.stroke;

    // Checkmark path: two lines forming a check.
    final start = Offset(size.width * 0.28, size.height * 0.50);
    final mid = Offset(size.width * 0.44, size.height * 0.65);
    final end = Offset(size.width * 0.72, size.height * 0.35);

    final path = Path()
      ..moveTo(start.dx, start.dy)
      ..lineTo(mid.dx, mid.dy)
      ..lineTo(end.dx, end.dy);

    final metrics = path.computeMetrics().first;
    final extractedPath = metrics.extractPath(
      0,
      metrics.length * checkProgress,
    );
    canvas.drawPath(extractedPath, strokePaint);
  }

  /// Clamp a value to [0, 1].
  static double _clampUnit(double v) => v.clamp(0.0, 1.0);

  /// Simple ease-out quad.
  static double _easeOut(double t) => 1 - (1 - t) * (1 - t);

  @override
  bool shouldRepaint(_CheckmarkPainter old) => old.progress != progress;
}

/// Maps [value] from [start..end] into 0..1 range for staggered animations.
double _stagger(double value, double start, double end) {
  if (value <= start) return 0;
  if (value >= end) return 1;
  return (value - start) / (end - start);
}
