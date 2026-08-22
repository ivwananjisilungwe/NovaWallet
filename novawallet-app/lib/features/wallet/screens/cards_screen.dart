import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/nova_colors.dart';
import '../../../core/widgets/widgets.dart';

/// Virtual cards — Tab #3. Backend cards are V2/post-MVP, so this renders
/// the intended experience with mock card data (per design 20).
///
/// Horizontal gradient card carousel, quick actions (freeze / PIN / settings),
/// recent card transactions, and an "Add a new card" CTA. Freeze state is
/// tracked locally; all other actions surface a "coming soon" snackbar.
class CardsScreen extends ConsumerStatefulWidget {
  const CardsScreen({super.key});

  @override
  ConsumerState<CardsScreen> createState() => _CardsScreenState();
}

class _CardsScreenState extends ConsumerState<CardsScreen> {
  final PageController _pageController = PageController(viewportFraction: 0.88);
  final Set<String> _frozenCardIds = {};
  int _currentIndex = 0;

  static const List<_MockCard> _demoCards = [
    _MockCard(
      id: 'card_indigo',
      holderName: 'CHIENGE K. MWANZA',
      lastFour: '4242',
      cardExpiry: '12/28',
      brand: 'VISA',
      startColor: NovaColors.primary,
      endColor: NovaColors.primaryContainer,
    ),
    _MockCard(
      id: 'card_purple',
      holderName: 'CHIENGE K. MWANZA',
      lastFour: '1892',
      cardExpiry: '09/29',
      brand: 'MASTERCARD',
      startColor: Color(0xFF7C3AED),
      endColor: Color(0xFFA855F7),
    ),
    _MockCard(
      id: 'card_teal',
      holderName: 'CHIENGE K. MWANZA',
      lastFour: '7703',
      cardExpiry: '04/30',
      brand: 'VISA',
      startColor: Color(0xFF0D9488),
      endColor: Color(0xFF2DD4BF),
    ),
  ];

  // Recent card transactions (mock): [title, subtitle, amountText, incoming].
  static const List<_MockCardTransaction> _recentTransactions = [
    _MockCardTransaction(
      title: 'Nova Grocers',
      subtitle: 'Card payment • Lusaka',
      amountText: 'ZMW 245.00',
      incoming: false,
    ),
    _MockCardTransaction(
      title: 'Zamtel Airtime',
      subtitle: 'Card top-up • Online',
      amountText: 'ZMW 50.00',
      incoming: false,
    ),
    _MockCardTransaction(
      title: 'Nova Kiosk Refund',
      subtitle: 'Refund • Card',
      amountText: 'ZMW 45.50',
      incoming: true,
    ),
    _MockCardTransaction(
      title: 'Shoprite Checkers',
      subtitle: 'Card payment • Lusaka',
      amountText: 'ZMW 312.80',
      incoming: false,
    ),
  ];

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  _MockCard get _currentCard =>
      _demoCards[_currentIndex.clamp(0, _demoCards.length - 1)];

  void _toast(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }

