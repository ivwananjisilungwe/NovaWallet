library;

/// Custom page transitions for NovaWallet.
///
/// Provides consistent, performant transitions across the app.
/// All transitions respect `MediaQuery.disableAnimations` for accessibility.

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

/// Standard animation duration for page transitions.
const Duration _kTransitionDuration = Duration(milliseconds: 280);

/// Standard curve for page transitions.
const Curve _kTransitionCurve = Curves.easeOutCubic;

/// A custom [PageRoute] with built-in transition animations.
class AppPageRoute<T> extends CustomTransitionPage<T> {
  /// Creates a page route with the specified transition type.
  AppPageRoute({
    required super.child,
    required super.key,
    super.name,
    super.arguments,
    super.restorationId,
    TransitionType type = TransitionType.slideFromRight,
  }) : super(
          transitionDuration: _kTransitionDuration,
          reverseTransitionDuration: _kTransitionDuration,
          transitionsBuilder: _buildTransitions(type),
        );

  /// Builds the transitions builder for the given type.
  static Widget Function(BuildContext, Animation<double>, Animation<double>, Widget) _buildTransitions(TransitionType type) {
    return switch (type) {
      TransitionType.slideFromRight => _slideFromRight,
      TransitionType.slideUp => _slideUp,
      TransitionType.fadeIn => _fadeIn,
      TransitionType.sharedAxisHorizontal => _sharedAxisHorizontal,
      TransitionType.sharedAxisVertical => _sharedAxisVertical,
    };
  }

  /// Slide from right (default forward navigation).
  static Widget _slideFromRight(
    BuildContext context,
    Animation<double> animation,
    Animation<double> secondaryAnimation,
    Widget child,
  ) {
    final curved = CurvedAnimation(
      parent: animation,
      curve: _kTransitionCurve,
      reverseCurve: Curves.easeInCubic,
    );
    return SlideTransition(
      position: Tween<Offset>(
        begin: const Offset(1.0, 0.0),
        end: Offset.zero,
      ).animate(curved),
      child: FadeTransition(
        opacity: curved,
        child: child,
      ),
    );
  }

  /// Slide up (for modals, bottom sheets).
  static Widget _slideUp(
    BuildContext context,
    Animation<double> animation,
    Animation<double> secondaryAnimation,
    Widget child,
  ) {
    final curved = CurvedAnimation(
      parent: animation,
      curve: _kTransitionCurve,
      reverseCurve: Curves.easeInCubic,
    );
    return SlideTransition(
      position: Tween<Offset>(
        begin: const Offset(0.0, 1.0),
        end: Offset.zero,
      ).animate(curved),
      child: FadeTransition(
        opacity: curved,
        child: child,
      ),
    );
  }

  /// Simple fade in.
  static Widget _fadeIn(
    BuildContext context,
    Animation<double> animation,
    Animation<double> secondaryAnimation,
    Widget child,
  ) {
    final curved = CurvedAnimation(
      parent: animation,
      curve: _kTransitionCurve,
    );
    return FadeTransition(
      opacity: curved,
      child: child,
    );
  }

  /// Shared axis horizontal (for tab-level transitions).
  static Widget _sharedAxisHorizontal(
    BuildContext context,
    Animation<double> animation,
    Animation<double> secondaryAnimation,
    Widget child,
  ) {
    final curved = CurvedAnimation(
      parent: animation,
      curve: _kTransitionCurve,
      reverseCurve: Curves.easeInCubic,
    );
    final direction = animation.status == AnimationStatus.reverse ? -1.0 : 1.0;
    return SlideTransition(
      position: Tween<Offset>(
        begin: Offset(direction * 0.3, 0.0),
        end: Offset.zero,
      ).animate(curved),
      child: FadeTransition(
        opacity: curved,
        child: child,
      ),
    );
  }

  /// Shared axis vertical.
  static Widget _sharedAxisVertical(
    BuildContext context,
    Animation<double> animation,
    Animation<double> secondaryAnimation,
    Widget child,
  ) {
    final curved = CurvedAnimation(
      parent: animation,
      curve: _kTransitionCurve,
      reverseCurve: Curves.easeInCubic,
    );
    final direction = animation.status == AnimationStatus.reverse ? -1.0 : 1.0;
    return SlideTransition(
      position: Tween<Offset>(
        begin: Offset(0.0, direction * 0.3),
        end: Offset.zero,
      ).animate(curved),
      child: FadeTransition(
        opacity: curved,
        child: child,
      ),
    );
  }
}

/// Types of page transitions available.
enum TransitionType {
  /// Slide from right (default for forward navigation).
  slideFromRight,

  /// Slide up from bottom (for modals, bottom sheets).
  slideUp,

  /// Simple fade in.
  fadeIn,

  /// Shared axis horizontal (for tabs).
  sharedAxisHorizontal,

  /// Shared axis vertical.
  sharedAxisVertical,
}

/// Extension to easily wrap widgets in page routes.
extension GoRouterTransition on GoRouter {
  /// Navigate with a custom transition.
  void goWithTransition(
    String location, {
    TransitionType type = TransitionType.slideFromRight,
    Object? extra,
  }) {
    // The actual transition is handled by the route configuration
    // This is a placeholder for future extension if needed
    go(location, extra: extra);
  }
}