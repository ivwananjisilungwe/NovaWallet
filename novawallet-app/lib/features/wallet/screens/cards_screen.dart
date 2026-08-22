import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/design_tokens.dart';
import '../../../core/widgets/widgets.dart';
import '../data/cards_demo_data.dart';

/// Virtual cards — Tab #3. Backend cards are V2/post-MVP, so this renders
/// the intended experience with design-stage demo data from
/// [demoCards] (per design 20).
///
/// Horizontal gradient card carousel, quick actions (freeze / PIN / settings),
/// recent card transactions, and an "Add a new card" CTA. Freeze state is
/// tracked locally; all other actions surface a "coming soon" snackbar.
///
/// Color strategy mirrors the dashboard hero card: gradient card faces are
/// theme-invariant brand surfaces painted from static [SemanticColors], while
/// screen chrome (background, dots, action tiles) reads
/// [Theme.of].colorScheme so dark mode renders correctly.
class CardsScreen extends ConsumerStatefulWidget {
  const CardsScreen({super.key});

  @override
  ConsumerState<CardsScreen> createState() => _CardsScreenState();
}

class _CardsScreenState extends ConsumerState<CardsScreen> {
  final PageController _pageController = PageController(viewportFraction: 0.88);
  final Set<String> _frozenCardIds = {};
  int _currentIndex = 0;

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  DemoCard get _currentCard =>
      demoCards[_currentIndex.clamp(0, demoCards.length - 1)];

  void _toast(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }

  void _toggleFreeze(DemoCard card) {
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
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      backgroundColor: scheme.surface,
      appBar: AppBar(title: const Text('My Cards')),
      body: SafeArea(
        child: demoCards.isEmpty
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
                    cards: demoCards,
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
                        for (final tx in demoCardTransactions)
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
  final List<DemoCard> cards;
  final Set<String> frozenCardIds;
  final int currentIndex;
  final ValueChanged<int> onPageChanged;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
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
                      ? scheme.primary
                      : scheme.outlineVariant,
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

  final DemoCard card;
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
    final scheme = Theme.of(context).colorScheme;
    return Material(
      color: scheme.surfaceContainerLowest,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: scheme.outlineVariant),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 22, color: scheme.primary),
              const SizedBox(height: 6),
              Text(
                label,
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: scheme.onSurface,
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
///
/// The face is a theme-invariant brand surface (like the dashboard hero
/// balance card), so its ink comes from static [SemanticColors] rather than
/// the ambient [ColorScheme].
class _GradientCard extends StatelessWidget {
  const _GradientCard({required this.card, required this.frozen});

  final DemoCard card;
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
            color: SemanticColors.shadowTint.withValues(alpha: 0.22),
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
                          color: SemanticColors.onBrandPrimary.withValues(
                            alpha: 0.95,
                          ),
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
                      color: SemanticColors.onBrandPrimary,
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
                              color: SemanticColors.onBrandPrimary.withValues(
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
                              color: SemanticColors.onBrandPrimary,
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
                              color: SemanticColors.onBrandPrimary.withValues(
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
                              color: SemanticColors.onBrandPrimary,
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
                  color: SemanticColors.onBackground.withValues(alpha: 0.45),
                  alignment: Alignment.center,
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 6,
                    ),
                    decoration: BoxDecoration(
                      color: SemanticColors.warning,
                      borderRadius: BorderRadius.circular(999),
                    ),
                    child: const Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          Icons.lock_outline,
                          size: 14,
                          color: SemanticColors.onWarning,
                        ),
                        SizedBox(width: 6),
                        Text(
                          'FROZEN',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w700,
                            letterSpacing: 1,
                            color: SemanticColors.onWarning,
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
        color: CardsDemoArt.chipGold,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(
          color: SemanticColors.onBrandPrimary.withValues(alpha: 0.6),
        ),
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
      ..color = SemanticColors.onBrandPrimary.withValues(alpha: 0.6)
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
