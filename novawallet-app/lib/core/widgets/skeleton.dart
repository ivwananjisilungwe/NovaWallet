library;

/// Skeleton loaders for NovaWallet.
///
/// Provides shimmer-based placeholder animations for perceived performance
/// while content is loading. All animations respect `MediaQuery.disableAnimations`.

import 'package:flutter/material.dart';

/// A shimmer-based skeleton loader for content placeholders.
class SkeletonLoader extends StatefulWidget {
  const SkeletonLoader({
    super.key,
    required this.child,
    this.enabled = true,
    this.baseColor,
    this.highlightColor,
    this.period = const Duration(milliseconds: 1500),
  });

  /// The shape to render as a skeleton.
  final Widget child;

  /// Whether the shimmer animation is active.
  final bool enabled;

  /// Base color of the skeleton
  /// (default: [ColorScheme.surfaceContainerHighest]).
  final Color? baseColor;

  /// Highlight color of the shimmer
  /// (default: [ColorScheme.surfaceContainerHigh]).
  final Color? highlightColor;

  /// Duration of one shimmer cycle.
  final Duration period;

  @override
  State<SkeletonLoader> createState() => _SkeletonLoaderState();
}

class _SkeletonLoaderState extends State<SkeletonLoader>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(duration: widget.period, vsync: this)
      ..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!widget.enabled) {
      return widget.child;
    }

    // Theme-driven defaults: light mode resolves to the canonical slate-tinted
    // placeholders, dark mode to the elevated dark container tiers so the
    // shimmer sweep stays visible against dark surfaces.
    final scheme = Theme.of(context).colorScheme;
    final baseColor = widget.baseColor ?? scheme.surfaceContainerHighest;
    final highlightColor = widget.highlightColor ?? scheme.surfaceContainerHigh;

    return AnimatedBuilder(
      animation: _controller,
      builder: (context, child) {
        return ShaderMask(
          blendMode: BlendMode.srcATop,
          shaderCallback: (bounds) {
            return LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [baseColor, highlightColor, baseColor],
              stops: const [0.0, 0.5, 1.0],
              transform: _SlidingGradientTransform(
                slidePercent: _controller.value,
              ),
            ).createShader(bounds);
          },
          child: widget.child,
        );
      },
    );
  }
}

/// Transform for sliding the gradient across the skeleton.
class _SlidingGradientTransform extends GradientTransform {
  const _SlidingGradientTransform({required this.slidePercent});

  final double slidePercent;

  @override
  Matrix4? transform(Rect bounds, {TextDirection? textDirection}) {
    final double width = bounds.width;
    final double translate = width * slidePercent * 2 - width;
    return Matrix4.translationValues(translate, 0.0, 0.0);
  }
}

/// A rectangular skeleton placeholder.
class SkeletonRect extends StatelessWidget {
  const SkeletonRect({
    super.key,
    this.width,
    this.height,
    this.borderRadius = 8,
    this.enabled = true,
  });

  final double? width;
  final double? height;
  final double borderRadius;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    return SkeletonLoader(
      enabled: enabled,
      child: Container(
        width: width,
        height: height,
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(borderRadius),
        ),
      ),
    );
  }
}

/// A circular skeleton placeholder (for avatars).
class SkeletonCircle extends StatelessWidget {
  const SkeletonCircle({super.key, this.size = 40, this.enabled = true});

  final double size;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    return SkeletonLoader(
      enabled: enabled,
      child: Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surfaceContainerHighest,
          shape: BoxShape.circle,
        ),
      ),
    );
  }
}

/// A text-like skeleton placeholder.
class SkeletonText extends StatelessWidget {
  const SkeletonText({
    super.key,
    this.lines = 1,
    this.width,
    this.lineHeight = 16,
    this.spacing = 8,
    this.enabled = true,
  });

  final int lines;
  final double? width;
  final double lineHeight;
  final double spacing;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: List.generate(lines, (index) {
        final isLast = index == lines - 1;
        final lineWidth = isLast && width != null
            ? width! *
                  0.6 // Last line shorter
            : width;
        return Padding(
          padding: EdgeInsets.only(bottom: isLast ? 0 : spacing),
          child: SkeletonRect(
            width: lineWidth,
            height: lineHeight,
            borderRadius: 4,
            enabled: enabled,
          ),
        );
      }),
    );
  }
}

/// Balance card skeleton.
class BalanceCardSkeleton extends StatelessWidget {
  const BalanceCardSkeleton({super.key});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [scheme.surfaceContainerHighest, scheme.surfaceContainerHigh],
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Greeting + avatar skeleton
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              SkeletonText(lines: 1, width: 120, lineHeight: 20),
              SkeletonCircle(size: 40),
            ],
          ),
          const SizedBox(height: 24),
          // Balance amount skeleton
          SkeletonRect(width: 160, height: 40, borderRadius: 8),
          const SizedBox(height: 8),
          SkeletonRect(width: 100, height: 16, borderRadius: 4),
          const SizedBox(height: 24),
          // Action buttons skeleton
          Row(
            children: [
              Expanded(child: SkeletonRect(height: 48, borderRadius: 16)),
              const SizedBox(width: 12),
              Expanded(child: SkeletonRect(height: 48, borderRadius: 16)),
            ],
          ),
        ],
      ),
    );
  }
}

/// Quick actions skeleton.
class QuickActionsSkeleton extends StatelessWidget {
  const QuickActionsSkeleton({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SkeletonText(lines: 1, width: 100, lineHeight: 20),
        const SizedBox(height: 16),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: List.generate(4, (index) {
            return Column(
              children: [
                SkeletonCircle(size: 56),
                const SizedBox(height: 8),
                SkeletonRect(width: 60, height: 14, borderRadius: 4),
              ],
            );
          }),
        ),
      ],
    );
  }
}

/// Transaction list skeleton.
class TransactionListSkeleton extends StatelessWidget {
  const TransactionListSkeleton({super.key, this.itemCount = 5});

  final int itemCount;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            SkeletonText(lines: 1, width: 120, lineHeight: 20),
            SkeletonText(lines: 1, width: 80, lineHeight: 16),
          ],
        ),
        const SizedBox(height: 16),
        ...List.generate(itemCount, (index) {
          return Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Row(
              children: [
                SkeletonCircle(size: 44),
                const SizedBox(width: 12),
                Expanded(
                  child: SkeletonText(lines: 2, lineHeight: 16, spacing: 4),
                ),
                SkeletonRect(width: 70, height: 20, borderRadius: 4),
              ],
            ),
          );
        }),
      ],
    );
  }
}

/// Full dashboard skeleton.
class DashboardSkeleton extends StatelessWidget {
  const DashboardSkeleton({super.key});

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const BalanceCardSkeleton(),
          const SizedBox(height: 24),
          const QuickActionsSkeleton(),
          const SizedBox(height: 24),
          const TransactionListSkeleton(),
        ],
      ),
    );
  }
}
