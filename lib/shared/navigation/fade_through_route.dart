import 'package:flutter/material.dart';

/// Short, restrained transition used between the onboarding screens
/// (welcome → login / registration, login → registration).
///
/// A brief fade with a very small upward drift: long enough to read as a
/// deliberate change of context, short enough that it never delays the user.
/// When the platform's "remove animations" accessibility preference is on, the
/// route becomes an instant swap, so nothing is withheld from users who have
/// asked for less motion.
class FadeThroughRoute<T> extends PageRouteBuilder<T> {
  FadeThroughRoute({required WidgetBuilder builder, super.settings})
    : super(
        pageBuilder: (context, animation, secondaryAnimation) =>
            builder(context),
        transitionDuration: const Duration(milliseconds: 280),
        reverseTransitionDuration: const Duration(milliseconds: 220),
        transitionsBuilder: (context, animation, secondaryAnimation, child) {
          if (MediaQuery.disableAnimationsOf(context)) {
            return child;
          }
          final curved = CurvedAnimation(
            parent: animation,
            curve: Curves.easeOutCubic,
            reverseCurve: Curves.easeInCubic,
          );
          return FadeTransition(
            opacity: curved,
            child: SlideTransition(
              position: Tween<Offset>(
                begin: const Offset(0, 0.02),
                end: Offset.zero,
              ).animate(curved),
              child: child,
            ),
          );
        },
      );
}
