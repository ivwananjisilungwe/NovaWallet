import 'package:flutter_test/flutter_test.dart';

import 'package:novawallet_app/features/auth/models/user.dart';
import 'package:novawallet_app/features/kyc/models/kyc_status.dart';
import 'package:novawallet_app/features/profile/data/user_repository.dart';
import 'package:novawallet_app/features/transaction/models/fee_estimate.dart';
import 'package:novawallet_app/features/transaction/models/transaction.dart';
import 'package:novawallet_app/features/wallet/models/wallet.dart';

void main() {
  group('User', () {
    test('fromJson parses all fields including nested wallet', () {
      final user = User.fromJson(<String, dynamic>{
        'id': 'user-1',
        'firstName': 'John',
        'lastName': 'Doe',
        'email': 'john@test.com',
        'phone': '+260971234567',
        'role': 'ADMIN',
        'kycStatus': 'APPROVED',
        'kycTier': 2,
        'wallet': <String, dynamic>{
          'id': 'wallet-1',
          'accountNumber': 'NW-100001',
          'balance': '2500.75',
          'status': 'ACTIVE',
        },
      });

      expect(user.id, 'user-1');
      expect(user.firstName, 'John');
      expect(user.lastName, 'Doe');
      expect(user.email, 'john@test.com');
      expect(user.phone, '+260971234567');
      expect(user.role, 'ADMIN');
      expect(user.isAdmin, isTrue);
      expect(user.kycStatus, 'APPROVED');
      expect(user.kycTier, 2);
      expect(user.walletId, 'wallet-1');
      expect(user.accountNumber, 'NW-100001');
      expect(user.balanceZmw, '2500.75');
      expect(user.walletStatus, 'ACTIVE');
    });

    test('fromJson/toJson round-trips all fields', () {
      final user = User.fromJson(<String, dynamic>{
        'id': 'user-1',
        'firstName': 'John',
        'lastName': 'Doe',
        'email': 'john@test.com',
        'phone': '+260971234567',
        'role': 'ADMIN',
        'kycStatus': 'APPROVED',
        'kycTier': 2,
        'walletId': 'wallet-1',
        'accountNumber': 'NW-100001',
        'balance': '2500.75',
        'walletStatus': 'ACTIVE',
      });

      final roundTrip = User.fromJson(user.toJson());

      expect(roundTrip.id, user.id);
      expect(roundTrip.firstName, user.firstName);
      expect(roundTrip.lastName, user.lastName);
      expect(roundTrip.email, user.email);
      expect(roundTrip.phone, user.phone);
      expect(roundTrip.role, user.role);
      expect(roundTrip.kycStatus, user.kycStatus);
      expect(roundTrip.kycTier, user.kycTier);
      expect(roundTrip.walletId, user.walletId);
      expect(roundTrip.accountNumber, user.accountNumber);
      expect(roundTrip.balanceZmw, user.balanceZmw);
      expect(roundTrip.walletStatus, user.walletStatus);
    });

    test('fromJson falls back to defaults for missing fields', () {
      final user = User.fromJson(<String, dynamic>{'id': ''});

      expect(user.id, '');
      expect(user.role, 'USER');
      expect(user.isAdmin, isFalse);
      expect(user.fullName, 'there');
      expect(user.kycStatus, isNull);
      expect(user.kycTier, isNull);
      expect(user.walletId, isNull);
      expect(user.walletStatus, isNull);
    });
  });

  group('AuthResponse', () {
    test('fromJson parses tokens and nested user', () {
      final auth = AuthResponse.fromJson(<String, dynamic>{
        'accessToken': 'access123',
        'refreshToken': 'refresh456',
        'user': <String, dynamic>{
          'id': 'user-1',
          'firstName': 'John',
          'email': 'john@test.com',
        },
      });

      expect(auth.accessToken, 'access123');
      expect(auth.refreshToken, 'refresh456');
      expect(auth.user, isNotNull);
      expect(auth.user!.id, 'user-1');
      expect(auth.user!.email, 'john@test.com');
    });

    test('fromJson defaults tokens and user when absent', () {
      final auth = AuthResponse.fromJson(<String, dynamic>{});

      expect(auth.accessToken, '');
      expect(auth.refreshToken, '');
      expect(auth.user, isNull);
    });
  });

  group('Wallet', () {
    test('fromJson parses all fields and createdAt', () {
      final wallet = Wallet.fromJson(<String, dynamic>{
        'id': 'wallet-1',
        'accountNumber': 'NW-100001',
        'balance': '1000.00',
        'currency': 'ZMW',
        'status': 'ACTIVE',
        'freezeReason': null,
        'createdAt': '2026-07-10T09:00:00',
      });

      expect(wallet.id, 'wallet-1');
      expect(wallet.accountNumber, 'NW-100001');
      expect(wallet.balance, '1000.00');
      expect(wallet.currency, 'ZMW');
      expect(wallet.status, 'ACTIVE');
      expect(wallet.freezeReason, isNull);
      expect(wallet.createdAt, DateTime.parse('2026-07-10T09:00:00'));
      expect(wallet.isFrozen, isFalse);
      expect(wallet.maskedAccount, 'NW •••0001');
    });

    test('fromJson applies defaults from empty map', () {
      final wallet = Wallet.fromJson(<String, dynamic>{});

      expect(wallet.id, '');
      expect(wallet.accountNumber, '');
      expect(wallet.balance, '0');
      expect(wallet.currency, 'ZMW');
      expect(wallet.status, 'ACTIVE');
      expect(wallet.createdAt, isNull);
      expect(wallet.isFrozen, isFalse);
    });
  });

  group('WalletTransaction', () {
    test('fromJson parses all fields', () {
      final txn = WalletTransaction.fromJson(<String, dynamic>{
        'id': 'txn-1',
        'reference': 'REF-001',
        'type': 'TRANSFER_CREDIT',
        'amount': '500.00',
        'status': 'SUCCESSFUL',
        'balanceBefore': '100.00',
        'balanceAfter': '600.00',
        'description': 'Money received',
        'senderWalletId': 'wallet-2',
        'receiverWalletId': 'wallet-1',
        'createdAt': '2026-07-10T09:00:00',
      });

      expect(txn.id, 'txn-1');
      expect(txn.reference, 'REF-001');
      expect(txn.type, 'TRANSFER_CREDIT');
      expect(txn.amount, '500.00');
      expect(txn.status, 'SUCCESSFUL');
      expect(txn.balanceBefore, '100.00');
      expect(txn.balanceAfter, '600.00');
      expect(txn.description, 'Money received');
      expect(txn.senderWalletId, 'wallet-2');
      expect(txn.receiverWalletId, 'wallet-1');
      expect(txn.createdAt, DateTime.parse('2026-07-10T09:00:00'));
      expect(txn.isIncoming, isTrue);
      expect(txn.isOutgoing, isFalse);
      expect(txn.displayTitle, 'Money received');
    });

    test('fromJson applies defaults for missing optional fields', () {
      final txn = WalletTransaction.fromJson(<String, dynamic>{
        'id': 'txn-1',
        'reference': 'REF-001',
        'type': 'WITHDRAWAL',
        'amount': '50.00',
        'status': 'PENDING',
      });

      expect(txn.id, 'txn-1');
      expect(txn.type, 'WITHDRAWAL');
      expect(txn.status, 'PENDING');
      expect(txn.balanceBefore, isNull);
      expect(txn.balanceAfter, isNull);
      expect(txn.description, isNull);
      expect(txn.senderWalletId, isNull);
      expect(txn.receiverWalletId, isNull);
      expect(txn.createdAt, isNull);
      expect(txn.isOutgoing, isTrue);
      expect(txn.displayTitle, 'Withdrawal');
    });
  });

  group('FeeEstimate', () {
    test('fromJson parses all fields', () {
      final fee = FeeEstimate.fromJson(<String, dynamic>{
        'transactionType': 'TRANSFER',
        'amount': '500.00',
        'percentageFee': '1.5',
        'flatFee': '2.00',
        'minFee': '1.00',
        'maxFee': '50.00',
        'totalFee': '9.50',
      });

      expect(fee.transactionType, 'TRANSFER');
      expect(fee.amount, '500.00');
      expect(fee.percentageFee, '1.5');
      expect(fee.flatFee, '2.00');
      expect(fee.minFee, '1.00');
      expect(fee.maxFee, '50.00');
      expect(fee.totalFee, '9.50');
    });

    test('fromJson applies defaults for missing fee fields', () {
      final fee = FeeEstimate.fromJson(<String, dynamic>{});

      expect(fee.transactionType, '');
      expect(fee.amount, '0');
      expect(fee.percentageFee, isNull);
      expect(fee.totalFee, '0');
    });
  });

  group('KycStatus', () {
    test('fromJson parses all fields including documents', () {
      final status = KycStatus.fromJson(<String, dynamic>{
        'kycStatus': 'APPROVED',
        'currentTier': 2,
        'tierName': 'Tier 2',
        'walletLimit': '50000',
        'dailySendLimit': '10000',
        'documents': <dynamic>[
          <String, dynamic>{
            'id': 'doc-1',
            'documentType': 'NATIONAL_ID',
            'fileName': 'nid.png',
            'status': 'APPROVED',
          },
        ],
        'submittedAt': '2026-07-01T09:00:00',
        'approvedAt': '2026-07-03T09:00:00',
        'rejectionReason': null,
      });

      expect(status.kycStatus, 'APPROVED');
      expect(status.currentTier, 2);
      expect(status.tierName, 'Tier 2');
      expect(status.walletLimit, '50000');
      expect(status.dailySendLimit, '10000');
      expect(status.documents, hasLength(1));
      expect(status.documents.first.id, 'doc-1');
      expect(status.documents.first.documentType, 'NATIONAL_ID');
      expect(status.documents.first.fileName, 'nid.png');
      expect(status.submittedAt, DateTime.parse('2026-07-01T09:00:00'));
      expect(status.approvedAt, DateTime.parse('2026-07-03T09:00:00'));
      expect(status.rejectionReason, isNull);
      expect(status.isApproved, isTrue);
    });

    test('fromJson defaults to NOT_SUBMITTED with empty documents', () {
      final status = KycStatus.fromJson(<String, dynamic>{});

      expect(status.kycStatus, 'NOT_SUBMITTED');
      expect(status.currentTier, 0);
      expect(status.isNotSubmitted, isTrue);
      expect(status.documents, isEmpty);
      expect(status.submittedAt, isNull);
      expect(status.approvedAt, isNull);
    });
  });

  group('UserProfile', () {
    test('fromJson parses all fields', () {
      final profile = UserProfile.fromJson(<String, dynamic>{
        'id': 'user-1',
        'firstName': 'Jane',
        'lastName': 'Doe',
        'email': 'jane@test.com',
        'phone': '+260976543210',
        'role': 'USER',
        'emailVerified': true,
        'pinSet': true,
      });

      expect(profile.id, 'user-1');
      expect(profile.firstName, 'Jane');
      expect(profile.role, 'USER');
      expect(profile.isAdmin, isFalse);
      expect(profile.fullName, 'Jane Doe');
      expect(profile.emailVerified, isTrue);
      expect(profile.pinSet, isTrue);
    });

    test('fromJson applies defaults for missing fields', () {
      final profile = UserProfile.fromJson(<String, dynamic>{'id': '1'});

      expect(profile.id, '1');
      expect(profile.role, 'USER');
      expect(profile.emailVerified, isFalse);
      expect(profile.pinSet, isFalse);
      expect(profile.fullName, '');
    });

    test('toUpdateJson only includes non-null fields', () {
      final profile = UserProfile.fromJson(<String, dynamic>{
        'id': 'user-1',
        'firstName': 'Jane',
        'email': 'jane@test.com',
        'role': 'ADMIN',
      });

      final update = profile.toUpdateJson();

      expect(update, <String, dynamic>{'firstName': 'Jane'});
    });
  });
}
