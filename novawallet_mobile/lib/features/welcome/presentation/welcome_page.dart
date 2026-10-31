import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:novawallet_mobile/app/theme/app_theme.dart';
import 'package:novawallet_mobile/core/design/app_tokens.dart';
import 'package:novawallet_mobile/features/auth/presentation/login_page.dart';
import 'package:novawallet_mobile/features/auth/presentation/register_page.dart';
import 'package:novawallet_mobile/shared/navigation/fade_through_route.dart';
import 'package:novawallet_mobile/shared/widgets/nova_mark.dart';

/// Screen 02 — Welcome / Intro screen.
///
/// Premium fintech onboarding: brand header, Midnight hero panel carrying the
/// supplied wallet artwork, clear value proposition, and primary/secondary
/// CTAs. The viewport fills on tall screens, anchors the CTAs toward the
/// bottom, and scrolls on short screens. Entrance sequencing reuses the shared
/// motion tokens and reduced-motion preference lands on the final composition
/// in the first frame; navigation never waits for animation.
class WelcomePage extends StatefulWidget {
  const WelcomePage({super.key});

  @override
  State<WelcomePage> createState() => _WelcomePageState();
}

class _WelcomePageState extends State<WelcomePage>
    with SingleTickerProviderStateMixin {
  /// Number of shared stagger steps between the sequenced entrances
  /// (brand, hero, headline, body, CTAs).
  static const int _motionSteps = 4;

  static const int _entranceTotalMs =
      AppTokens.animationEntranceMs +
      AppTokens.animationStaggerMs * _motionSteps;

  /// Welcome-specific composition sizes; not shared design tokens.
  static const double _heroMaxSide = 240;
  static const double _heroHeightFraction = 0.30;

  late final AnimationController _controller;
  late final Animation<double> _brandOpacity;
  late final Animation<double> _heroOpacity;
  late final Animation<Offset> _heroSlide;
  late final Animation<double> _headlineOpacity;
  late final Animation<double> _bodyOpacity;
  late final Animation<double> _ctaOpacity;

  bool _motionStarted = false;
  bool _didNavigateToRegister = false;
  bool _didNavigateToLogin = false;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: _entranceTotalMs),
    );

    _brandOpacity = _fadeAtStep(0, Curves.easeOut);
    _heroOpacity = _fadeAtStep(1, Curves.easeOut);
    _heroSlide = Tween<Offset>(begin: const Offset(0, 0.08), end: Offset.zero)
        .animate(
          CurvedAnimation(
            parent: _controller,
            curve: _entryInterval(1, Curves.easeOutCubic),
          ),
        );
    _headlineOpacity = _fadeAtStep(2, Curves.easeOut);
    _bodyOpacity = _fadeAtStep(3, Curves.easeOut);
    _ctaOpacity = _fadeAtStep(4, Curves.easeOut);
  }

  Interval _entryInterval(int step, Curve curve) {
    final int startMs = AppTokens.animationStaggerMs * step;
    final int endMs = startMs + AppTokens.animationEntranceMs;
    return Interval(
      startMs / _entranceTotalMs,
      endMs / _entranceTotalMs,
      curve: curve,
    );
  }

  Animation<double> _fadeAtStep(int step, Curve curve) =>
      Tween<double>(begin: 0.0, end: 1.0).animate(
        CurvedAnimation(
          parent: _controller,
          curve: _entryInterval(step, curve),
        ),
      );

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_motionStarted) return;
    _motionStarted = true;
    if (MediaQuery.disableAnimationsOf(context)) {
      // Reduced motion: land on the final composition in the first frame;
      // the controller never runs, so actions are available immediately.
      _controller.value = 1.0;
    } else {
      _controller.forward();
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _handleGetStarted() async {
    if (_didNavigateToRegister) return;
    _didNavigateToRegister = true;

    await Navigator.of(
      context,
    ).push<void>(FadeThroughRoute<void>(builder: (_) => const RegisterPage()));
    if (mounted) setState(() => _didNavigateToRegister = false);
  }

  Future<void> _handleSignIn() async {
    if (_didNavigateToLogin) return;
    _didNavigateToLogin = true;

    await Navigator.of(context)
        .push<void>(FadeThroughRoute<void>(builder: (_) => const LoginPage()));
    if (mounted) setState(() => _didNavigateToLogin = false);
  }

  /// Hero panel side that fits both the available width and a restrained
  /// share of the viewport height, shrinking on short screens before the
  /// headline or CTAs are sacrificed.
  static double _heroSideFor(BoxConstraints constraints) {
    final double widthLimit = math.max(
      0.0,
      constraints.maxWidth - AppTokens.space24 * 2,
    );
    final double heightTarget = math.max(
      96.0,
      constraints.maxHeight * _heroHeightFraction,
    );
    return math.min(_heroMaxSide, math.min(heightTarget, widthLimit));
  }

  @override
  Widget build(BuildContext context) {
    final brightness = Theme.of(context).brightness;
    final colorScheme = Theme.of(context).colorScheme;

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
          child: LayoutBuilder(
            builder: (context, constraints) {
              final double heroSide = _heroSideFor(constraints);

              return SingleChildScrollView(
                // Tablet / desktop-window widths constrain the composition to
                // the shared content measure so the CTAs and copy do not
                // stretch across the whole viewport.
                child: Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(
                      maxWidth: AppTokens.contentMaxWidth,
                    ),
                    child: ConstrainedBox(
                      constraints: BoxConstraints(
                        minHeight: constraints.maxHeight,
                      ),
                      child: Padding(
                        padding: const EdgeInsets.fromLTRB(
                          AppTokens.space24,
                          0,
                          AppTokens.space24,
                          AppTokens.space32,
                        ),
                        child: IntrinsicHeight(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.center,
                            children: [
                              const SizedBox(height: AppTokens.space24),

                              // Brand header: small mark + wordmark.
                              FadeTransition(
                                opacity: _brandOpacity,
                                child: const _BrandHeader(),
                              ),
                              const SizedBox(height: AppTokens.space32),

                              // Hero illustration on the Midnight brand panel.
                              FadeTransition(
                                opacity: _heroOpacity,
                                child: SlideTransition(
                                  position: _heroSlide,
                                  child: _HeroPanel(side: heroSide),
                                ),
                              ),
                              const SizedBox(height: AppTokens.space32),

                              // Headline.
                              FadeTransition(
                                opacity: _headlineOpacity,
                                child: Semantics(
                                  header: true,
                                  child: Text(
                                    'Your money. Your control.',
                                    style: Theme.of(context)
                                        .textTheme
                                        .headlineLarge
                                        ?.copyWith(height: 1.15),
                                    textAlign: TextAlign.center,
                                  ),
                                ),
                              ),
                              const SizedBox(height: AppTokens.space16),

                              // Supporting copy.
                              FadeTransition(
                                opacity: _bodyOpacity,
                                child: Text(
                                  'A simpler, smarter way to manage your money with NovaWallet.',
                                  style: Theme.of(context).textTheme.bodyLarge
                                      ?.copyWith(
                                        color: colorScheme.onSurfaceVariant,
                                      ),
                                  textAlign: TextAlign.center,
                                ),
                              ),
                              const SizedBox(height: AppTokens.space24),

                              // Absorbs surplus height on tall screens so the
                              // CTAs sit within thumb reach; collapses to
                              // nothing on short screens instead of
                              // overflowing.
                              const Spacer(),

                              // CTAs.
                              FadeTransition(
                                opacity: _ctaOpacity,
                                child: Column(
                                  crossAxisAlignment:
                                      CrossAxisAlignment.stretch,
                                  children: [
                                    // Primary CTA: Get Started.
                                    FilledButton(
                                      onPressed: _handleGetStarted,
                                      child: const Text('Get Started'),
                                    ),
                                    const SizedBox(height: AppTokens.space16),

                                    // Secondary CTA: Sign In. The light theme
                                    // uses the palette's deep navy blue so the
                                    // link clears 4.5:1 contrast on Cloud
                                    // White; the dark theme already exceeds it
                                    // with primary.
                                    TextButton(
                                      onPressed: _handleSignIn,
                                      style: TextButton.styleFrom(
                                        minimumSize: const Size(
                                          0,
                                          AppTokens.buttonHeight,
                                        ),
                                        foregroundColor:
                                            brightness == Brightness.light
                                            ? colorScheme.onPrimaryContainer
                                            : colorScheme.primary,
                                      ),
                                      child: const Text(
                                        'I already have an account',
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ),
    );
  }
}

/// Small brand header with the Nova mark and wordmark.
class _BrandHeader extends StatelessWidget {
  const _BrandHeader();

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        const NovaMark(size: 32),
        const SizedBox(width: AppTokens.space12),
        // The wordmark is a logo-equivalent: it keeps its designed scale
        // instead of growing with the user's text-scale setting. A Flexible
        // with ellipsis guards extreme viewports/font metrics from
        // horizontal overflow.
        Flexible(
          child: Text(
            'NovaWallet',
            textScaler: TextScaler.noScaling,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.titleLarge
                ?.copyWith(letterSpacing: -0.3),
          ),
        ),
      ],
    );
  }
}

/// Wallet artwork inside an intentional Midnight brand panel.
///
/// The supplied artwork (`assets/branding/03_welcome_illustration.png`) is an
/// opaque RGB image whose subject is baked onto a pure black backdrop; no
/// transparent source asset exists, and keying the black out would strip the
/// artwork's own shadow detail. Presenting it unchanged on the approved
/// Midnight surface keeps every pixel intact while the backdrop reads as a
/// deliberate brand element in either theme.
///
/// The dark page background is the same Midnight value, so the panel carries a
/// hairline outline there to keep the composition's structure legible; on the
/// light Cloud White background the Midnight panel already separates strongly
/// and needs no border.
class _HeroPanel extends StatelessWidget {
  const _HeroPanel({required this.side});

  final double side;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final brightness = Theme.of(context).brightness;

    return Container(
      key: const Key('welcome_hero_panel'),
      width: side,
      height: side,
      padding: const EdgeInsets.all(AppTokens.space16),
      decoration: BoxDecoration(
        color: AppTheme.midnight,
        borderRadius: BorderRadius.circular(AppTokens.radiusExtraLarge),
        border: brightness == Brightness.dark
            ? Border.all(color: colorScheme.outlineVariant)
            : null,
      ),
      child: Semantics(
        image: true,
        label: 'NovaWallet blue wallet with payment cards',
        child: Image.asset(
          'assets/branding/03_welcome_illustration.png',
          fit: BoxFit.contain,
          errorBuilder: (_, _, _) => const Center(
            child: NovaMark(size: 160, color: AppTheme.electricBlue),
          ),
        ),
      ),
    );
  }
}
