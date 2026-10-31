import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:novawallet_mobile/core/design/app_tokens.dart';
import 'package:novawallet_mobile/features/auth/data/auth_service.dart';
import 'package:novawallet_mobile/features/auth/presentation/email_verification_page.dart';
import 'package:novawallet_mobile/features/auth/presentation/login_page.dart';
import 'package:novawallet_mobile/shared/navigation/fade_through_route.dart';
import 'package:novawallet_mobile/shared/widgets/nova_mark.dart';

/// Screen 04 — Registration / Create account.
///
/// Connects to the verified Spring Boot `novawallet-api` registration
/// contract (`POST /api/v1/auth/register`) through an injectable
/// [AuthService].
///
/// Registration may require email verification; this app has no verification
/// screen yet, so a successful registration surfaces an honest confirmation
/// instead of navigating to an invented route (see TODO.md).
class RegisterPage extends StatefulWidget {
  const RegisterPage({super.key, this.authService});

  final AuthService? authService;

  @override
  State<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends State<RegisterPage>
    with SingleTickerProviderStateMixin {
  final _formKey = GlobalKey<FormState>();
  final _firstNameController = TextEditingController();
  final _lastNameController = TextEditingController();
  final _emailController = TextEditingController();
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();

  final _firstNameFocus = FocusNode();
  final _lastNameFocus = FocusNode();
  final _emailFocus = FocusNode();
  final _phoneFocus = FocusNode();
  final _passwordFocus = FocusNode();
  final _confirmPasswordFocus = FocusNode();

  bool _obscurePassword = true;
  bool _obscureConfirm = true;
  bool _isSubmitting = false;
  String? _errorMessage;
  String? _successMessage;
  bool _reducedMotionHandled = false;

  late final AnimationController _controller;
  late final Animation<double> _brandOpacity;
  late final Animation<Offset> _brandSlide;
  late final Animation<double> _headingOpacity;
  late final Animation<Offset> _headingSlide;
  late final Animation<double> _formOpacity;
  late final Animation<Offset> _formSlide;

  AuthService get _authService => widget.authService ?? HttpAuthService();

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
    _firstNameController.dispose();
    _lastNameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    _firstNameFocus.dispose();
    _lastNameFocus.dispose();
    _emailFocus.dispose();
    _phoneFocus.dispose();
    _passwordFocus.dispose();
    _confirmPasswordFocus.dispose();
    _controller.dispose();
    super.dispose();
  }

  static final RegExp _emailRegex = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$');
  static final RegExp _phoneRegex = RegExp(r'^\+?[1-9]\d{9,14}$');
  static bool _passwordMeetsPolicy(String value) {
    if (value.length < 8) return false;
    final hasLower = value.contains(RegExp(r'[a-z]'));
    final hasUpper = value.contains(RegExp(r'[A-Z]'));
    final hasDigit = value.contains(RegExp(r'\d'));
    final hasSpecial = value.contains(
      RegExp(
        r'[!@#\$%^&*()_+\-=\[\]{};:'
        "'"
        '"\\|,.<>/?]',
      ),
    );
    return hasLower && hasUpper && hasDigit && hasSpecial;
  }

  String? _validateRequired(String? value, String label) {
    if (value == null || value.trim().isEmpty) {
      return '$label is required';
    }
    return null;
  }

  String? _validateFirstName(String? value) =>
      _validateRequired(value, 'First name');

  String? _validateLastName(String? value) =>
      _validateRequired(value, 'Last name');

