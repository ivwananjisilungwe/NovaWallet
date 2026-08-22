import 'package:hive_flutter/hive_flutter.dart';

/// Lightweight Hive cache for offline resilience (Phase C).
///
/// Stores last-known wallet and transactions as JSON strings so cold start
/// renders cached data instantly when offline. Full stale-while-revalidate
/// wiring will read from these boxes before hitting the network.
class HiveBoxes {
  static const _walletBox = 'wallet';
  static const _txBox = 'transactions';
  static const _outbox = 'pending_ops';

  static Future<void> init() async {
    await Hive.initFlutter();
    await Future.wait([
      Hive.openBox<String>(_walletBox),
      Hive.openBox<String>(_txBox),
      Hive.openBox<String>(_outbox),
    ]);
  }

  static Box<String> get wallet => Hive.box<String>(_walletBox);
  static Box<String> get transactions => Hive.box<String>(_txBox);
  static Box<String> get outbox => Hive.box<String>(_outbox);
}
