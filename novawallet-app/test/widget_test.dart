// Core widget smoke test — replaced with themed app-trees once features land.
import 'package:flutter_test/flutter_test.dart';

import 'package:novawallet_app/core/theme/app_theme.dart';

void main() {
  test('light theme exposes the brand palette', () {
    final theme = AppTheme.light;
    // Design system anchor: indigo primary.
    expect(theme.colorScheme.primary.toARGB32(), 0xFF3525CD);
  });
}