  String? _validateEmail(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'Email is required';
    }
    if (!_emailRegex.hasMatch(value.trim())) {
      return 'Enter a valid email address';
    }
    return null;
  }

  String? _validatePhone(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'Phone number is required';
    }
    if (!_phoneRegex.hasMatch(value.trim())) {
      return 'Enter a valid phone number (e.g. +260971234567)';
    }
    return null;
  }

  String? _validatePassword(String? value) {
    if (value == null || value.isEmpty) {
      return 'Password is required';
    }
    if (value.length < 8 || value.length > 128) {
      return 'Password must be between 8 and 128 characters';
    }
    if (!_passwordMeetsPolicy(value)) {
      return 'Password must contain uppercase, lowercase, digit, and special character';
    }
    return null;
  }

  String? _validateConfirmPassword(String? value) {
    if (value == null || value.isEmpty) {
      return 'Confirm password is required';
    }
    if (value != _passwordController.text) {
      return 'Passwords do not match';
    }
    return null;
  }

  Future<void> _handleRegister() async {
    if (_isSubmitting) return;
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
      _successMessage = null;
    });

    try {
      final session = await _authService.register(
        firstName: _firstNameController.text.trim(),
        lastName: _lastNameController.text.trim(),
        email: _emailController.text.trim(),
        phone: _phoneController.text.trim(),
        password: _passwordController.text,
      );
      if (!mounted) return;
      setState(() {
        _isSubmitting = false;
        _successMessage =
            'Account created for ${session.user.displayName.isNotEmpty ? session.user.displayName : session.user.email}. Please verify your email to continue.';
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Account created. Please verify your email to continue.',
          ),
          duration: const Duration(seconds: 4),
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
    // Login screen exists — hand off honestly.
    Navigator.of(context).pushReplacement(
      FadeThroughRoute<void>(builder: (_) => const LoginPage()),
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

  Widget _buildSuccessBanner(BuildContext context, ColorScheme colorScheme) {
    if (_successMessage == null) return const SizedBox.shrink();
    return Semantics(
      liveRegion: true,
      child: Container(
        margin: const EdgeInsets.only(bottom: AppTokens.space16),
        padding: const EdgeInsets.all(AppTokens.space12),
        decoration: BoxDecoration(
          color: colorScheme.primary.withValues(alpha: 0.10),
          border: Border.all(
            color: colorScheme.primary.withValues(alpha: 0.30),
          ),
          borderRadius: BorderRadius.circular(AppTokens.radiusMedium),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(
              Icons.check_circle_outline,
              color: colorScheme.primary,
              size: 20,
            ),
            const SizedBox(width: AppTokens.space12),
            Expanded(
              child: Text(
                _successMessage!,
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
    );
  }

  InputDecoration _fieldDecoration({
    required String label,
    required String hint,
    required IconData prefixIcon,
  }) {
    return InputDecoration(
      labelText: label,
      hintText: hint,
      prefixIcon: Icon(prefixIcon),
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
                    child: Form(
                      key: _formKey,
                      autovalidateMode: AutovalidateMode.disabled,
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
                                    style: Theme.of(context)
                                        .textTheme
                                        .titleLarge
                                        ?.copyWith(letterSpacing: -0.3),
                                  ),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(height: AppTokens.space32),

                          FadeTransition(
                            opacity: _headingOpacity,
                            child: SlideTransition(
                              position: _headingSlide,
                              child: Text(
                                'Create your account',
                                style: Theme.of(context)
                                    .textTheme
                                    .headlineLarge,
                                textAlign: TextAlign.center,
                              ),
                            ),
                          ),
                          const SizedBox(height: AppTokens.space12),
                          FadeTransition(
                            opacity: _headingOpacity,
                            child: SlideTransition(
                              position: _headingSlide,
                              child: Text(
                                'Join NovaWallet to manage your money.',
                                style: Theme.of(context).textTheme.bodyLarge
                                    ?.copyWith(
                                      color: colorScheme.onSurfaceVariant,
                                    ),
                                textAlign: TextAlign.center,
                              ),
                            ),
                          ),
                          const SizedBox(height: AppTokens.space32),

                          FadeTransition(
                            opacity: _formOpacity,
                            child: SlideTransition(
                              position: _formSlide,
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: [
                                  _buildErrorBanner(context, colorScheme),
                                  _buildSuccessBanner(context, colorScheme),
                                  if (_successMessage != null)
                                    Padding(
                                      padding: const EdgeInsets.only(
                                        bottom: AppTokens.space16,
                                      ),
                                      child: TextButton(
                                        onPressed: () {
                                          Navigator.of(context).push(
                                            MaterialPageRoute<void>(
                                              builder: (_) =>
                                                  EmailVerificationPage(
                                                    authService: _authService,
                                                    email: _emailController.text
                                                        .trim(),
                                                  ),
                                            ),
                                          );
                                        },
                                        child: const Text(
                                          'Go to email verification',
                                        ),
                                      ),
                                    ),

                                  TextFormField(
                                    controller: _firstNameController,
                                    focusNode: _firstNameFocus,
                                    textInputAction: TextInputAction.next,
                                    onFieldSubmitted: (_) =>
                                        FocusScope.of(context)
                                            .requestFocus(_lastNameFocus),
                                    autofillHints: const [
                                      AutofillHints.givenName,
                                    ],
                                    validator: _validateFirstName,
                                    decoration: _fieldDecoration(
                                      label: 'First name',
                                      hint: 'John',
                                      prefixIcon: Icons.person_outline,
                                    ),
                                  ),
                                  const SizedBox(height: AppTokens.space16),

                                  TextFormField(
                                    controller: _lastNameController,
                                    focusNode: _lastNameFocus,
                                    textInputAction: TextInputAction.next,
                                    onFieldSubmitted: (_) =>
                                        FocusScope.of(context)
                                            .requestFocus(_emailFocus),
                                    autofillHints: const [
                                      AutofillHints.familyName,
                                    ],
                                    validator: _validateLastName,
                                    decoration: _fieldDecoration(
                                      label: 'Last name',
                                      hint: 'Doe',
                                      prefixIcon: Icons.person_outline,
                                    ),
                                  ),
                                  const SizedBox(height: AppTokens.space16),

                                  TextFormField(
                                    controller: _emailController,
                                    focusNode: _emailFocus,
                                    keyboardType: TextInputType.emailAddress,
                                    textInputAction: TextInputAction.next,
                                    onFieldSubmitted: (_) =>
                                        FocusScope.of(context)
                                            .requestFocus(_phoneFocus),
                                    autofillHints: const [AutofillHints.email],
                                    validator: _validateEmail,
                                    decoration: _fieldDecoration(
                                      label: 'Email',
                                      hint: 'you@example.com',
                                      prefixIcon: Icons.email_outlined,
                                    ),
                                  ),
                                  const SizedBox(height: AppTokens.space16),

                                  TextFormField(
                                    controller: _phoneController,
                                    focusNode: _phoneFocus,
                                    keyboardType: TextInputType.phone,
                                    textInputAction: TextInputAction.next,
                                    onFieldSubmitted: (_) =>
                                        FocusScope.of(context)
                                            .requestFocus(_passwordFocus),
                                    autofillHints: const [
                                      AutofillHints.telephoneNumber,
                                    ],
                                    validator: _validatePhone,
                                    decoration: _fieldDecoration(
                                      label: 'Phone number',
                                      hint: '+260971234567',
                                      prefixIcon: Icons.phone_outlined,
                                    ),
                                  ),
                                  const SizedBox(height: AppTokens.space16),

                                  TextFormField(
                                    controller: _passwordController,
                                    focusNode: _passwordFocus,
                                    keyboardType: TextInputType.visiblePassword,
                                    obscureText: _obscurePassword,
                                    textInputAction: TextInputAction.next,
                                    onFieldSubmitted: (_) => FocusScope.of(
                                      context,
                                    ).requestFocus(_confirmPasswordFocus),
                                    autofillHints: const [
                                      AutofillHints.newPassword,
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
                                          _obscurePassword = !_obscurePassword;
                                        }),
                                      ),
                                    ),
                                  ),
                                  const SizedBox(height: AppTokens.space16),

                                  TextFormField(
                                    controller: _confirmPasswordController,
                                    focusNode: _confirmPasswordFocus,
                                    keyboardType: TextInputType.visiblePassword,
                                    obscureText: _obscureConfirm,
                                    textInputAction: TextInputAction.done,
                                    onFieldSubmitted: (_) => _handleRegister(),
                                    autofillHints: const [
                                      AutofillHints.newPassword,
                                    ],
                                    validator: _validateConfirmPassword,
                                    decoration: InputDecoration(
                                      labelText: 'Confirm password',
                                      hintText: '••••••••',
                                      prefixIcon: const Icon(
                                        Icons.lock_outline,
                                      ),
                                      suffixIcon: IconButton(
                                        icon: Icon(
                                          _obscureConfirm
                                              ? Icons.visibility_off_outlined
                                              : Icons.visibility_outlined,
                                        ),
                                        tooltip: _obscureConfirm
                                            ? 'Show password'
                                            : 'Hide password',
                                        onPressed: () => setState(() {
                                          _obscureConfirm = !_obscureConfirm;
                                        }),
                                      ),
                                    ),
                                  ),
                                  const SizedBox(height: AppTokens.space32),

                                  FilledButton(
                                    onPressed: _isSubmitting
                                        ? null
                                        : _handleRegister,
                                    child: _isSubmitting
                                        ? const SizedBox(
                                            height: 20,
                                            width: 20,
                                            child: CircularProgressIndicator(
                                              strokeWidth: 2,
                                            ),
                                          )
                                        : const Text('Create account'),
                                  ),
                                  const SizedBox(height: AppTokens.space24),

                                  Wrap(
                                    alignment: WrapAlignment.center,
                                    spacing: AppTokens.space8,
                                    children: [
                                      const Text('Already have an account? '),
                                      TextButton(
                                        onPressed: _isSubmitting
                                            ? null
                                            : _handleBackToLogin,
                                        child: const Text('Sign in'),
                                      ),
                                    ],
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
                ),
              );
            },
          ),
        ),
      ),
    );
  }
}
