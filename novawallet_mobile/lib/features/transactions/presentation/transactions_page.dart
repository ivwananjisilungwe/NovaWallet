import 'package:flutter/material.dart';

import 'package:novawallet_mobile/core/design/app_tokens.dart';
import 'package:novawallet_mobile/features/auth/data/auth_service.dart';
import 'package:novawallet_mobile/features/auth/data/session_store.dart';
import 'package:novawallet_mobile/features/auth/presentation/login_page.dart';
import 'package:novawallet_mobile/features/transactions/data/transaction_service.dart';
import 'package:novawallet_mobile/features/transactions/presentation/transaction_detail_page.dart';
import 'package:novawallet_mobile/features/transactions/presentation/transaction_model.dart';
import 'package:novawallet_mobile/features/wallet/data/wallet.dart';
import 'package:novawallet_mobile/features/wallet/data/wallet_service.dart';
import 'package:novawallet_mobile/shared/widgets/transaction_tile.dart';

class TransactionsPage extends StatefulWidget {
  const TransactionsPage({
    super.key,
    this.sessionStore,
    this.walletId,
    this.walletService,
    this.transactionService,
  });

  final SessionStore? sessionStore;
  final String? walletId;
  final WalletService? walletService;
  final TransactionService? transactionService;

  bool get _realMode =>
      sessionStore != null ||
      walletId != null ||
      walletService != null ||
      transactionService != null;

  @override
  State<TransactionsPage> createState() => _TransactionsPageState();
}

class _TransactionsPageState extends State<TransactionsPage> {
  bool _loading = true;
  String? _error;
  Wallet? _wallet;
  List<Transaction> _transactions = const <Transaction>[];
  bool _requestInFlight = false;

  WalletService get _walletService =>
      widget.walletService ??
      HttpWalletService(sessionStore: widget.sessionStore!);
  TransactionService get _transactionService =>
      widget.transactionService ??
      HttpTransactionService(sessionStore: widget.sessionStore!);

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
      Wallet? wallet = _wallet;
      if (wallet == null && widget.walletId == null) {
        wallet = await _walletService.getMyWallet();
      }
      final id = widget.walletId ?? wallet?.id;
      if (id == null || id.isEmpty) {
        throw const WalletNotFoundException();
      }
      final transactions = await _transactionService.getTransactions(id);
      if (!mounted) return;
      setState(() {
        _wallet = wallet;
        _transactions = transactions;
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
    } on WalletNotFoundException {
      if (!mounted) return;
      setState(() {
        _error = 'Your wallet is not available yet. Wallets are created after KYC approval.';
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = 'Unable to load your transactions. Please try again.';
        _loading = false;
      });
    } finally {
      _requestInFlight = false;
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!widget._realMode) {
      return _buildSample(context);
    }
    return _buildReal(context);
  }

  Widget _buildReal(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final currency = _wallet?.currency;
    return Scaffold(
      appBar: AppBar(title: const Text('Transaction History')),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          padding: AppTokens.pagePadding,
          children: [
            Text(
              'Transaction History',
              style: Theme.of(context).textTheme.headlineMedium,
            ),
            const SizedBox(height: AppTokens.space8),
            if (_wallet != null)
              Text(
                '${_wallet!.accountNumber} · ${currency == null ? '' : '$currency '}${_wallet!.balance}',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            const SizedBox(height: AppTokens.space20),
            if (_loading) ...[
              const Center(child: CircularProgressIndicator()),
            ] else if (_error != null) ...[
              Text(_error!, style: TextStyle(color: colors.error)),
              const SizedBox(height: AppTokens.space12),
              FilledButton(onPressed: _load, child: const Text('Retry')),
            ] else if (_transactions.isEmpty) ...[
              Text(
                'No transactions yet.',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ] else ...[
              for (final tx in _transactions)
                TransactionTile(
                  transaction: tx,
                  currency: currency,
                  onTap: () => Navigator.of(context).push(
                    MaterialPageRoute<void>(
                      builder: (_) => TransactionDetailPage(
                        transaction: tx,
                        transactionService: widget.transactionService,
                        sessionStore: widget.sessionStore,
                        currency: _wallet?.currency,
                      ),
                    ),
                  ),
                ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildSample(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Transaction History')),
      body: ListView(
        padding: AppTokens.pagePadding,
        children: [
          Text(
            'Transaction History',
            style: Theme.of(context).textTheme.headlineMedium,
          ),
          const SizedBox(height: AppTokens.space8),
          Text(
            'Your transactions',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: AppTokens.space8),
          Text(
            'These examples are sample data only.',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: AppTokens.space20),
          for (final transaction in sampleTransactions)
            TransactionTile(transaction: transaction),
        ],
      ),
    );
  }
}
