import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:novawallet_mobile/app/app_shell.dart';
import 'package:novawallet_mobile/core/design/app_tokens.dart';
import 'package:novawallet_mobile/features/auth/data/auth_service.dart';
import 'package:novawallet_mobile/features/auth/data/session_store.dart';
import 'package:novawallet_mobile/features/auth/presentation/register_page.dart';
import 'package:novawallet_mobile/shared/navigation/fade_through_route.dart';
import 'package:novawallet_mobile/shared/widgets/nova_mark.dart';

/// Screen 03 — Login / Sign in.
///
/// Connects to the verified Spring Boot `novawallet-api` login contract
/// (`POST /api/v1/auth/login`) through an injectable [AuthService].
///
/// The post-login destination is not yet implemented in this app, so a
/// successful login without a wired [SessionStore] surfaces an honest
/// confirmation snackbar instead of navigating to an invented route (see
/// TODO.md for the integration boundary). With a [SessionStore] provided the
/// session is persisted and the app shell is shown.
///
/// The viewport fills tall screens, anchors the form toward the bottom, and
/// scrolls on short screens or while the keyboard is open. The entrance
/// sequence reuses the shared motion tokens; reduced motion lands on the final
/// composition in the first frame and never delays sign-in.
class LoginPage extends StatefulWidget {
  const LoginPage({super.key, this.authService, this.sessionStore});

  /// Auth service used to call the login API. Inject a test double in tests;
  /// otherwise the real `HttpAuthService` backed by `AppConfig` is used.
  final AuthService? authService;

