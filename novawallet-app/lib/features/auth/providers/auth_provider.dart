import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/providers.dart';
import '../data/auth_repository.dart';
import '../models/user.dart';

/// Auth session state.
///
/// Serves two masters:
///  * the router (`refreshListenable` + `_redirect`) — needs sync reads of
///    `isAuthenticated` / `hasPin`,
///  * every screen — consumes `authProvider` to get the current [User].
///
/// Token persistence (secure storage) is owned by [AuthRepository].
class AuthState {
  const AuthState({
    this.status = AuthStatus.unknown,
    this.user,
    this.hasPin = false,
  });

  final AuthStatus status;
  final User? user;
  final bool hasPin;

  bool get isAuthenticated => status == AuthStatus.authenticated;
  bool get isInitializing => status == AuthStatus.unknown;
  bool get isAdmin => user?.isAdmin ?? false;
}

enum AuthStatus { unknown, unauthenticated, authenticated }

class AuthNotifier extends ChangeNotifier {
  AuthNotifier(this._repo);

  final AuthRepository _repo;

  AuthState _state = const AuthState();
  AuthState get state => _state;

  /// Restores a persisted session on cold start.
  Future<void> restore() async {
    final user = await _repo.restoreSession();
    final hasPin = await _repo.hasPin();
    _state = AuthState(
      status: user == null ? AuthStatus.unauthenticated : AuthStatus.authenticated,
      user: user,
      hasPin: hasPin,
    );
    notifyListeners();
  }

  Future<AuthResponse> register({
    required String firstName,
    required String lastName,
    required String email,
    required String phone,
    required String password,
  }) async {
    final auth = await _repo.register(
      firstName: firstName,
      lastName: lastName,
      email: email,
      phone: phone,
      password: password,
    );
    _apply(auth);
    return auth;
  }

  Future<AuthResponse> login({required String email, required String password}) async {
    final auth = await _repo.login(email: email, password: password);
    _apply(auth);
    return auth;
  }

  Future<void> setPin(String pin) async {
    await _repo.setPin(pin);
    await _repo.markPinSet();
    _state = AuthState(
      status: AuthStatus.authenticated,
      user: _state.user,
      hasPin: true,
    );
    notifyListeners();
  }

  Future<void> forgotPassword(String email) => _repo.forgotPassword(email);

  Future<void> resetPassword({
    required String token,
    required String newPassword,
  }) =>
      _repo.resetPassword(token: token, newPassword: newPassword);

  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
  }) =>
      _repo.changePassword(
        currentPassword: currentPassword,
        newPassword: newPassword,
      );

  Future<void> verifyEmail(String token) => _repo.verifyEmail(token);

  Future<void> logout() async {
    await _repo.logout();
    _state = const AuthState(status: AuthStatus.unauthenticated, hasPin: false);
    notifyListeners();
  }

  /// Apply a fresh session payload and persist it.
  void _apply(AuthResponse auth) {
    _state = AuthState(
      status: AuthStatus.authenticated,
      user: auth.user ?? _state.user,
      hasPin: _state.hasPin,
    );
    notifyListeners();
  }
}

/// App-wide auth provider. Read synchronously in redirects.
final authProvider = ChangeNotifierProvider<AuthNotifier>((ref) {
  final api = ref.watch(apiClientProvider);
  final storage = ref.watch(tokenStorageProvider);
  final notifier = AuthNotifier(AuthRepository(api, storage));
  // Kick off session restore without blocking first frame.
  Future.microtask(notifier.restore);
  return notifier;
});