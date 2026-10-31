// ignore_for_file: unnecessary_underscores

import 'package:flutter/material.dart';

import '../../app/theme/app_theme.dart';

/// Geometric "N" brand mark, drawn programmatically.
///
/// Decorative only — excluded from semantics. Shared across splash and welcome
/// screens so the mark is drawn from a single source of truth.
class NovaMark extends StatelessWidget {
  const NovaMark({
    super.key,
    this.size = 100,
    this.color = AppTheme.electricBlue,
  });

  final double size;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return ExcludeSemantics(
      child: SizedBox(
        width: size,
        height: size,
        child: CustomPaint(painter: _NMarkPainter(color: color)),
      ),
    );
  }
}

class _NMarkPainter extends CustomPainter {
  _NMarkPainter({required this.color});
  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = size.width * 0.10
      ..strokeCap = StrokeCap.round;
    final pad = size.width * 0.22;
    // Left stem.
    canvas.drawLine(Offset(pad, pad), Offset(pad, size.height - pad), paint);
    // Right stem.
    canvas.drawLine(
      Offset(size.width - pad, pad),
      Offset(size.width - pad, size.height - pad),
      paint,
    );
    // Diagonal.
    canvas.drawLine(
      Offset(pad, pad),
      Offset(size.width - pad, size.height - pad),
      paint,
    );
  }

  @override
  bool shouldRepaint(covariant _NMarkPainter old) => old.color != color;
}
