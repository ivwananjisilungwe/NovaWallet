import 'package:flutter/material.dart';

import 'package:novawallet_mobile/core/design/app_tokens.dart';
import 'package:novawallet_mobile/features/auth/data/auth_service.dart';
import 'package:novawallet_mobile/features/auth/data/session_store.dart';
import 'package:novawallet_mobile/features/auth/presentation/login_page.dart';
import 'package:novawallet_mobile/features/transactions/presentation/transaction_model.dart';
import 'package:novawallet_mobile/features/user/data/user_profile.dart';
import 'package:novawallet_mobile/features/user/data/user_service.dart';
import 'package:novawallet_mobile/features/wallet/data/wallet.dart';
import 'package:novawallet_mobile/features/wallet/data/wallet_service.dart';
import 'package:novawallet_mobile/shared/section_header.dart';
import 'package:novawallet_mobile/shared/widgets/quick_action_button.dart';
import 'package:novawallet_mobile/shared/widgets/transaction_tile.dart';
import 'package:novawallet_mobile/shared/widgets/wallet_balance_card.dart';

class DashboardPage extends StatefulWidget {
  const DashboardPage({
    required this.onViewAll,
    this.onDiscover,
    this.sessionStore,
    this.userService,
    this.walletService,
    super.key,
  });

  final VoidCallback onViewAll;

  /// Switches the shell to the Discover tab. Null when the dashboard is used
  /// outside the shell, in which case the action reports the preview state.
  final VoidCallback? onDiscover;
  final SessionStore? sessionStore;
  final UserService? userService;
  final WalletService? walletService;

  bool get _realMode =>
      sessionStore != null || userService != null || walletService != null;

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage> {
  bool _balanceVisible = true;
  bool _loading = true;
  String? _error;
  UserProfile? _profile;
  Wallet? _wallet;
  bool _requestInFlight = false;

  UserService get _userService =>
      widget.userService ?? HttpUserService(sessionStore: widget.sessionStore!);
  WalletService get _walletService =>
      widget.walletService ??
      HttpWalletService(sessionStore: widget.sessionStore!);

  @override
  void initState() {
    super.initState();
    if (widget._realMode) {
      _load();
    } else {
      _loading = false;
    }
  }

  Future<void> _load() async {
    if (_requestInFlight) return;
    _requestInFlight = true;
    if (mounted) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }
    try {
      final profile = await _userService.getProfile();
      Wallet? wallet;
      try {
        wallet = await _walletService.getMyWallet();
      } on WalletNotFoundException {
        wallet = null;
      }
      if (!mounted) return;
      setState(() {
        _profile = profile;
        _wallet = wallet;
        _loading = false;
      });
    } on AuthException catch (error) {
      if (error.type == AuthErrorType.sessionExpired ||
          error.statusCode == 401) {
        await widget.sessionStore?.clear();
        if (!mounted) return;
        Navigator.of(context).pushReplacement(
          MaterialPageRoute<void>(
            builder: (_) => LoginPage(sessionStore: widget.sessionStore),
          ),
        );
        return;
      }
      if (!mounted) return;
      setState(() {
        _error = error.userMessage;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = 'Unable to load your dashboard. Please try again.';
        _loading = false;
      });
    } finally {
      _requestInFlight = false;
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!widget._realMode) {
      return _buildSampleDashboard(context);
    }
    return _buildRealDashboard(context);
  }

