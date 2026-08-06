/// Shared NovaWallet UI building blocks (per DESIGN.md).
///
/// Grouped in one library so every screen can `import 'core/widgets.dart'`
/// and stay consistent without duplicating component code.
library;

import 'package:flutter/material.dart';

import '../theme/nova_colors.dart';
import 'pill_button.dart';

export 'pill_button.dart';

/// Indigo gradient balance card — the anchor of the dashboard.
/// Mirrors the home-dashboard hero card: masked account number, action
/// buttons, and a subtle FROZEN badge when [frozen] is true.
class BalanceCard extends StatelessWidget {
  const BalanceCard({
    super.key,
    required this.balanceLabel,
    required this.balanceAmount,
    this.accountNumber,
    this.frozen = false,
    this.onDeposit,
    this.onWithdraw,
  });

  final String balanceLabel;
  final String balanceAmount;

  /// e.g. `NW ••••0421`
  final String? accountNumber;
  final bool frozen;
  final VoidCallback? onDeposit;
  final VoidCallback? onWithdraw;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [NovaColors.primary, NovaColors.primaryContainer],
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: NovaColors.shadowTint.withValues(alpha: 0.25),
            blurRadius: 20,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Balance row: label + amount + masked account + copy hint.
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      balanceLabel,
                      style: TextStyle(
                        fontSize: 14,
                        color: NovaColors.onPrimary.withValues(alpha: 0.9),
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      balanceAmount,
                      style: const TextStyle(
                        fontSize: 28,
                        fontWeight: FontWeight.w700,
                        letterSpacing: -0.02,
                        color: NovaColors.onPrimary,
                      ),
                    ),
                  ],
                ),
                if (accountNumber != null)
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 6,
                    ),
                    decoration: BoxDecoration(
                      color: NovaColors.onPrimary.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(999),
                    ),
                    child: Row(
                      children: [
                        Text(
                          accountNumber!,
                          style: const TextStyle(
                            fontSize: 13,
                            color: NovaColors.onPrimary,
                          ),
                        ),
                        const SizedBox(width: 4),
                        const Icon(
                          Icons.content_copy,
                          size: 14,
                          color: NovaColors.onPrimary,
                        ),
                      ],
                    ),
                  ),
              ],
            ),
            if (frozen) ...[
              const SizedBox(height: 8),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  color: NovaColors.warning,
                  borderRadius: BorderRadius.circular(999),
                ),
                child: const Text(
                  'FROZEN',
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 0.5,
                    color: Colors.white,
                  ),
                ),
              ),
            ],
            const SizedBox(height: 20),
            // Action row: Deposit (solid white) / Withdraw (outline).
            Row(
              children: [
                Expanded(
                  child: PillButton(
                    label: 'Deposit',
                    filled: true,
                    expanded: false,
                    onPressed: onDeposit,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: PillButton(
                    label: 'Withdraw',
                    filled: false,
                    expanded: false,
                    onPressed: onWithdraw,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

/// Single recent-transaction row: colored leading icon, title, subtitle,
/// and right-aligned signed amount (green in / red out).
class TransactionTile extends StatelessWidget {
  const TransactionTile({
    super.key,
    required this.title,
    required this.subtitle,
    required this.amountText,
    required this.incoming,
    this.icon,
    this.onTap,
  });

  final String title;
  final String subtitle;
  final String amountText;
  final bool incoming;
  final IconData? icon;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final color = incoming ? NovaColors.success : NovaColors.error;
    final icon = this.icon ??
        (incoming ? Icons.arrow_downward : Icons.arrow_upward);

    return Material(
      color: NovaColors.surfaceContainerLowest,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: NovaColors.surfaceContainerLow),
          ),
          child: Row(
            children: [
              // Leading colored icon.
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.12),
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, size: 20, color: color),
              ),
              const SizedBox(width: 12),
              // Title + subtitle.
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: NovaColors.onSurface,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: const TextStyle(
                        fontSize: 12,
                        color: NovaColors.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              // Signed amount.
              Text(
                amountText,
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: color,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Pill-shaped status chip (Completed / Pending / Failed / FROZEN...).
class StatusChip extends StatelessWidget {
  const StatusChip({
    super.key,
    required this.label,
    required this.color,
  });

  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w600,
          color: color,
        ),
      ),
    );
  }
}

/// Rounded avatar circle with initials (no network dependency).
class InitialsAvatar extends StatelessWidget {
  const InitialsAvatar({super.key, required this.name, this.size = 40});

  final String name;
  final double size;

  @override
  Widget build(BuildContext context) {
    final initials = name
        .split(' ')
        .where((p) => p.isNotEmpty)
        .take(2)
        .map((p) => p[0].toUpperCase())
        .join();
    return Container(
      width: size,
      height: size,
      decoration: const BoxDecoration(
        color: NovaColors.surfaceContainerHigh,
        shape: BoxShape.circle,
      ),
      alignment: Alignment.center,
      child: Text(
        initials,
        style: TextStyle(
          fontSize: size * 0.38,
          fontWeight: FontWeight.w700,
          color: NovaColors.primary,
        ),
      ),
    );
  }
}

/// Full-screen centered message used by every feature's error state.
class ErrorStateView extends StatelessWidget {
  const ErrorStateView({
    super.key,
    required this.message,
    this.onRetry,
    this.title = 'Something went wrong',
  });

  final String title;
  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.error_outline,
              size: 56,
              color: NovaColors.error,
            ),
            const SizedBox(height: 16),
            Text(
              title,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: NovaColors.onSurface,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(color: NovaColors.onSurfaceVariant),
            ),
            if (onRetry != null) ...[
              const SizedBox(height: 20),
              PillButton(
                label: 'Try again',
                expanded: false,
                onPressed: onRetry,
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// Full-screen empty state (e.g. "No transactions yet").
class EmptyStateView extends StatelessWidget {
  const EmptyStateView({
    super.key,
    required this.title,
    required this.message,
    this.icon = Icons.inbox_outlined,
    this.actionLabel,
    this.onAction,
  });

  final String title;
  final String message;
  final IconData icon;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 56, color: NovaColors.outlineVariant),
            const SizedBox(height: 16),
            Text(
              title,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: NovaColors.onSurface,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(color: NovaColors.onSurfaceVariant),
            ),
            if (actionLabel != null && onAction != null) ...[
              const SizedBox(height: 20),
              PillButton(
                label: actionLabel!,
                expanded: false,
                onPressed: onAction,
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// Small centered spinner used inside screens while async data loads.
class LoadingView extends StatelessWidget {
  const LoadingView({super.key, this.message});

  final String? message;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const CircularProgressIndicator(),
          if (message != null) ...[
            const SizedBox(height: 16),
            Text(
              message!,
              style: const TextStyle(color: NovaColors.onSurfaceVariant),
            ),
          ],
        ],
      ),
    );
  }
}

/// Section header used across screens ("Recent transactions" + "View all").
class SectionHeader extends StatelessWidget {
  const SectionHeader({
    super.key,
    required this.title,
    this.actionLabel,
    this.onAction,
  });

  final String title;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          title,
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: NovaColors.onSurface,
          ),
        ),
        if (actionLabel != null)
          TextButton(
            onPressed: onAction,
            style: TextButton.styleFrom(
              padding: EdgeInsets.zero,
              minimumSize: Size.zero,
              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
            ),
            child: Text(actionLabel!),
          ),
      ],
    );
  }
}

