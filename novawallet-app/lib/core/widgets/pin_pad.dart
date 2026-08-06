import 'package:flutter/material.dart';

import '../theme/nova_colors.dart';

/// Numeric PIN pad with live dots, used by set-PIN, transfer confirm, withdraw
/// and lockout flows. Emits the full PIN via [onComplete].
class PinPad extends StatefulWidget {
  const PinPad({
    super.key,
    required this.length,
    required this.onComplete,
    this.confirmMode = false,
  });

  /// Number of digits (4-6).
  final int length;
  final bool confirmMode;

  /// Called with the whole PIN when [length] digits are entered.
  final ValueChanged<String> onComplete;

  @override
  State<PinPad> createState() => _PinPadState();
}

class _PinPadState extends State<PinPad> {
  final List<String> _digits = [];

  bool get _isFilled => _digits.length >= widget.length;

  void _press(String d) {
    if (_isFilled) return;
    setState(() => _digits.add(d));
    if (_isFilled) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        widget.onComplete(_digits.join());
      });
    }
  }

  void _back() {
    if (_digits.isEmpty) return;
    setState(() => _digits.removeLast());
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            for (var i = 0; i < widget.length; i++) ...[
              _dot(_digits.length > i),
              if (i != widget.length - 1) const SizedBox(width: 20),
            ],
          ],
        ),
        const SizedBox(height: 32),
        _keypad(),
      ],
    );
  }

  Widget _dot(bool filled) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 120),
      width: 16,
      height: 16,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: filled ? NovaColors.primary : NovaColors.surfaceContainerHighest,
      ),
    );
  }

  Widget _keypad() {
    const keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '', '0', '⌫'];
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        for (var row = 0; row < 4; row++) ...[
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              for (var col = 0; col < 3; col++) _key(keys[row * 3 + col]),
            ],
          ),
          if (row != 3) const SizedBox(height: 8),
        ],
      ],
    );
  }

  Widget _key(String label) {
    return SizedBox(
      width: 84,
      height: 62,
      child: label.isEmpty
          ? const SizedBox.shrink()
          : TextButton(
              onPressed: label == '⌫' ? _back : () => _press(label),
              style: TextButton.styleFrom(
                foregroundColor: NovaColors.onSurface,
                shape: const StadiumBorder(),
                backgroundColor: NovaColors.surfaceContainerLow,
              ),
              child: Text(
                label,
                style: const TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
    );
  }
}
