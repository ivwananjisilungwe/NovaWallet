import 'package:flutter/material.dart';

import 'package:novawallet_mobile/core/design/app_tokens.dart';
import 'package:novawallet_mobile/features/discover/domain/service_category.dart';
import 'package:novawallet_mobile/shared/section_header.dart';

/// Discover & Services catalogue.
///
/// Presents NovaWallet's planned services honestly: every entry is either
/// [ServiceAvailability.planned] or [ServiceAvailability.unavailable] because
/// this build has no live service integration. Planned entries explain their
/// state when selected; unavailable entries are clearly non-interactive and
/// carry their reason in place, so nothing here implies that money moves.
class DiscoverPage extends StatelessWidget {
  const DiscoverPage({super.key});

  static const String _previewNote =
      'Services are in preview. Nothing here is connected to a provider yet, '
      'so no payment, top-up or card can be made from this screen.';

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final planned = discoverCatalogue
        .where((service) => service.isPlanned)
        .toList();
    final unavailable = discoverCatalogue
        .where((service) => service.isUnavailable)
        .toList();

    return Scaffold(
      appBar: AppBar(title: const Text('Discover')),
      body: SafeArea(
        child: Align(
          alignment: Alignment.topCenter,
          child: ConstrainedBox(
            constraints: const BoxConstraints(
              maxWidth: AppTokens.contentMaxWidth,
            ),
            child: LayoutBuilder(
              builder: (context, constraints) {
                final columns = constraints.maxWidth >= AppTokens.tileBreakpoint
                    ? 2
                    : 1;
                return ListView(
                  key: const PageStorageKey<String>('discover-scroll'),
                  padding: AppTokens.pagePadding,
                  children: [
                    Text('Services', style: theme.textTheme.headlineMedium),
                    const SizedBox(height: AppTokens.space8),
                    Text(
                      'Everything your wallet will let you do, gathered in one '
                      'place.',
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: AppTokens.space24),
                    const _AvailabilityLegend(),
                    const SizedBox(height: AppTokens.space16),
                    const _PreviewNote(note: _previewNote),
                    const SectionHeader(title: 'Coming soon'),
                    const SizedBox(height: AppTokens.space12),
                    _ServiceSection(
                      services: planned,
                      columns: columns,
                      emptyMessage: 'Nothing is on the roadmap yet.',
                    ),
                    const SizedBox(height: AppTokens.space32),
                    const SectionHeader(title: 'Not available yet'),
                    const SizedBox(height: AppTokens.space12),
                    _ServiceSection(
                      services: unavailable,
                      columns: columns,
                      emptyMessage: 'Nothing is unavailable right now.',
                    ),
                    const SizedBox(height: AppTokens.space24),
                  ],
                );
              },
            ),
          ),
        ),
      ),
    );
  }
}

/// Legend explaining what each availability badge means.
class _AvailabilityLegend extends StatelessWidget {
  const _AvailabilityLegend();

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: _LegendDot(
            color: colors.primary,
            label: 'Planned',
            description: 'on the roadmap, not built yet',
          ),
        ),
        const SizedBox(width: AppTokens.space12),
        Expanded(
          child: _LegendDot(
            color: colors.outline,
            label: 'Unavailable',
            description: 'not offered in this preview',
          ),
        ),
      ],
    );
  }
}

class _LegendDot extends StatelessWidget {
  const _LegendDot({
    required this.color,
    required this.label,
    required this.description,
  });

  final Color color;
  final String label;
  final String description;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Row(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: AppTokens.space8,
          height: AppTokens.space8,
          margin: const EdgeInsets.only(top: AppTokens.space4),
          decoration: BoxDecoration(shape: BoxShape.circle, color: color),
        ),
        const SizedBox(width: AppTokens.space8),
        Flexible(
          child: Text(
            '$label · $description',
            style: theme.textTheme.labelMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ),
      ],
    );
  }
}

/// Honest disclosure that no service on this screen performs any transaction.
class _PreviewNote extends StatelessWidget {
  const _PreviewNote({required this.note});

