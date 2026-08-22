import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'package:novawallet_app/features/auth/data/biometric_service.dart';
import 'package:novawallet_app/features/auth/data/biometric_settings_store.dart';
import 'package:novawallet_app/features/auth/providers/biometric_provider.dart';

class MockBiometricService extends Mock implements BiometricService {}

class MockBiometricSettingsStore extends Mock
    implements BiometricSettingsStore {}

void main() {
  group('BiometricNotifier', () {
    late MockBiometricService mockService;
    late MockBiometricSettingsStore mockStore;
    late BiometricNotifier notifier;

    setUp(() {
      mockService = MockBiometricService();
      mockStore = MockBiometricSettingsStore();
      notifier = BiometricNotifier(mockService, mockStore);
      addTearDown(notifier.dispose);

      // Store writes are accepted by default in these tests.
      when(() => mockStore.save(any())).thenAnswer((_) async {});
    });

    test('default state is OFF, unchecked, and locked', () {
      expect(notifier.state.enabled, isFalse);
      expect(notifier.state.checked, isFalse);
      expect(notifier.state.deviceSupported, isFalse);
      expect(notifier.state.canCheckBiometrics, isFalse);
      expect(notifier.state.sessionUnlocked, isFalse);
      expect(notifier.state.usable, isFalse);
    });

    group('init', () {
      test('loads flag and probes capabilities', () async {
        when(() => mockStore.load()).thenAnswer((_) async => true);
        when(
          () => mockService.isDeviceSupported(),
        ).thenAnswer((_) async => true);
        when(
          () => mockService.canCheckBiometrics(),
        ).thenAnswer((_) async => true);

        await notifier.init();

        expect(notifier.state.enabled, isTrue);
        expect(notifier.state.checked, isTrue);
        expect(notifier.state.deviceSupported, isTrue);
        expect(notifier.state.canCheckBiometrics, isTrue);
        expect(notifier.state.usable, isTrue);
        verify(() => mockStore.load()).called(1);
      });

      test('flag OFF still probes capabilities (settings screen needs them)',
          () async {
        when(() => mockStore.load()).thenAnswer((_) async => false);
        when(
          () => mockService.isDeviceSupported(),
        ).thenAnswer((_) async => true);
        when(
          () => mockService.canCheckBiometrics(),
        ).thenAnswer((_) async => false);

        await notifier.init();

        expect(notifier.state.enabled, isFalse);
        expect(notifier.state.checked, isTrue);
        expect(notifier.state.deviceSupported, isTrue);
        expect(notifier.state.canCheckBiometrics, isFalse);
      });

      test('is idempotent — a second call does not re-probe', () async {
        when(() => mockStore.load()).thenAnswer((_) async => false);
        when(
          () => mockService.isDeviceSupported(),
        ).thenAnswer((_) async => false);
        when(
          () => mockService.canCheckBiometrics(),
        ).thenAnswer((_) async => false);

        await notifier.init();
        await notifier.init();

        verify(() => mockStore.load()).called(1);
      });
    });

    group('setEnabled', () {
      test('persists ON and updates state', () async {
        await notifier.setEnabled(true);

        expect(notifier.state.enabled, isTrue);
        verify(() => mockStore.save(true)).called(1);
      });

      test('turning OFF also disarms the current cycle', () async {
        notifier.markUnlocked();
        expect(notifier.state.sessionUnlocked, isTrue);

        await notifier.setEnabled(false);

        expect(notifier.state.enabled, isFalse);
        expect(notifier.state.sessionUnlocked, isFalse);
      });

      test('turning ON keeps the cycle locked until a prompt succeeds',
          () async {
        await notifier.setEnabled(true);

        expect(notifier.state.sessionUnlocked, isFalse);
      });
    });

    group('unlock / markUnlocked / requireUnlock', () {
      test('unlock delegates to the platform prompt', () async {
        when(
          () => mockService.authenticate(reason: any(named: 'reason')),
        ).thenAnswer((_) async => true);

        final result = await notifier.unlock();

        expect(result, isTrue);
        verify(
          () => mockService.authenticate(reason: any(named: 'reason')),
        ).called(1);
      });

      test('markUnlocked opens the gate for this cycle', () {
        notifier.markUnlocked();

        expect(notifier.state.sessionUnlocked, isTrue);
      });

      test('requireUnlock re-arms only when enabled and unlocked', () async {
        // Disabled + locked: no-op.
        notifier.requireUnlock();
        expect(notifier.state.sessionUnlocked, isFalse);

        // Enabled + unlocked: re-arms (resume from background).
        await notifier.setEnabled(true);
        notifier.markUnlocked();
        expect(notifier.state.sessionUnlocked, isTrue);
        notifier.requireUnlock();
        expect(notifier.state.sessionUnlocked, isFalse);

        // Already locked: stays locked.
        notifier.requireUnlock();
        expect(notifier.state.sessionUnlocked, isFalse);
      });
    });
  });
}
