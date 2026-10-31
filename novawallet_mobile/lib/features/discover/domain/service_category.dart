import 'package:flutter/material.dart';

/// Availability of a Discover catalogue entry, as proven by the repository.
///
/// Nothing in this catalogue may claim a live provider, payment rail or card
/// issuer unless the app actually implements it. Keep that distinction explicit
/// at the UI level instead of relying on prose.
enum ServiceAvailability {
  /// Implemented and usable right now.
  available,

  /// On the product roadmap, with no functional integration yet. Selecting the
  /// entry explains the state honestly instead of faking a flow.
  planned,

  /// Not offered in this preview, or not available to the current user.
  unavailable,
}

extension on ServiceAvailability {
  String get label => switch (this) {
    ServiceAvailability.available => 'Available',
    ServiceAvailability.planned => 'Planned',
    ServiceAvailability.unavailable => 'Unavailable',
  };
}

String describeAvailability(ServiceAvailability availability) =>
    availability.label;

/// A single entry in the Discover & Services catalogue.
class ServiceCategory {
  const ServiceCategory({
    required this.id,
    required this.title,
    required this.summary,
    required this.icon,
    required this.availability,
    this.footnote,
  });

  final String id;
  final String title;
  final String summary;
  final IconData icon;
  final ServiceAvailability availability;

  /// Context shown alongside an entry — for example why card services are not
  /// offered in this preview.
  final String? footnote;

  bool get isPlanned => availability == ServiceAvailability.planned;

  bool get isUnavailable => availability == ServiceAvailability.unavailable;
}

/// The NovaWallet services catalogue.
///
/// Every entry here is [ServiceAvailability.planned] or
/// [ServiceAvailability.unavailable] because this build has no live service
/// integration: it is a UI foundation running on sample data, the payment
/// gateway module is disabled by default, and no card issuance, airtime, bill,
/// or merchant endpoint exists in the app. Do not mark an entry
/// [ServiceAvailability.available] until that flow is implemented and tested.
const List<ServiceCategory> discoverCatalogue = <ServiceCategory>[
  ServiceCategory(
    id: 'airtime',
    title: 'Airtime & bundles',
    summary: 'Airtime top-ups and data bundles for any local network.',
    icon: Icons.phone_android_outlined,
    availability: ServiceAvailability.planned,
  ),
  ServiceCategory(
    id: 'utility-bills',
    title: 'Utility bills',
    summary: 'Electricity, water, TV and school fees, all in one place.',
    icon: Icons.receipt_long_outlined,
    availability: ServiceAvailability.planned,
  ),
  ServiceCategory(
    id: 'merchant-payments',
    title: 'Merchant payments',
    summary: 'Merchant payments by code or NovaWallet ID.',
    icon: Icons.storefront_outlined,
    availability: ServiceAvailability.planned,
  ),
  ServiceCategory(
    id: 'digital-goods',
    title: 'Digital goods & subscriptions',
    summary: 'Streaming, gaming and app subscriptions from one place.',
    icon: Icons.shopping_bag_outlined,
    availability: ServiceAvailability.planned,
  ),
  ServiceCategory(
    id: 'card-services',
    title: 'Cards & card services',
    summary: 'Virtual and physical NovaWallet cards.',
    icon: Icons.credit_card_outlined,
    availability: ServiceAvailability.unavailable,
    footnote:
        'Cards are not offered in this preview. No card can be applied for '
        'or funded here.',
  ),
];