  void _toggleFreeze(_MockCard card) {
    setState(() {
      if (_frozenCardIds.contains(card.id)) {
        _frozenCardIds.remove(card.id);
        _toast('Card unfrozen');
      } else {
        _frozenCardIds.add(card.id);
        _toast('Card frozen');
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NovaColors.background,
      appBar: AppBar(title: const Text('My Cards')),
      body: SafeArea(
        child: _demoCards.isEmpty
            ? EmptyStateView(
                icon: Icons.credit_card_off_outlined,
                title: 'No cards yet',
                message: 'Add a virtual card to pay securely online.',
                actionLabel: 'Add a new card',
                onAction: () => _toast('Card issuance coming soon'),
              )
            : ListView(
                padding: const EdgeInsets.symmetric(vertical: 20),
                children: [
                  _CardCarousel(
                    controller: _pageController,
                    cards: _demoCards,
                    frozenCardIds: _frozenCardIds,
                    currentIndex: _currentIndex,
                    onPageChanged: (index) =>
                        setState(() => _currentIndex = index),
                  ),
                  const SizedBox(height: 20),
                  _CardActions(
                    card: _currentCard,
                    frozen: _frozenCardIds.contains(_currentCard.id),
                    onFreeze: () => _toggleFreeze(_currentCard),
                    onViewPin: () => _toast('PIN reveal coming soon'),
                    onSettings: () => _toast('Card settings coming soon'),
                  ),
                  const SizedBox(height: 28),
                  const Padding(
                    padding: EdgeInsets.symmetric(horizontal: 20),
                    child: SectionHeader(title: 'Recent transactions'),
                  ),
                  const SizedBox(height: 12),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    child: Column(
                      children: [
                        for (final tx in _recentTransactions)
                          Padding(
                            padding: const EdgeInsets.only(bottom: 8),
                            child: TransactionTile(
                              title: tx.title,
                              subtitle: tx.subtitle,
                              amountText: tx.amountText,
                              incoming: tx.incoming,
                            ),
                          ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 20),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    child: PillButton(
                      label: 'Add a new card',
                      variant: PillButtonVariant.outlined,
                      icon: Icons.add,
                      onPressed: () => _toast('Card issuance coming soon'),
                    ),
                  ),
                  const SizedBox(height: 24),
                ],
              ),
      ),
    );
  }
}

/// Horizontal snap-scrolling carousel of gradient cards with page dots.
class _CardCarousel extends StatelessWidget {
  const _CardCarousel({
    required this.controller,
    required this.cards,
    required this.frozenCardIds,
    required this.currentIndex,
    required this.onPageChanged,
  });

  final PageController controller;
  final List<_MockCard> cards;
  final Set<String> frozenCardIds;
  final int currentIndex;
  final ValueChanged<int> onPageChanged;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        SizedBox(
          height: 210,
          child: PageView.builder(
            controller: controller,
            itemCount: cards.length,
            onPageChanged: onPageChanged,
            itemBuilder: (context, index) {
              final card = cards[index];
              return Padding(
                padding: const EdgeInsets.symmetric(horizontal: 6),
                child: _GradientCard(
                  card: card,
                  frozen: frozenCardIds.contains(card.id),
                ),
              );
            },
          ),
        ),
        const SizedBox(height: 14),
        // Page indicator dots.
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            for (var i = 0; i < cards.length; i++)
              AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                margin: const EdgeInsets.symmetric(horizontal: 3),
                width: i == currentIndex ? 20 : 7,
                height: 7,
                decoration: BoxDecoration(
                  color: i == currentIndex
                      ? NovaColors.primary
                      : NovaColors.outlineVariant,
                  borderRadius: BorderRadius.circular(999),
                ),
              ),
          ],
        ),
      ],
    );
  }
}

/// Freeze / View PIN / Settings quick actions for the visible card.
class _CardActions extends StatelessWidget {
  const _CardActions({
    required this.card,
    required this.frozen,
    required this.onFreeze,
    required this.onViewPin,
    required this.onSettings,
  });

  final _MockCard card;
  final bool frozen;
  final VoidCallback onFreeze;
  final VoidCallback onViewPin;
  final VoidCallback onSettings;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Row(
        children: [
          Expanded(
            child: _ActionButton(
              icon: frozen ? Icons.lock_open : Icons.ac_unit,
              label: frozen ? 'Unfreeze' : 'Freeze',
              onTap: onFreeze,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: _ActionButton(
              icon: Icons.pin_outlined,
              label: 'View PIN',
              onTap: onViewPin,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: _ActionButton(
              icon: Icons.settings_outlined,
              label: 'Settings',
              onTap: onSettings,
            ),
          ),
        ],
      ),
    );
  }
}

