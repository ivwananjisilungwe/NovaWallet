import 'package:flutter/material.dart';

import '../theme/nova_colors.dart';

/// Secondary "Sign in with Google" button using the brand's pill outline style.
class GoogleSignInButton extends StatelessWidget {
  const GoogleSignInButton({super.key, required this.onPressed});

  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return OutlinedButton.icon(
      onPressed: onPressed,
      style: OutlinedButton.styleFrom(
        minimumSize: const Size.fromHeight(56),
        foregroundColor: NovaColors.onSurface,
        side: const BorderSide(color: NovaColors.outlineVariant, width: 2),
        shape: const StadiumBorder(),
        backgroundColor: NovaColors.surfaceContainerLowest,
      ),
      icon: const Icon(Icons.g_mobiledata, size: 32, color: NovaColors.primary),
      label: const Text(
        'Sign in with Google',
        style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
      ),
    );
  }
}
