/// Shared NovaWallet UI building blocks (per DESIGN.md).
///
/// Pure export barrel: each widget lives in its own file under
/// `core/widgets/`, and this single library re-exports them so every screen
/// can keep `import 'core/widgets/widgets.dart'` and stay consistent without
/// duplicating component code.
library;

export 'amount_input.dart';
export 'balance_card.dart';
export 'empty_state_view.dart';
export 'error_state_view.dart';
export 'google_sign_in_button.dart';
export 'initials_avatar.dart';
export 'loading_view.dart';
export 'or_divider.dart';
export 'pill_button.dart';
export 'section_header.dart';
export 'status_chip.dart';
export 'transaction_tile.dart';
