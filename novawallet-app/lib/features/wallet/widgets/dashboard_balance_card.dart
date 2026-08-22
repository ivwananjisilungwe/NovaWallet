import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/design_tokens.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/widgets.dart';
import '../models/wallet.dart';

/// Balance card with gradient background and action buttons.
class DashboardBalanceCard extends ConsumerStatefulWidget {
  const DashboardBalanceCard({
    super.key,
    required this.wallet,
    required this.onDeposit,
    required this.onWithdraw,
  });

  final Wallet wallet;
  final VoidCallback onDeposit;
  final VoidCallback onWithdraw;

  @override
  ConsumerState<DashboardBalanceCard> createState() =>
      _DashboardBalanceCardState();
}

class _DashboardBalanceCardState extends ConsumerState<DashboardBalanceCard>
    with SingleTickerProviderStateMixin {
  late final AnimationController _shimmerController;
  late final Animation<double> _shimmerAnimation;

  @override
  void initState() {
    super.initState();
    _shimmerController = AnimationController(
      duration: const Duration(milliseconds: 2000),
      vsync: this,
    )..repeat();
    _shimmerAnimation = _shimmerController;
  }

  @override
  void dispose() {
    _shimmerController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final balance = widget.wallet.balance;
    final maskedAccount = _maskAccount(widget.wallet.accountNumber);

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        // Brand hero card — identical gradient in both themes (white ink on
        // indigo), so it uses the theme-invariant brand tokens.
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            SemanticColors.brandPrimary, // #3525CD — brand indigo
            SemanticColors.brandPrimaryContainer, // #4F46E5 — gradient anchor
          ],
        ),
        boxShadow: ElevationTokens.shadowCard,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Account label
          Row(
            children: [
              const Icon(
                Icons.account_balance_wallet_outlined,
                color: SemanticColors.onBrandPrimary,
                size: 20,
              ),
              const SizedBox(width: 8),
              Text(
                'NovaWallet',
                style: Theme.of(context).textTheme.labelLarge?.copyWith(
                  color: SemanticColors.onBrandPrimary.withValues(alpha: 0.9),
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          // Masked account number
          Text(
            maskedAccount,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: SemanticColors.onBrandPrimary.withValues(alpha: 0.7),
            ),
          ),
          const SizedBox(height: 24),
          // Balance amount with count-up (800ms) layered under shimmer
          TweenAnimationBuilder<double>(
            tween: Tween<double>(
              begin: 0,
              end:
                  double.tryParse(balance.replaceAll(RegExp(r'[^0-9.]'), '')) ??
                  0,
            ),
            duration: const Duration(milliseconds: 800),
            curve: Curves.easeOutCubic,
            builder: (context, animatedValue, _) {
              return AnimatedBuilder(
                animation: _shimmerAnimation,
                builder: (context, child) {
                  return ShaderMask(
                    blendMode: BlendMode.srcIn,
                    shaderCallback: (bounds) {
                      return LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        colors: [
                          SemanticColors.onBrandPrimary,
                          SemanticColors.onBrandPrimary.withValues(alpha: 0.6),
                          SemanticColors.onBrandPrimary,
                        ],
                        stops: const [0.0, 0.5, 1.0],
                        transform: _SlidingGradientTransform(
                          slidePercent: _shimmerAnimation.value,
                        ),
                      ).createShader(bounds);
                    },
                    child: Text(
                      formatZmw(animatedValue.toStringAsFixed(2)),
                      style: Theme.of(context).textTheme.displayMedium
                          ?.copyWith(
                            color: SemanticColors.onBrandPrimary,
                            fontWeight: FontWeight.bold,
                            height: 1.1,
                          ),
                    ),
                  );
                },
              );
            },
          ),
          const SizedBox(height: 4),
          Text(
            'Available Balance',
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: SemanticColors.onBrandPrimary.withValues(alpha: 0.7),
            ),
          ),
          const SizedBox(height: 24),
          // Action buttons
          Row(
            children: [
              Expanded(
                child: PillButton(
                  label: 'Deposit',
                  icon: Icons.add_circle_outline,
                  variant: PillButtonVariant.outlined,
                  onPressed: widget.onDeposit,
                  semanticsLabel: 'Deposit money',
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: PillButton(
                  label: 'Withdraw',
                  icon: Icons.remove_circle_outline,
                  variant: PillButtonVariant.filled,
                  onPressed: widget.onWithdraw,
                  semanticsLabel: 'Withdraw money',
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  String _maskAccount(String accountNumber) {
    if (accountNumber.length <= 4) return accountNumber;
    final visible = accountNumber.substring(accountNumber.length - 4);
    return '**** **** **** $visible';
  }
}

/// Transform for sliding gradient in balance card.
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
