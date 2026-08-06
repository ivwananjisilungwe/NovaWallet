import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/nova_colors.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/widgets.dart';
import '../providers/transaction_provider.dart';

/// Fee estimator (per design 14 + 34 disclosure). Shows live fee for a type.
class FeeEstimateScreen extends ConsumerStatefulWidget {
  const FeeEstimateScreen({super.key});

  @override
  ConsumerState<FeeEstimateScreen> createState() => _FeeEstimateScreenState();
}

class _FeeEstimateScreenState extends ConsumerState<FeeEstimateScreen> {
  final _amount = TextEditingController();
  String _type = 'TRANSFER';

  @override
  void dispose() {
    _amount.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final amount = _amount.text.trim().replaceAll(',', '');
    final valid = double.tryParse(amount) != null && double.parse(amount) > 0;
    final feeAsync = valid
        ? ref.watch(feeEstimateProvider((type: _type, amount: amount)))
        : null;

    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('Fee estimate')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              SegmentedButton<String>(
                segments: const [
                  ButtonSegment(value: 'TRANSFER', label: Text('Transfer')),
                  ButtonSegment(value: 'WITHDRAWAL', label: Text('Withdraw')),
                  ButtonSegment(value: 'DEPOSIT', label: Text('Deposit')),
                ],
                selected: {_type},
                onSelectionChanged: (s) => setState(() => _type = s.first),
              ),
              const SizedBox(height: 20),
              AmountInput(controller: _amount, onChanged: (_) => setState(() {})),
              const SizedBox(height: 24),
              if (feeAsync == null)
                const Text(
                  'Enter an amount to see the estimated fee.',
                  style: TextStyle(color: NovaColors.onSurfaceVariant),
                )
              else
                feeAsync.when(
                  loading: () => const LoadingView(message: 'Estimating…'),
                  error: (e, _) => ErrorStateView(message: '$e'),
                  data: (fee) => Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: NovaColors.surfaceContainerLowest,
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Column(
                      children: [
                        _Row(label: 'Amount', value: formatZmw(fee.amount)),
                        if (fee.percentageFee != null)
                          _Row(label: 'Percentage fee', value: '${fee.percentageFee}%'),
                        if (fee.flatFee != null && fee.flatFee != '0')
                          _Row(label: 'Flat fee', value: formatZmw(fee.flatFee)),
                        const Divider(height: 20),
                        _Row(label: 'Total fee', value: formatZmw(fee.totalFee), bold: true),
                      ],
                    ),
                  ),
                ),
              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }
}

class _Row extends StatelessWidget {
  const _Row({required this.label, required this.value, this.bold = false});

  final String label;
  final String value;
  final bool bold;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: TextStyle(color: NovaColors.onSurfaceVariant, fontWeight: bold ? FontWeight.w700 : FontWeight.w400)),
          Text(
            value,
            style: TextStyle(
              color: NovaColors.onSurface,
              fontWeight: bold ? FontWeight.w700 : FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}
