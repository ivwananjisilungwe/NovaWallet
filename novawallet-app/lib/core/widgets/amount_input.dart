import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/nova_colors.dart';

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
              color: NovaColors.amountInputHint,
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
                    HapticFeedback.selectionClick();
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