  final String note;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      width: double.infinity,
      padding: AppTokens.cardPadding,
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(AppTokens.radiusLarge),
        border: Border.all(color: theme.colorScheme.outlineVariant),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            Icons.info_outline,
            size: AppTokens.iconFrameIconSize,
            color: theme.colorScheme.onSurfaceVariant,
          ),
          const SizedBox(width: AppTokens.space12),
          Expanded(
            child: Text(
              note,
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// A catalogue group laid out as fixed-width rows of equal tiles so every tile
/// has the same intrinsic height and long copy never overflows.
class _ServiceSection extends StatelessWidget {
  const _ServiceSection({
    required this.services,
    required this.columns,
    required this.emptyMessage,
  });

  final List<ServiceCategory> services;
  final int columns;
  final String emptyMessage;

  @override
  Widget build(BuildContext context) {
    if (services.isEmpty) {
      return Text(
        emptyMessage,
        style: Theme.of(context).textTheme.bodyMedium
            ?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant),
      );
    }

    const gap = AppTokens.gridGutter;

    final rows = <Widget>[];
    for (var index = 0; index < services.length; index += columns) {
      final cells = <Widget>[];
      for (var column = 0; column < columns; column++) {
        if (index + column < services.length) {
          cells.add(
            Expanded(child: _ServiceTile(service: services[index + column])),
          );
        } else {
          cells.add(Spacer(flex: 1));
        }
      }
      // IntrinsicHeight bounds the stretch axis: this section sits inside a
      // scrollable column, so the row receives no height of its own.
      rows.add(
        IntrinsicHeight(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: cells,
          ),
        ),
      );
    }

    return Column(
      children: [
        for (final row in rows)
          Padding(
            padding: EdgeInsets.only(bottom: row == rows.last ? 0 : gap),
            child: row,
          ),
      ],
    );
  }
}

class _ServiceTile extends StatelessWidget {
  const _ServiceTile({required this.service});

  final ServiceCategory service;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final planned = service.isPlanned;
    final status = describeAvailability(service.availability);
    // Spoken state: a screen reader must hear the availability, not just a
    // badge word beside a title.
    final spokenLabel =
        '$status. '
        '${planned ? 'On the roadmap, not built yet.' : 'Not offered in this preview.'}';

    final leading = Container(
      width: AppTokens.iconFrameSize,
      height: AppTokens.iconFrameSize,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: planned
            ? colors.primaryContainer
            : colors.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(AppTokens.radiusMedium),
      ),
      child: Icon(
        service.icon,
        size: AppTokens.iconFrameIconSize,
        color: planned ? colors.onPrimaryContainer : colors.onSurfaceVariant,
      ),
    );

    final body = Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          service.title,
          style: theme.textTheme.titleSmall?.copyWith(
            fontWeight: FontWeight.w600,
            color: planned ? colors.onSurface : colors.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: AppTokens.space4),
        Text(
          service.footnote ?? service.summary,
          style: theme.textTheme.bodySmall?.copyWith(
            color: colors.onSurfaceVariant,
          ),
        ),
      ],
    );

    final badge = _AvailabilityBadge(label: status, emphasised: planned);

    final content = ConstrainedBox(
      constraints: const BoxConstraints(minHeight: AppTokens.tileMinHeight),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          leading,
          const SizedBox(width: AppTokens.space16),
          Expanded(child: body),
          const SizedBox(width: AppTokens.space12),
          // The badge may shrink rather than push the tile wide enough to
          // overflow on a 320 screen at double text scale.
          Flexible(child: badge),
        ],
      ),
    );

    final surface = DecoratedBox(
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(AppTokens.radiusLarge),
        border: Border.all(
          color: planned ? colors.outlineVariant : colors.outline,
        ),
      ),
      child: Padding(padding: AppTokens.cardPadding, child: content),
    );

    // Unavailable entries are read-only: no tap target, no feedback, and their
    // reason is shown in place instead of behind a tap.
    if (!planned) {
      return Semantics(
        label: '${service.title}. $spokenLabel',
        button: false,
        child: ExcludeSemantics(child: surface),
      );
    }

    return Material(
      color: colors.surface,
      borderRadius: BorderRadius.circular(AppTokens.radiusLarge),
      clipBehavior: Clip.antiAlias,
      child: Semantics(
        label: '${service.title}. $spokenLabel',
        child: ExcludeSemantics(
          child: InkWell(
            onTap: () => ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('This service is not connected in preview mode.'),
              ),
            ),
            borderRadius: BorderRadius.circular(AppTokens.radiusLarge),
            child: surface,
          ),
        ),
      ),
    );
  }
}

/// Availability chip for a catalogue entry.
///
/// The label is never allowed to grow its row: at double text scale on a
/// narrow screen it shrinks instead of forcing a horizontal overflow.
class _AvailabilityBadge extends StatelessWidget {
  const _AvailabilityBadge({required this.label, required this.emphasised});

  final String label;
  final bool emphasised;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppTokens.space12,
        vertical: AppTokens.space4,
      ),
      decoration: BoxDecoration(
        color: emphasised
            ? colors.primaryContainer
            : colors.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(AppTokens.radiusSmall),
        border: Border.all(color: emphasised ? colors.primary : colors.outline),
      ),
      child: Text(
        label,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: theme.textTheme.labelSmall?.copyWith(
          color: emphasised
              ? colors.onPrimaryContainer
              : colors.onSurfaceVariant,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