/// Amount entry with ZMW prefix + quick-amount chips (per send/deposit
/// designs). Exposes a [TextEditingController] for form integration.
class AmountInput extends StatelessWidget {
  const AmountInput({
    super.key,
    required this.controller,
    this.quickAmounts = const [],
    this.onChanged,
  });

  final TextEditingController controller;
  final List<String> quickAmounts;
  final ValueChanged<String>? onChanged;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TextField(
          controller: controller,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          onChanged: onChanged,
          style: const TextStyle(fontSize: 28, fontWeight: FontWeight.w700),
          decoration: const InputDecoration(
            prefixText: 'ZMW ',
            prefixStyle: TextStyle(
              fontSize: 28,
              fontWeight: FontWeight.w700,
              color: NovaColors.primary,
            ),
            hintText: '0.00',
            hintStyle: TextStyle(
              fontSize: 28,
              fontWeight: FontWeight.w700,
              color: NovaColors.outlineVariant,
            ),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.all(Radius.circular(16)),
              borderSide: BorderSide(color: NovaColors.outlineVariant),
            ),
          ),
        ),
        if (quickAmounts.isNotEmpty) ...[
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            children: [
              for (final amount in quickAmounts)
                ActionChip(
                  label: Text('ZMW $amount'),
                  onPressed: () {
                    controller.text = amount;
                    onChanged?.call(amount);
                  },
                ),
            ],
          ),
        ],
      ],
    );
  }
}

/// Horizontal "or continue with X" divider line.
class OrDivider extends StatelessWidget {
  const OrDivider({super.key, required this.label, this.padding = 20});

  final String label;
  final double padding;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.symmetric(vertical: padding),
      child: Row(
        children: [
          const Expanded(child: Divider(color: NovaColors.outlineVariant)),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Text(
              label,
              style: const TextStyle(
                color: NovaColors.onSurfaceVariant,
                fontSize: 13,
              ),
            ),
          ),
          const Expanded(child: Divider(color: NovaColors.outlineVariant)),
        ],
      ),
    );
  }
}

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