  /// Optional session store; when provided, a successful login persists the
  /// session and navigates to the dashboard.
  final SessionStore? sessionStore;

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage>
    with SingleTickerProviderStateMixin {
  /// Sequenced entrances (brand, heading, form).
  static const int _motionSteps = 2;

  static const int _entranceTotalMs =
      AppTokens.animationEntranceMs +
      AppTokens.animationStaggerMs * _motionSteps;

  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _emailFocus = FocusNode();
  final _passwordFocus = FocusNode();

  bool _obscurePassword = true;
  bool _isSubmitting = false;
  bool _isNavigatingToRegister = false;
  bool _autovalidate = false;
  String? _errorMessage;
  bool _motionStarted = false;

  late final AnimationController _controller;
  late final Animation<double> _brandOpacity;
  late final Animation<Offset> _brandSlide;
  late final Animation<double> _headingOpacity;
  late final Animation<Offset> _headingSlide;
  late final Animation<double> _formOpacity;
  late final Animation<Offset> _formSlide;

  /// The auth service driving login — a test double when injected, the real
  /// HTTP-backed service otherwise.
  AuthService get _authService => widget.authService ?? HttpAuthService();

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: _entranceTotalMs),
    );

    _brandOpacity = _fadeAtStep(0, Curves.easeOut);
    _brandSlide = _slideAtStep(0, 0.12, Curves.easeOutCubic);
    _headingOpacity = _fadeAtStep(1, Curves.easeOut);
    _headingSlide = _slideAtStep(1, 0.08, Curves.easeOutCubic);
    _formOpacity = _fadeAtStep(2, Curves.easeOut);
    _formSlide = _slideAtStep(2, 0.08, Curves.easeOutCubic);
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

  Animation<Offset> _slideAtStep(int step, double dy, Curve curve) =>
      Tween<Offset>(begin: Offset(0, dy), end: Offset.zero).animate(
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
      // Reduced motion: final composition on the first frame; the controller
      // never runs, so the form is usable immediately.
      _controller.value = 1.0;
    } else {
      _controller.forward();
    }
  }

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _emailFocus.dispose();
    _passwordFocus.dispose();
    _controller.dispose();
    super.dispose();
  }

  String? _validateEmail(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'Email is required';
    }
    final emailRegex = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$');
    if (!emailRegex.hasMatch(value.trim())) {
      return 'Enter a valid email address';
    }
    return null;
  }

  String? _validatePassword(String? value) {
    if (value == null || value.isEmpty) {
      return 'Password is required';
    }
    return null;
  }

  Future<void> _handleSignIn() async {
    if (_isSubmitting) return;
    if (!_formKey.currentState!.validate()) {
      // First failed attempt turns on per-field autovalidation so the user is
      // guided while correcting entries — not before they have tried.
      if (!_autovalidate) {
        setState(() => _autovalidate = true);
      }
      return;
    }

    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });

    try {
      final session = await _authService.login(
        email: _emailController.text.trim(),
        password: _passwordController.text,
      );
      if (!mounted) return;

      if (widget.sessionStore != null) {
        await widget.sessionStore!.saveSession(session);
        if (!mounted) return;
        setState(() => _isSubmitting = false);
        Navigator.of(context).pushReplacement(
          MaterialPageRoute<void>(
            builder: (_) => AppShell(sessionStore: widget.sessionStore),
          ),
        );
        return;
      }

      setState(() => _isSubmitting = false);

      // Honest state: no session store wired (preview). Show confirmation.
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Signed in as ${session.user.displayName.isNotEmpty ? session.user.displayName : session.user.email}.',
          ),
          duration: const Duration(seconds: 3),
        ),
      );
    } on AuthException catch (error) {
      if (!mounted) return;
      setState(() {
        _isSubmitting = false;
        _errorMessage = error.userMessage;
      });
    } catch (_) {
      // Never leak unexpected internals: the spinner always clears and the
      // user gets the same safe message shape as the service's generic error.
      if (!mounted) return;
      setState(() {
        _isSubmitting = false;
        _errorMessage = 'Unable to sign in. Please try again.';
      });
    }
  }

  void _handleForgotPassword() {
    // Placeholder: Forgot Password screen is not yet implemented.
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Password reset is not connected in preview mode.'),
        duration: Duration(seconds: 2),
      ),
    );
  }

  Future<void> _handleCreateAccount() async {
    if (_isNavigatingToRegister || _isSubmitting) return;
    setState(() => _isNavigatingToRegister = true);
    await Navigator.of(context).push<void>(
      FadeThroughRoute<void>(
        builder: (_) => RegisterPage(authService: widget.authService),
      ),
    );
    if (mounted) setState(() => _isNavigatingToRegister = false);
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
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
          child: LayoutBuilder(
            builder: (context, constraints) {
              // Keep a readable measure on wide windows without disturbing the
              // vertical fill/scroll behavior (dynamic padding, no Center,
              // which would re-center the block vertically).
              final horizontalPadding =
                  constraints.maxWidth > AppTokens.formMaxWidth
                  ? (constraints.maxWidth - AppTokens.formMaxWidth) / 2
                  : AppTokens.space24;

              return SingleChildScrollView(
                child: ConstrainedBox(
                  constraints: BoxConstraints(minHeight: constraints.maxHeight),
                  child: Padding(
                    padding: EdgeInsets.fromLTRB(
                      horizontalPadding,
                      AppTokens.space24,
                      horizontalPadding,
                      AppTokens.space32,
                    ),
                    child: IntrinsicHeight(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.center,
                        children: [
                          // Brand header: small mark + wordmark.
                          FadeTransition(
                            opacity: _brandOpacity,
                            child: SlideTransition(
                              position: _brandSlide,
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  const NovaMark(size: 32),
                                  const SizedBox(width: AppTokens.space12),
                                  // Logo-equivalent wordmark: fixed scale
                                  // with an ellipsis guard so extreme
                                  // text scales never overflow the row.
                                  Flexible(
                                    child: Text(
                                      'NovaWallet',
                                      textScaler: TextScaler.noScaling,
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: Theme.of(context)
                                          .textTheme
                                          .titleLarge
                                          ?.copyWith(letterSpacing: -0.3),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(height: AppTokens.space40),

                          // Heading.
                          FadeTransition(
                            opacity: _headingOpacity,
                            child: SlideTransition(
                              position: _headingSlide,
                              child: Semantics(
                                header: true,
                                child: Text(
                                  'Welcome back',
                                  style: Theme.of(context)
                                      .textTheme
                                      .headlineLarge
                                      ?.copyWith(height: 1.15),
                                  textAlign: TextAlign.center,
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(height: AppTokens.space16),

                          // Supporting copy.
                          FadeTransition(
                            opacity: _headingOpacity,
                            child: SlideTransition(
                              position: _headingSlide,
                              child: Text(
                                'Sign in to continue to your wallet.',
                                style: Theme.of(context).textTheme.bodyLarge
                                    ?.copyWith(
                                      color: colorScheme.onSurfaceVariant,
                                    ),
                                textAlign: TextAlign.center,
                              ),
                            ),
                          ),
                          const SizedBox(height: AppTokens.space32),

                          // Surplus height is absorbed here so the form
                          // sits in thumb reach on tall screens; on short
                          // screens it collapses and the viewport scrolls.
                          const Spacer(),

                          // Form.
                          FadeTransition(
                            opacity: _formOpacity,
                            child: SlideTransition(
                              position: _formSlide,
                              child: Form(
                                key: _formKey,
                                autovalidateMode: _autovalidate
                                    ? AutovalidateMode.onUserInteraction
                                    : AutovalidateMode.disabled,
                                child: Column(
                                  crossAxisAlignment:
                                      CrossAxisAlignment.stretch,
                                  children: [
                                    // The banner slot animates its height so
                                    // an error appearing does not shove the
                                    // form down with a hard jump.
                                    AnimatedSize(
                                      duration: const Duration(
                                        milliseconds: 200,
                                      ),
                                      curve: Curves.easeOutCubic,
                                      alignment: Alignment.topCenter,
                                      child: _LoginErrorBanner(
                                        message: _errorMessage,
                                      ),
                                    ),
                                    const SizedBox(height: AppTokens.space16),

                                    // Email field.
                                    TextFormField(
                                      controller: _emailController,
                                      focusNode: _emailFocus,
                                      keyboardType: TextInputType.emailAddress,
                                      textInputAction: TextInputAction.next,
                                      // Addresses are case-insensitive and
                                      // must not be auto-capitalised or
                                      // "corrected" by the keyboard.
                                      textCapitalization:
                                          TextCapitalization.none,
                                      autocorrect: false,
                                      enableSuggestions: false,
                                      onFieldSubmitted: (_) =>
                                          FocusScope.of(context)
                                              .requestFocus(_passwordFocus),
                                      autofillHints: const [
                                        AutofillHints.email,
                                      ],
                                      validator: _validateEmail,
                                      decoration: const InputDecoration(
                                        labelText: 'Email',
                                        hintText: 'you@example.com',
                                        prefixIcon: Icon(Icons.email_outlined),
                                      ),
                                    ),
                                    const SizedBox(height: AppTokens.space16),

                                    // Password field.
                                    TextFormField(
                                      controller: _passwordController,
                                      focusNode: _passwordFocus,
                                      keyboardType:
                                          TextInputType.visiblePassword,
                                      obscureText: _obscurePassword,
                                      textInputAction: TextInputAction.done,
                                      // Never capitalise or suggest content
                                      // inside a password.
                                      textCapitalization:
                                          TextCapitalization.none,
                                      autocorrect: false,
                                      enableSuggestions: false,
                                      onFieldSubmitted: (_) => _handleSignIn(),
                                      autofillHints: const [
                                        AutofillHints.password,
                                      ],
                                      validator: _validatePassword,
                                      decoration: InputDecoration(
                                        labelText: 'Password',
                                        hintText: '••••••••',
                                        prefixIcon: const Icon(
                                          Icons.lock_outline,
                                        ),
                                        suffixIcon: IconButton(
                                          icon: Icon(
                                            _obscurePassword
                                                ? Icons.visibility_off_outlined
                                                : Icons.visibility_outlined,
                                          ),
                                          tooltip: _obscurePassword
                                              ? 'Show password'
                                              : 'Hide password',
                                          onPressed: () => setState(() {
                                            _obscurePassword =
                                                !_obscurePassword;
                                          }),
                                        ),
                                      ),
                                    ),
                                    const SizedBox(height: AppTokens.space12),

                                    // Forgot password — pending screen.
                                    Align(
                                      alignment: Alignment.centerRight,
                                      child: TextButton(
                                        onPressed: _isSubmitting
                                            ? null
                                            : _handleForgotPassword,
                                        style: TextButton.styleFrom(
                                          minimumSize: const Size(0, 48),
                                          padding: const EdgeInsets.symmetric(
                                            horizontal: AppTokens.space8,
                                            vertical: AppTokens.space4,
                                          ),
                                        ),
                                        child: const Text('Forgot password?'),
                                      ),
                                    ),
                                    const SizedBox(height: AppTokens.space32),

                                    // Primary sign-in action.
                                    FilledButton(
                                      onPressed: _isSubmitting
                                          ? null
                                          : _handleSignIn,
                                      child: _isSubmitting
                                          ? const _SigningInIndicator()
                                          : const Text('Sign in'),
                                    ),
                                    const SizedBox(height: AppTokens.space24),

                                    // Account creation link.
                                    Wrap(
                                      alignment: WrapAlignment.center,
                                      spacing: AppTokens.space8,
                                      children: [
                                        const Text("Don't have an account? "),
                                        TextButton(
                                          onPressed:
                                              _isSubmitting ||
                                                  _isNavigatingToRegister
                                              ? null
                                              : _handleCreateAccount,
                                          style: TextButton.styleFrom(
                                            minimumSize: const Size(0, 48),
                                            padding: const EdgeInsets.symmetric(
                                              horizontal: AppTokens.space8,
                                              vertical: AppTokens.space4,
                                            ),
                                          ),
                                          child: const Text('Create account'),
                                        ),
                                      ],
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        ],
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

/// Busy state for the primary sign-in action.
///
/// Keeps the spinner *and* a plain-language label so the user knows the
/// request is in progress, and announces the state to screen readers through
/// a live region. It is shown only while a real authentication request is
/// running — never as a success animation.
class _SigningInIndicator extends StatelessWidget {
  const _SigningInIndicator();

  @override
  Widget build(BuildContext context) {
    return Semantics(
      liveRegion: true,
      label: 'Signing in',
      child: Row(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const SizedBox(
            height: 20,
            width: 20,
            child: CircularProgressIndicator(strokeWidth: 2),
          ),
          const SizedBox(width: AppTokens.space12),
          const Text('Signing in…'),
        ],
      ),
    );
  }
}

/// Live-region error banner for authentication and network failures.
///
/// Presentational only — the message is always a user-safe string produced by
/// the auth layer; no tokens, credentials, or backend payloads are shown.
class _LoginErrorBanner extends StatelessWidget {
  const _LoginErrorBanner({required this.message});

  final String? message;

  @override
  Widget build(BuildContext context) {
    final message = this.message;
    if (message == null) {
      return const SizedBox.shrink();
    }
    final colorScheme = Theme.of(context).colorScheme;
    return Semantics(
      liveRegion: true,
      child: Container(
        padding: const EdgeInsets.all(AppTokens.space12),
        decoration: BoxDecoration(
          color: colorScheme.error.withValues(alpha: 0.10),
          border: Border.all(color: colorScheme.error.withValues(alpha: 0.30)),
          borderRadius: BorderRadius.circular(AppTokens.radiusMedium),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(Icons.error_outline, color: colorScheme.error, size: 20),
            const SizedBox(width: AppTokens.space12),
            Expanded(
              child: Text(
                message,
                style: Theme.of(context).textTheme.bodyMedium
                    ?.copyWith(color: colorScheme.error, height: 1.4),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
