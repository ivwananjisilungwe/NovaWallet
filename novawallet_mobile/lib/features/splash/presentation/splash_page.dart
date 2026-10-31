import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

// ignore_for_file: unnecessary_underscores

import 'package:novawallet_mobile/app/theme/app_theme.dart';
import 'package:novawallet_mobile/core/design/app_tokens.dart';
import 'package:novawallet_mobile/features/welcome/presentation/welcome_page.dart';
import 'package:novawallet_mobile/shared/widgets/nova_mark.dart';
export 'package:novawallet_mobile/shared/widgets/nova_mark.dart';

/// Screen 01 — animated splash / loading screen.
///
/// Runs a brief, restrained brand intro (mark -> wordmark -> accent line) and
/// then hands off to Screen 02 (the welcome / intro screen). A genuine
/// startup task, if provided, is awaited honestly with an indeterminate
/// indicator; no fake progress is ever shown.
class SplashPage extends StatefulWidget {
  const SplashPage({super.key, this.startupTask});

  /// Optional genuine startup work to complete before leaving the splash.
  ///
  /// When `null` (the current UI-foundation reality) no loading indicator is
  /// shown and navigation happens as soon as the intro finishes.
  final Future<void>? startupTask;

  @override
  State<SplashPage> createState() => _SplashPageState();
}

class _SplashPageState extends State<SplashPage>
    with SingleTickerProviderStateMixin {
  late final AnimationController _introController;
  late final Animation<double> _markScale;
  late final Animation<double> _markOpacity;
  late final Animation<double> _accentWidth;
  late final Animation<double> _accentOpacity;

  bool _didNavigate = false;
  bool _startupWaiting = false;
  bool _reducedMotionHandled = false;

  @override
  void initState() {
    super.initState();
    _introController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 800),
    );

    _markScale = Tween<double>(begin: 0.94, end: 1.00).animate(
      CurvedAnimation(
        parent: _introController,
        curve: const Interval(0.0, 0.55, curve: Curves.easeOutCubic),
      ),
    );
    _markOpacity = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(
        parent: _introController,
        curve: const Interval(0.0, 0.55, curve: Curves.easeOutCubic),
      ),
    );
    _accentWidth = Tween<double>(begin: 0.0, end: 48.0).animate(
      CurvedAnimation(
        parent: _introController,
        curve: const Interval(0.45, 0.90, curve: Curves.easeOut),
      ),
    );
    _accentOpacity = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(
        parent: _introController,
        curve: const Interval(0.45, 0.90, curve: Curves.easeOut),
      ),
    );

    _introController.addStatusListener((status) {
      if (status == AnimationStatus.completed) {
        // Fire-and-forget: navigation happens directly or after genuine
        // startup work. The future is intentionally not awaited here because
        // animation status listeners are synchronous.
        _proceedWhenReady();
      }
    });

    _introController.forward();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_reducedMotionHandled) return;
    if (!MediaQuery.disableAnimationsOf(context)) return;
    _reducedMotionHandled = true;
    // Jump straight to the end state after the first frame, then navigate.
    // Never navigate during build.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      _introController.value = 1.0;
    });
  }

  Future<void> _proceedWhenReady() async {
    if (_didNavigate) return;
    if (widget.startupTask == null) {
      _proceedFromSplash();
      return;
    }
    // Genuine startup work is pending — show an honest indicator while we wait.
    setState(() => _startupWaiting = true);
    await widget.startupTask;
    if (!mounted || _didNavigate) return;
    setState(() => _startupWaiting = false);
    _proceedFromSplash();
  }

  void _proceedFromSplash() {
    if (_didNavigate) return;
    _didNavigate = true;
    // Hand off to Screen 02 — the welcome / intro screen.
    Navigator.of(context).pushReplacement(
      PageRouteBuilder(
        pageBuilder: (_, __, ___) => const WelcomePage(),
        transitionsBuilder: (_, animation, __, child) =>
            FadeTransition(opacity: animation, child: child),
        transitionDuration: const Duration(milliseconds: 250),
      ),
    );
  }

  @override
  void dispose() {
    _introController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final brightness = Theme.of(context).brightness;
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness: brightness == Brightness.dark
            ? Brightness.light
            : Brightness.dark,
        statusBarBrightness: brightness == Brightness.dark
            ? Brightness.dark
            : Brightness.light,
      ),
      child: Scaffold(
        body: SafeArea(
          child: Column(
            children: [
              Expanded(
                child: Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      FadeTransition(
                        opacity: _markOpacity,
                        child: ScaleTransition(
                          scale: _markScale,
                          child: Semantics(
                            image: true,
                            label: 'NovaWallet logo',
                            child: Container(
                              color: AppTheme.midnight,
                              padding: const EdgeInsets.all(AppTokens.space8),
                              child: Image.asset(
                                'assets/branding/01_novawallet_logo.png',
                                width: 240,
                                height: 320,
                                fit: BoxFit.contain,
                                errorBuilder: (_, _, _) => const NovaMark(
                                  size: 160,
                                  color: AppTheme.electricBlue,
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: AppTokens.space16),
                      AnimatedBuilder(
                        animation: _introController,
                        builder: (_, __) {
                          return Opacity(
                            opacity: _accentOpacity.value,
                            child: SizedBox(
                              width: _accentWidth.value,
                              height: 4,
                              child: DecoratedBox(
                                decoration: BoxDecoration(
                                  color: AppTheme.electricBlue,
                                  borderRadius: BorderRadius.circular(2),
                                ),
                              ),
                            ),
                          );
                        },
                      ),
                    ],
                  ),
                ),
              ),
              if (_startupWaiting)
                Padding(
                  padding: const EdgeInsets.only(bottom: AppTokens.space24),
                  child: SizedBox(
                    width: 160,
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(2),
                      child: LinearProgressIndicator(
                        minHeight: 3,
                        valueColor: AlwaysStoppedAnimation(
                          AppTheme.electricBlue,
                        ),
                        backgroundColor: AppTheme.electricBlue.withValues(
                          alpha: 0.25,
                        ),
                      ),
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
