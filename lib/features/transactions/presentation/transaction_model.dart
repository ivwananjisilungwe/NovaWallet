export '../data/transaction.dart';

enum TransactionKind { purchase, received, payment }

class SampleTransaction {
  const SampleTransaction({
    required this.title,
    required this.subtitle,
    required this.amount,
    required this.timestamp,
    required this.kind,
    required this.status,
  });

  final String title;
  final String subtitle;
  final String amount;
  final String timestamp;
  final TransactionKind kind;
  final String status;
}

const sampleTransactions = <SampleTransaction>[
  SampleTransaction(
    title: 'Campus Café',
    subtitle: 'Sample purchase · Today, 10:42',
    amount: '− ZMW 85.00',
    timestamp: 'Today, 10:42',
    kind: TransactionKind.purchase,
    status: 'Sample',
  ),
  SampleTransaction(
    title: 'Money received',
    subtitle: 'Sample incoming transfer · Yesterday, 16:18',
    amount: '+ ZMW 350.00',
    timestamp: 'Yesterday, 16:18',
    kind: TransactionKind.received,
    status: 'Sample',
  ),
  SampleTransaction(
    title: 'Mobile payment',
    subtitle: 'Sample payment · 12 May, 09:05',
    amount: '− ZMW 120.00',
    timestamp: '12 May, 09:05',
    kind: TransactionKind.payment,
    status: 'Sample',
  ),
];
