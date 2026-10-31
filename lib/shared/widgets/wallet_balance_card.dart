import 'package:flutter/material.dart';

import '../../app/theme/app_theme.dart';
import '../../core/design/app_tokens.dart';

/// Prominent wallet balance presentation.
///
/// The balance is supplied by the caller; this widget does not invent
/// or fetch financial data.
class WalletBalanceCard extends StatelessWidget {
  const WalletBalanceCard({
    required this.balance,
    required this.accountLabel,
    required this.balanceVisible,
    required this.onToggleVisibility,
    super.key,
  });

  final String balance;
  final String accountLabel;
  final bool balanceVisible;
  final VoidCallback onToggleVisibility;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(AppTokens.space24),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [AppTheme.electricBlue, Color(0xFF2449B8), AppTheme.midnight],
        ),
        borderRadius: BorderRadius.circular(AppTokens.radiusExtraLarge),
        boxShadow: [
          BoxShadow(
            color: AppTheme.electricBlue.withValues(alpha: 0.18),
            blurRadius: 28,
            offset: const Offset(0, 12),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                Icons.account_balance_wallet_rounded,
                color: Colors.white.withValues(alpha: 0.9),
                size: 20,
              ),
              const SizedBox(width: AppTokens.space8),
              Expanded(
                child: Text(
                  accountLabel,
                  style: TextStyle(
                    color: Colors.white.withValues(alpha: 0.85),
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
              IconButton(
                onPressed: onToggleVisibility,
                tooltip: balanceVisible ? 'Hide balance' : 'Show balance',
                icon: Icon(
                  balanceVisible
                      ? Icons.visibility_rounded
                      : Icons.visibility_off_rounded,
                  color: Colors.white,
                ),
              ),
            ],
          ),
          const SizedBox(height: AppTokens.space16),
          Text(
            'Available balance',
            style: TextStyle(
              color: Colors.white.withValues(alpha: 0.8),
              fontSize: 13,
            ),
          ),
          const SizedBox(height: AppTokens.space8),
          Semantics(
            label: balanceVisible ? balance : 'Balance hidden',
            child: ExcludeSemantics(
              child: AnimatedSwitcher(
                duration: const Duration(milliseconds: 180),
                child: Text(
                  balanceVisible ? balance : '••••••••',
                  key: ValueKey(balanceVisible),
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 34,
                    fontWeight: FontWeight.w700,
                    letterSpacing: -1.1,
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(height: AppTokens.space20),
          Container(height: 1, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: AppTokens.space16),
          const Row(
            children: [
              Icon(
                Icons.verified_user_outlined,
                size: 16,
                color: Colors.white70,
              ),
              SizedBox(width: AppTokens.space8),
              Expanded(
                child: Text(
                  'Your wallet at a glance',
                  style: TextStyle(color: Colors.white70, fontSize: 12),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
