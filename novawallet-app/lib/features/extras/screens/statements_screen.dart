import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/widgets.dart';

/// Statements (per design 28). Month summary + CSV/PDF export placeholders.
class StatementsScreen extends StatefulWidget {
  const StatementsScreen({super.key});

  @override
  State<StatementsScreen> createState() => _StatementsScreenState();
}

class _StatementsScreenState extends State<StatementsScreen> {
  DateTime _month = DateTime.now();

  void _shift(int delta) =>
      setState(() => _month = DateTime(_month.year, _month.month + delta));

  @override
  Widget build(BuildContext context) {
    final label = DateFormat('MMMM yyyy').format(_month);
    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('Statements')),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  IconButton(
                    onPressed: () => _shift(-1),
                    icon: const Icon(Icons.chevron_left),
                  ),
                  Text(
                    label,
                    style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: NovaColors.onBackground),
                  ),
                  IconButton(
                    onPressed: () => _shift(1),
                    icon: const Icon(Icons.chevron_right),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [NovaColors.primary, NovaColors.primaryContainer],
                  ),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Column(
                  children: const [
                    Text('Statement summary', style: TextStyle(color: Colors.white70)),
                    SizedBox(height: 8),
                    Text('ZMW 0.00', style: TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.w700)),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              const EmptyStateView(
                icon: Icons.description_outlined,
                title: 'No statements generated yet',
                message: 'CSV and PDF export will appear here once you have activity.',
              ),
            ],
          ),
        ),
      ),
    );
  }
}