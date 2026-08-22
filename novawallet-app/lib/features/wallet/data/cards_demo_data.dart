/// Design-stage demo data for the virtual-cards experience.
///
/// Backend cards are V2/post-MVP, so the cards screen renders the intended
/// experience from these static placeholders (per design 20). When the cards
/// domain ships on the API, delete this file and map real domain models
/// (card art themes, card ledger entries) instead — the screen consumes only
/// the types and lists exported here.
library;

import 'package:flutter/material.dart';

import '../../../core/theme/design_tokens.dart';

/// One-off decorative anchors for the demo payment-card artwork.
///
/// These live here rather than in the design-token layer because they are
/// marketing-style gradient stops for demo card faces, not reusable semantic
/// roles. They are intentionally theme-invariant: payment-card faces keep
/// their brand gradients in both light and dark mode (same convention as the
/// dashboard hero balance card).
abstract final class CardsDemoArt {
  /// Violet gradient face (demo MASTERCARD).
  static const Color violetStart = Color(0xFF7C3AED);
  static const Color violetEnd = Color(0xFFA855F7);

  /// Teal gradient face (demo VISA #3).
  static const Color tealStart = Color(0xFF0D9488);
  static const Color tealEnd = Color(0xFF2DD4BF);

  /// Metallic EMV chip body.
  static const Color chipGold = Color(0xFFE6C563);
}

/// A demo payment card shown until the backend card service ships.
class DemoCard {
  const DemoCard({
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

  /// Gradient anchors of the card face (theme-invariant brand surface).
  final Color startColor;
  final Color endColor;
}

/// A demo recent card transaction (post-MVP shape of a card ledger entry).
class DemoCardTransaction {
  const DemoCardTransaction({
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

/// Demo cards rendered by the carousel until issuance exists.
const List<DemoCard> demoCards = [
  DemoCard(
    id: 'card_indigo',
    holderName: 'CHIENGE K. MWANZA',
    lastFour: '4242',
    cardExpiry: '12/28',
    brand: 'VISA',
    startColor: SemanticColors.brandPrimary,
    endColor: SemanticColors.brandPrimaryContainer,
  ),
  DemoCard(
    id: 'card_purple',
    holderName: 'CHIENGE K. MWANZA',
    lastFour: '1892',
    cardExpiry: '09/29',
    brand: 'MASTERCARD',
    startColor: CardsDemoArt.violetStart,
    endColor: CardsDemoArt.violetEnd,
  ),
  DemoCard(
    id: 'card_teal',
    holderName: 'CHIENGE K. MWANZA',
    lastFour: '7703',
    cardExpiry: '04/30',
    brand: 'VISA',
    startColor: CardsDemoArt.tealStart,
    endColor: CardsDemoArt.tealEnd,
  ),
];

/// Recent card transactions shown under the carousel (mock ledger entries):
/// title, subtitle, display amount, direction.
const List<DemoCardTransaction> demoCardTransactions = [
  DemoCardTransaction(
    title: 'Nova Grocers',
    subtitle: 'Card payment • Lusaka',
    amountText: 'ZMW 245.00',
    incoming: false,
  ),
  DemoCardTransaction(
    title: 'Zamtel Airtime',
    subtitle: 'Card top-up • Online',
    amountText: 'ZMW 50.00',
    incoming: false,
  ),
  DemoCardTransaction(
    title: 'Nova Kiosk Refund',
    subtitle: 'Refund • Card',
    amountText: 'ZMW 45.50',
    incoming: true,
  ),
  DemoCardTransaction(
    title: 'Shoprite Checkers',
    subtitle: 'Card payment • Lusaka',
    amountText: 'ZMW 312.80',
    incoming: false,
  ),
];