/// Outlined icon + label tile used by the card quick actions.
class _ActionButton extends StatelessWidget {
  const _ActionButton({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: NovaColors.surfaceContainerLowest,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: NovaColors.outlineVariant),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 22, color: NovaColors.primary),
              const SizedBox(height: 6),
              Text(
                label,
                style: const TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: NovaColors.onSurface,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Gradient payment-card face: chip, masked number, holder, expiry, brand.
class _GradientCard extends StatelessWidget {
  const _GradientCard({required this.card, required this.frozen});

  final _MockCard card;
  final bool frozen;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [card.startColor, card.endColor],
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: NovaColors.shadowTint.withValues(alpha: 0.22),
            blurRadius: 18,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(24),
        child: Stack(
          fit: StackFit.expand,
          children: [
            Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  // Chip + brand row.
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const _CardChip(),
                      Text(
                        card.brand,
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w800,
                          fontStyle: FontStyle.italic,
                          letterSpacing: 2,
                          color: NovaColors.onPrimary.withValues(alpha: 0.95),
                        ),
                      ),
                    ],
                  ),
                  // Masked card number.
                  Text(
                    '••••  ••••  ••••  ${card.lastFour}',
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w600,
                      letterSpacing: 2.2,
                      color: NovaColors.onPrimary,
                    ),
                  ),
                  // Holder + expiry.
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'CARD HOLDER',
                            style: TextStyle(
                              fontSize: 9,
                              letterSpacing: 1.2,
                              color: NovaColors.onPrimary.withValues(
                                alpha: 0.7,
                              ),
                            ),
                          ),
                          const SizedBox(height: 3),
                          Text(
                            card.holderName,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              letterSpacing: 0.8,
                              color: NovaColors.onPrimary,
                            ),
                          ),
                        ],
                      ),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Text(
                            'EXPIRES',
                            style: TextStyle(
                              fontSize: 9,
                              letterSpacing: 1.2,
                              color: NovaColors.onPrimary.withValues(
                                alpha: 0.7,
                              ),
                            ),
                          ),
                          const SizedBox(height: 3),
                          Text(
                            card.cardExpiry,
                            style: const TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w700,
                              letterSpacing: 1,
                              color: NovaColors.onPrimary,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ],
              ),
            ),
            // Frosted overlay when the card is frozen.
            if (frozen)
              Positioned.fill(
                child: Container(
                  color: NovaColors.onBackground.withValues(alpha: 0.45),
                  alignment: Alignment.center,
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 6,
                    ),
                    decoration: BoxDecoration(
                      color: NovaColors.warning,
                      borderRadius: BorderRadius.circular(999),
                    ),
                    child: const Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          Icons.lock_outline,
                          size: 14,
                          color: NovaColors.onWarning,
                        ),
                        SizedBox(width: 6),
                        Text(
                          'FROZEN',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w700,
                            letterSpacing: 1,
                            color: NovaColors.onWarning,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

/// Small metallic payment-card chip with contact-lines pattern.
class _CardChip extends StatelessWidget {
  const _CardChip();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 40,
      height: 30,
      decoration: BoxDecoration(
        color: const Color(0xFFE6C563),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.white.withValues(alpha: 0.6)),
      ),
      child: const CustomPaint(painter: _ChipPainter()),
    );
  }
}

class _ChipPainter extends CustomPainter {
  const _ChipPainter();

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.white.withValues(alpha: 0.6)
      ..strokeWidth = 1;
    final midX = size.width / 2;
    canvas.drawLine(Offset(midX, 0), Offset(midX, size.height), paint);
    final third = size.height / 3;
    canvas.drawLine(Offset(0, third), Offset(size.width, third), paint);
    canvas.drawLine(Offset(0, third * 2), Offset(size.width, third * 2), paint);
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        Rect.fromLTWH(midX - 5, 0, 10, third),
        const Radius.circular(2),
      ),
      paint,
    );
  }

  @override
  bool shouldRepaint(covariant _ChipPainter oldDelegate) => false;
}

/// Mock card data shown until the backend card service ships.
class _MockCard {
  const _MockCard({
    required this.id,
    required this.holderName,
    required this.lastFour,
    required this.cardExpiry,
    required this.brand,
    required this.startColor,
    required this.endColor,
  });

  final String id;
  final String holderName;
  final String lastFour;
  final String cardExpiry;
  final String brand;
  final Color startColor;
  final Color endColor;
}

/// Mock recent card transaction (post-MVP shape of a card ledger entry).
class _MockCardTransaction {
  const _MockCardTransaction({
    required this.title,
    required this.subtitle,
    required this.amountText,
    required this.incoming,
  });

  final String title;
  final String subtitle;
  final String amountText;
  final bool incoming;
}
