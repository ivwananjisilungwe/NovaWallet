import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:novawallet_mobile/core/design/app_tokens.dart';
import 'package:novawallet_mobile/features/auth/data/auth_service.dart';
import 'package:novawallet_mobile/features/auth/presentation/login_page.dart';
import 'package:novawallet_mobile/shared/widgets/nova_mark.dart';

/// Screen 05 — Email verification.
///
/// Submits a verification token (when one is available, e.g. from a
/// supported link) to the verified backend endpoint
/// `POST /api/v1/email/verify?token=...`. The backend does not provide an
/// OTP or resend endpoint, so verification relies on the link emailed at
/// registration; the app never fabricates success.
class EmailVerificationPage extends StatefulWidget {
  const EmailVerificationPage({
    super.key,
    this.authService,
    this.token,
    this.email,
  });

  final AuthService? authService;
  final String? token;
  final String? email;

  @override
  State<EmailVerificationPage> createState() => _EmailVerificationPageState();
}

class _EmailVerificationPageState extends State<EmailVerificationPage>
    with SingleTickerProviderStateMixin {
  bool _isSubmitting = false;
  String? _errorMessage;
  bool _verified = false;
  bool _reducedMotionHandled = false;

  late final AnimationController _controller;
  late final Animation<double> _brandOpacity;
  late final Animation<Offset> _brandSlide;
  late final Animation<double> _headingOpacity;
  late final Animation<Offset> _headingSlide;
  late final Animation<double> _formOpacity;
  late final Animation<Offset> _formSlide;

  AuthService get _authService => widget.authService ?? HttpAuthService();

  bool get _hasToken => widget.token != null && widget.token!.trim().isNotEmpty;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 900),
    );

    _brandOpacity = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.0, 0.40, curve: Curves.easeOut),
      ),
    );
    _brandSlide = Tween<Offset>(begin: const Offset(0, 0.15), end: Offset.zero)
        .animate(
          CurvedAnimation(
            parent: _controller,
            curve: const Interval(0.0, 0.40, curve: Curves.easeOutCubic),
          ),
        );

    _headingOpacity = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.10, 0.50, curve: Curves.easeOut),
      ),
    );
    _headingSlide =
        Tween<Offset>(begin: const Offset(0, 0.10), end: Offset.zero).animate(
          CurvedAnimation(
            parent: _controller,
            curve: const Interval(0.10, 0.50, curve: Curves.easeOutCubic),
          ),
        );

    _formOpacity = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.20, 0.60, curve: Curves.easeOut),
      ),
    );
    _formSlide = Tween<Offset>(begin: const Offset(0, 0.10), end: Offset.zero)
        .animate(
          CurvedAnimation(
            parent: _controller,
            curve: const Interval(0.20, 0.60, curve: Curves.easeOutCubic),
          ),
        );

    _controller.forward();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_reducedMotionHandled) return;
    if (!MediaQuery.disableAnimationsOf(context)) return;
    _reducedMotionHandled = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      _controller.value = 1.0;
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _handleVerify() async {
    if (_isSubmitting || !_hasToken) return;
    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });

    try {
      await _authService.verifyEmail(token: widget.token!.trim());
      if (!mounted) return;
      setState(() {
        _isSubmitting = false;
        _verified = true;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Email verified successfully.'),
          duration: Duration(seconds: 3),
        ),
      );
    } on AuthException catch (error) {
      if (!mounted) return;
      setState(() {
        _isSubmitting = false;
        _errorMessage = error.userMessage;
      });
    }
  }

  void _handleBackToLogin() {
    Navigator.of(context).pushReplacement(
      MaterialPageRoute<void>(builder: (_) => const LoginPage()),
    );
  }

  Widget _buildErrorBanner(BuildContext context, ColorScheme colorScheme) {
    if (_errorMessage == null) return const SizedBox.shrink();
    return Semantics(
      liveRegion: true,
      child: Container(
        margin: const EdgeInsets.only(bottom: AppTokens.space16),
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
                _errorMessage!,
                style: TextStyle(
                  color: colorScheme.error,
                  fontSize: 14,
                  height: 1.4,
                ),
              ),
            ),
          ],
        ),
      ),
    );
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
              final effectiveWidth =
                  constraints.maxWidth > AppTokens.formMaxWidth
                  ? AppTokens.formMaxWidth
                  : constraints.maxWidth;

              return Center(
                child: SizedBox(
                  width: effectiveWidth,
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.symmetric(
                      horizontal: AppTokens.space24,
                      vertical: AppTokens.space32,
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        FadeTransition(
                          opacity: _brandOpacity,
                          child: SlideTransition(
                            position: _brandSlide,
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                const NovaMark(size: 32),
                                const SizedBox(width: AppTokens.space12),
                                Text(
                                  'NovaWallet',
                                  style: Theme.of(context).textTheme.titleLarge
                                      ?.copyWith(letterSpacing: -0.3),
                                ),
                              ],
                            ),
                          ),
                        ),
                        const SizedBox(height: AppTokens.space48),

                        FadeTransition(
                          opacity: _headingOpacity,
                          child: SlideTransition(
                            position: _headingSlide,
                            child: Text(
                              'Verify your email',
                              style: Theme.of(context).textTheme.headlineLarge,
                              textAlign: TextAlign.center,
                            ),
                          ),
                        ),
                        const SizedBox(height: AppTokens.space16),
                        FadeTransition(
                          opacity: _headingOpacity,
                          child: SlideTransition(
                            position: _headingSlide,
                            child: Text(
                              'A verification link was sent to your registered email. Open that link to verify your account.',
                              style: Theme.of(context).textTheme.bodyLarge
                                  ?.copyWith(
                                    color: colorScheme.onSurfaceVariant,
                                  ),
                              textAlign: TextAlign.center,
                            ),
                          ),
                        ),
                        if (widget.email != null &&
                            widget.email!.trim().isNotEmpty) ...[
                          const SizedBox(height: AppTokens.space16),
                          FadeTransition(
                            opacity: _headingOpacity,
                            child: Text(
                              widget.email!.trim(),
                              style: Theme.of(context).textTheme.titleMedium
                                  ?.copyWith(color: colorScheme.primary),
                              textAlign: TextAlign.center,
                            ),
                          ),
                        ],
                        const SizedBox(height: AppTokens.space40),

                        FadeTransition(
                          opacity: _formOpacity,
                          child: SlideTransition(
                            position: _formSlide,
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [
                                _buildErrorBanner(context, colorScheme),
                                if (_verified)
                                  Semantics(
                                    liveRegion: true,
                                    child: Container(
                                      margin: const EdgeInsets.only(
                                        bottom: AppTokens.space16,
                                      ),
                                      padding: const EdgeInsets.all(
                                        AppTokens.space12,
                                      ),
                                      decoration: BoxDecoration(
                                        color: colorScheme.primary.withValues(
                                          alpha: 0.10,
                                        ),
                                        border: Border.all(
                                          color: colorScheme.primary.withValues(
                                            alpha: 0.30,
                                          ),
                                        ),
                                        borderRadius: BorderRadius.circular(
                                          AppTokens.radiusMedium,
                                        ),
                                      ),
                                      child: Row(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: [
                                          Icon(
                                            Icons.check_circle_outline,
                                            color: colorScheme.primary,
                                            size: 20,
                                          ),
                                          const SizedBox(
                                            width: AppTokens.space12,
                                          ),
                                          Expanded(
                                            child: Text(
                                              'Email verified successfully.',
                                              style: TextStyle(
                                                color: colorScheme.onSurface,
                                                fontSize: 14,
                                                height: 1.4,
                                              ),
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ),
                                if (_hasToken)
                                  FilledButton(
                                    onPressed: _isSubmitting
                                        ? null
                                        : _handleVerify,
                                    child: _isSubmitting
                                        ? const SizedBox(
                                            height: 20,
                                            width: 20,
                                            child: CircularProgressIndicator(
                                              strokeWidth: 2,
                                            ),
                                          )
                                        : const Text('Verify email'),
                                  )
                                else
                                  Text(
                                    'Verification requires opening the link sent to your email. This app does not accept a verification code.',
                                    style: Theme.of(context)
                                        .textTheme
                                        .bodyMedium
                                        ?.copyWith(
                                          color: colorScheme.onSurfaceVariant,
                                        ),
                                    textAlign: TextAlign.center,
                                  ),
                                const SizedBox(height: AppTokens.space32),
                                TextButton(
                                  onPressed: _isSubmitting
                                      ? null
                                      : _handleBackToLogin,
                                  child: const Text('Back to sign in'),
                                ),
                              ],
                            ),
                          ),
                        ),
                        const SizedBox(height: AppTokens.space32),
                      ],
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
