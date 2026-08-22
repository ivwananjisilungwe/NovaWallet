import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/nova_colors.dart';
import '../theme/design_tokens.dart';

/// Pill-shaped button matching DESIGN.md primary buttons.
///
/// Variants:
/// - **filled** (default): solid indigo background, white label.
/// - **outlined**: ghost style with indigo border and text.
/// - **text**: label-only, no background or border.
///
/// Accessibility:
/// - Minimum touch target 56×56 dp (exceeds 48 dp WCAG AA).
/// - [Semantics] label is set for screen readers.
/// - Loading state announces "Loading" to assistive tech.
class PillButton extends StatefulWidget {
  const PillButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.variant = PillButtonVariant.filled,
    this.loading = false,
    this.expanded = true,
    this.errorColor = false,
    this.semanticsLabel,
  });

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;

  /// Visual variant of the button.
  final PillButtonVariant variant;

  /// Show a small spinner instead of the label while [loading].
  final bool loading;

  /// Fill the available horizontal space.
  final bool expanded;

  /// Tint the button with [NovaColors.error] instead of the primary color.
  final bool errorColor;

  /// Override for screen-reader label (defaults to [label]).
  final String? semanticsLabel;

  @override
  State<PillButton> createState() => _PillButtonState();
}

class _PillButtonState extends State<PillButton> with SingleTickerProviderStateMixin {
  late final AnimationController _scaleController;
  late final Animation<double> _scaleAnimation;
  bool _isPressed = false;

  @override
  void initState() {
    super.initState();
    _scaleController = AnimationController(
      duration: const Duration(milliseconds: 100),
      vsync: this,
    );
    _scaleAnimation = Tween<double>(
      begin: 1.0,
      end: 0.95,
    ).animate(CurvedAnimation(
      parent: _scaleController,
      curve: Curves.easeOutCubic,
      reverseCurve: Curves.easeOutCubic,
    ));
  }

  @override
  void dispose() {
    _scaleController.dispose();
    super.dispose();
  }

  void _handleTapDown(TapDownDetails details) {
    if (widget.onPressed != null && !widget.loading) {
      setState(() => _isPressed = true);
      _scaleController.forward();
    }
  }

  void _handleTapUp(TapUpDetails details) {
    if (_isPressed) {
      setState(() => _isPressed = false);
      _scaleController.reverse();
    }
  }

  void _handleTapCancel() {
    if (_isPressed) {
      setState(() => _isPressed = false);
      _scaleController.reverse();
    }
  }

  void _handleOnPressed() {
    if (widget.onPressed != null && !widget.loading) {
      HapticFeedback.lightImpact();
      widget.onPressed!();
    }
  }

  @override
  Widget build(BuildContext context) {
    final effectiveLabel = widget.semanticsLabel ?? widget.label;
    final isDisabled = widget.onPressed == null || widget.loading;

    final child = AnimatedBuilder(
      animation: _scaleAnimation,
      builder: (context, _) {
        return Transform.scale(
          scale: _scaleAnimation.value,
          child: _buildChild(context),
        );
      },
    );

    final buttonWidget = switch (widget.variant) {
      PillButtonVariant.filled => FilledButton(
        onPressed: isDisabled ? null : _handleOnPressed,
        style: FilledButton.styleFrom(
          minimumSize: widget.expanded
              ? const Size.fromHeight(ComponentTokens.pillButtonMinHeight)
              : const Size(48, 48),
          padding: ComponentTokens.pillButtonPadding,
          shape: ComponentTokens.pillButtonShape,
          backgroundColor: widget.errorColor ? NovaColors.error : NovaColors.primary,
          foregroundColor: NovaColors.onPrimary,
          disabledBackgroundColor: widget.errorColor
              ? NovaColors.error.withValues(alpha: 0.38)
              : NovaColors.primary.withValues(alpha: 0.38),
          disabledForegroundColor: NovaColors.onPrimary.withValues(alpha: 0.38),
          overlayColor: widget.errorColor
              ? NovaColors.onError.withValues(alpha: 0.1)
              : NovaColors.onPrimary.withValues(alpha: 0.1),
        ),
        child: child,
      ),
      PillButtonVariant.outlined => OutlinedButton(
        onPressed: isDisabled ? null : _handleOnPressed,
        style: OutlinedButton.styleFrom(
          minimumSize: widget.expanded
              ? const Size.fromHeight(ComponentTokens.pillButtonMinHeight)
              : const Size(48, 48),
          padding: ComponentTokens.pillButtonPadding,
          shape: ComponentTokens.pillButtonShape,
          foregroundColor: widget.errorColor ? NovaColors.error : NovaColors.primary,
          side: BorderSide(
            color: widget.errorColor ? NovaColors.error : NovaColors.primary,
            width: 1.5,
          ),
          disabledForegroundColor: widget.errorColor
              ? NovaColors.error.withValues(alpha: 0.38)
              : NovaColors.primary.withValues(alpha: 0.38),
          overlayColor: widget.errorColor
              ? NovaColors.error.withValues(alpha: 0.1)
              : NovaColors.primary.withValues(alpha: 0.1),
        ),
        child: child,
      ),
      PillButtonVariant.text => TextButton(
        onPressed: isDisabled ? null : _handleOnPressed,
        style: TextButton.styleFrom(
          minimumSize: widget.expanded
              ? const Size.fromHeight(ComponentTokens.pillButtonMinHeight)
              : const Size(48, 48),
          padding: ComponentTokens.pillButtonPadding,
          shape: ComponentTokens.pillButtonShape,
          foregroundColor: widget.errorColor ? NovaColors.error : NovaColors.primary,
          disabledForegroundColor: widget.errorColor
              ? NovaColors.error.withValues(alpha: 0.38)
              : NovaColors.primary.withValues(alpha: 0.38),
          overlayColor: widget.errorColor
              ? NovaColors.error.withValues(alpha: 0.1)
              : NovaColors.primary.withValues(alpha: 0.1),
        ),
        child: child,
      ),
    };

    return Semantics(
      button: true,
      enabled: !isDisabled,
      label: widget.loading ? 'Loading, $effectiveLabel' : effectiveLabel,
      child: GestureDetector(
        onTapDown: _handleTapDown,
        onTapUp: _handleTapUp,
        onTapCancel: _handleTapCancel,
        child: buttonWidget,
      ),
    );
  }

  Widget _buildChild(BuildContext context) {
    if (widget.loading) {
      return const SizedBox(
        width: 20,
        height: 20,
        child: CircularProgressIndicator(
          strokeWidth: 2,
          valueColor: AlwaysStoppedAnimation<Color>(NovaColors.onPrimary),
        ),
      );
    }

    if (widget.icon != null) {
      return Row(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(widget.icon, size: 20),
          const SizedBox(width: 8),
          Text(
            widget.label,
            style: const TextStyle(
              fontWeight: FontWeight.w600,
              fontSize: 16,
            ),
          ),
        ],
      );
    }

    return Text(
      widget.label,
      style: const TextStyle(
        fontWeight: FontWeight.w600,
        fontSize: 16,
      ),
    );
  }
}

/// Visual variants for [PillButton].
enum PillButtonVariant {
  /// Solid background with primary color.
  filled,

  /// Outlined with primary color border.
  outlined,

  /// Text only, no background or border.
  text,
}