  Widget _buildRealDashboard(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Scaffold(
      body: SingleChildScrollView(
        key: const PageStorageKey<String>('dashboard-scroll'),
        child: Padding(
          padding: AppTokens.pagePadding,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'NovaWallet',
                          style: Theme.of(context).textTheme.titleLarge,
                        ),
                        const SizedBox(height: AppTokens.space4),
                        Text(
                          _profile != null
                              ? 'Welcome back, ${_profile!.displayName}'
                              : 'Your money, in control.',
                          style: Theme.of(context).textTheme.bodyMedium,
                        ),
                      ],
                    ),
                  ),
                  IconButton.filledTonal(
                    tooltip: 'Notifications',
                    onPressed: () => ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text(
                          'Notifications are not connected in preview mode.',
                        ),
                      ),
                    ),
                    icon: const Icon(Icons.notifications_none_rounded),
                  ),
                ],
              ),
              const SizedBox(height: AppTokens.space24),
              if (_loading) ...[
                const Center(child: CircularProgressIndicator()),
              ] else if (_error != null) ...[
                Text(_error!, style: TextStyle(color: colors.error)),
                const SizedBox(height: AppTokens.space12),
                FilledButton(onPressed: _load, child: const Text('Retry')),
              ] else if (_wallet == null) ...[
                Container(
                  padding: AppTokens.cardPadding,
                  decoration: BoxDecoration(
                    color: colors.surfaceContainerHighest,
                    borderRadius: BorderRadius.circular(AppTokens.radiusLarge),
                  ),
                  child: Text(
                    'Your wallet is not available yet. Wallets are created after KYC approval.',
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                ),
              ] else ...[
                WalletBalanceCard(
                  balance: '${_wallet!.currency} ${_wallet!.balance}',
                  accountLabel: _wallet!.accountNumber,
                  balanceVisible: _balanceVisible,
                  onToggleVisibility: () =>
                      setState(() => _balanceVisible = !_balanceVisible),
                ),
                if (_wallet!.status.toUpperCase() == 'FROZEN') ...[
                  const SizedBox(height: AppTokens.space12),
                  Text(
                    'Wallet frozen${_wallet!.freezeReason != null ? ': ${_wallet!.freezeReason}' : ''}.',
                    style: TextStyle(color: colors.error),
                  ),
                ],
                const SizedBox(height: AppTokens.space24),
                const SectionHeader(title: 'Quick actions'),
                const SizedBox(height: AppTokens.space12),
                Text(
                  'Quick actions are not connected in preview mode.',
                  style: Theme.of(context).textTheme.labelMedium
                      ?.copyWith(color: colors.onSurfaceVariant),
                ),
              ],
              const SizedBox(height: AppTokens.space24),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSampleDashboard(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Scaffold(
      body: SingleChildScrollView(
        key: const PageStorageKey<String>('dashboard-scroll'),
        child: Padding(
          padding: AppTokens.pagePadding,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'NovaWallet',
                          style: Theme.of(context).textTheme.titleLarge,
                        ),
                        const SizedBox(height: AppTokens.space4),
                        Text(
                          'Your money, in control.',
                          style: Theme.of(context).textTheme.bodyMedium,
                        ),
                      ],
                    ),
                  ),
                  IconButton.filledTonal(
                    tooltip: 'Notifications',
                    onPressed: () => ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text(
                          'Notifications are not connected in preview mode.',
                        ),
                      ),
                    ),
                    icon: const Icon(Icons.notifications_none_rounded),
                  ),
                ],
              ),
              const SizedBox(height: AppTokens.space24),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppTokens.space12,
                  vertical: AppTokens.space8,
                ),
                decoration: BoxDecoration(
                  color: colors.secondaryContainer,
                  borderRadius: BorderRadius.circular(AppTokens.radiusMedium),
                ),
                child: Text(
                  'Preview mode · Sample financial data',
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.labelMedium
                      ?.copyWith(color: colors.onSecondaryContainer),
                ),
              ),
              const SizedBox(height: AppTokens.space24),
              WalletBalanceCard(
                balance: 'ZMW 2,450.00',
                accountLabel: 'Main wallet',
                balanceVisible: _balanceVisible,
                onToggleVisibility: () =>
                    setState(() => _balanceVisible = !_balanceVisible),
              ),
              const SizedBox(height: AppTokens.space24),
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: const [
                        Text('Sample income'),
                        Text('This month · Sample'),
                        Text('ZMW 1,250.00'),
                      ],
                    ),
                  ),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: const [
                        Text('Sample spending'),
                        Text('ZMW 620.00'),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: AppTokens.space24),
              // P2 hierarchy: the primary wallet actions sit directly under
              // the balance, before the activity feed.
              const SectionHeader(title: 'Quick actions'),
              const SizedBox(height: AppTokens.space12),
              Wrap(
                spacing: AppTokens.space12,
                runSpacing: AppTokens.space12,
                children: [
                  for (final action in [
                    ('Send', Icons.send_rounded),
                    ('Receive', Icons.call_received_rounded),
                    ('Deposit', Icons.add_circle_outline_rounded),
                    ('Withdraw', Icons.payments_outlined),
                  ])
                    QuickActionButton(
                      label: action.$1,
                      icon: action.$2,
                      onTap: () => ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Text(
                            '${action.$1} is not connected in preview mode.',
                          ),
                        ),
                      ),
                    ),
                  QuickActionButton(
                    label: 'Discover',
                    icon: Icons.explore_outlined,
                    onTap: () {
                      final onDiscover = widget.onDiscover;
                      if (onDiscover != null) {
                        onDiscover();
                      } else {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text(
                              'Discover is not connected in preview mode.',
                            ),
                          ),
                        );
                      }
                    },
                  ),
                ],
              ),
              const SizedBox(height: AppTokens.space24),
              SectionHeader(
                title: 'Recent activity',
                actionLabel: 'View all',
                onAction: widget.onViewAll,
              ),
              const SizedBox(height: AppTokens.space8),
              for (final tx in sampleTransactions)
                TransactionTile(transaction: tx),
              const SizedBox(height: AppTokens.space24),
            ],
          ),
        ),
      ),
    );
  }
}
