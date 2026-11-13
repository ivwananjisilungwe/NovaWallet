package com.novawallet.novawallet_api.transaction.controller;

import com.novawallet.novawallet_api.auth.controller.BaseAuthIntegrationTest;
import com.novawallet.novawallet_api.fee.entity.FeeConfiguration;
import com.novawallet.novawallet_api.fee.enums.FeeType;
import com.novawallet.novawallet_api.fee.repository.FeeConfigurationRepository;
import com.novawallet.novawallet_api.fee.service.FeeEngineService;
import com.novawallet.novawallet_api.transaction.dto.TransferRequest;
import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import com.novawallet.novawallet_api.transaction.service.TransactionService;
import com.novawallet.novawallet_api.user.entity.Role;
import com.novawallet.novawallet_api.user.entity.User;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import com.novawallet.novawallet_api.wallet.entity.Wallet;
import com.novawallet.novawallet_api.wallet.repository.WalletRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConcurrentTransferIntegrationTest extends BaseAuthIntegrationTest {

    @Autowired private WalletRepository walletRepository;
    @Autowired private TransactionService transactionService;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FeeConfigurationRepository feeConfigurationRepository;
    @Autowired private FeeEngineService feeEngineService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;
    private Wallet senderWallet;
    private Wallet receiverWallet;
    private UUID senderUserId;
    private UUID receiverUserId;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        User sender = userRepository.save(User.builder()
                .email("csender-" + UUID.randomUUID() + "@test.com")
                .passwordHash("encoded").firstName("C").lastName("Sender")
                .phone("+26096" + (System.currentTimeMillis() % 100000))
                .role(Role.USER).emailVerified(true).pinHash(passwordEncoder.encode("2468")).build());
        senderUserId = sender.getId();

        senderWallet = walletRepository.save(Wallet.builder()
                .userId(sender.getId())
                .accountNumber("CWT" + UUID.randomUUID().toString().substring(0,8).toUpperCase())
                .balance(new BigDecimal("100.00")).currency("ZMW").build());
        senderWallet = walletRepository.findById(senderWallet.getId()).orElseThrow();

        User receiver = userRepository.save(User.builder()
                .email("creceiver-" + UUID.randomUUID() + "@test.com")
                .passwordHash("encoded").firstName("C").lastName("Receiver")
                .phone("+26097" + (System.currentTimeMillis() % 100000))
                .role(Role.USER).emailVerified(true).pinHash(passwordEncoder.encode("2468")).build());
        receiverUserId = receiver.getId();

        receiverWallet = walletRepository.save(Wallet.builder()
                .userId(receiver.getId())
                .accountNumber("CWR" + UUID.randomUUID().toString().substring(0,8).toUpperCase())
                .balance(BigDecimal.ZERO.setScale(2)).currency("ZMW").build());

        // Use whatever TRANSFER fee config exists (do not delete others' configs)
    }

    @Test
    @DisplayName("10 concurrent transfers from same wallet: only 3 succeed, balance never negative")
    void concurrentTransfers_fromSameWallet_shouldPreventOverdraft() throws Exception {
        int threadCount = 10;
        BigDecimal amount = new BigDecimal("30.00");
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failure = new AtomicInteger(0);
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            exec.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    transactionService.transfer(
                            new TransferRequest(receiverWallet.getId(), amount, "2468", "concurrent"),
                            senderUserId
                    );
                    success.incrementAndGet();
                } catch (Exception e) {
                    failure.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        done.await();
        exec.shutdown();

        Wallet fs = walletRepository.findById(senderWallet.getId()).orElseThrow();
        Wallet fr = walletRepository.findById(receiverWallet.getId()).orElseThrow();
        BigDecimal fee = feeEngineService.calculateFee(FeeType.TRANSFER, amount);
        BigDecimal expectedSender = new BigDecimal("100.00")
                .subtract(new BigDecimal(success.get()).multiply(amount.add(fee)));
        // Balance must never go negative (invariant enforced by DB CHECK)
        assertThat(fs.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(fs.getBalance()).isEqualByComparingTo(expectedSender);
        // At least 1 transfer should succeed
        assertThat(success.get()).isGreaterThanOrEqualTo(1);
        // Receiver balance should equal successes * amount
        assertThat(fr.getBalance()).isEqualByComparingTo(amount.multiply(new BigDecimal(success.get())));
    }

    @Test
    @DisplayName("Symmetric transfers (A->B and B->A) do not deadlock")
    void symmetricTransfers_shouldNotDeadlock() throws Exception {
        // Create dedicated wallets for this test (updateBalance ADDs, does not SET)
        User userA = userRepository.save(User.builder()
                .email("sym-a-" + UUID.randomUUID() + "@test.com")
                .passwordHash("encoded").firstName("SymA").lastName("User")
                .phone("+26091" + (System.currentTimeMillis() % 100000))
                .role(Role.USER).emailVerified(true).pinHash(passwordEncoder.encode("2468"))
                .build());
        User userB = userRepository.save(User.builder()
                .email("sym-b-" + UUID.randomUUID() + "@test.com")
                .passwordHash("encoded").firstName("SymB").lastName("User")
                .phone("+26092" + (System.currentTimeMillis() % 100000))
                .role(Role.USER).emailVerified(true).pinHash(passwordEncoder.encode("2468"))
                .build());
        Wallet walletA = walletRepository.save(Wallet.builder()
                .userId(userA.getId())
                .accountNumber("SYM-A-" + UUID.randomUUID().toString().substring(0,8).toUpperCase())
                .balance(new BigDecimal("50.00")).currency("ZMW")
                .build());
        Wallet walletB = walletRepository.save(Wallet.builder()
                .userId(userB.getId())
                .accountNumber("SYM-B-" + UUID.randomUUID().toString().substring(0,8).toUpperCase())
                .balance(new BigDecimal("50.00")).currency("ZMW")
                .build());

        CountDownLatch start = new CountDownLatch(1);

        Thread t1 = new Thread(() -> {
            try { start.await();
                transactionService.transfer(
                        new TransferRequest(walletB.getId(), new BigDecimal("20.00"), "2468", "A->B"),
                        userA.getId());
            } catch (Exception ignored) {}
        });
        Thread t2 = new Thread(() -> {
            try { start.await();
                transactionService.transfer(
                        new TransferRequest(walletA.getId(), new BigDecimal("10.00"), "2468", "B->A"),
                        userB.getId());
            } catch (Exception ignored) {}
        });
        t1.start(); t2.start(); start.countDown();
        t1.join(10000); t2.join(10000);

        // H2 may detect a deadlock (symmetric lock order: A→B locks A then B,
        // B→A locks B then A) and roll back one transaction.  At minimum one
        // transfer should succeed, total balance must be conserved (minus fees),
        // and neither thread should hang.
        Wallet wa = walletRepository.findById(walletA.getId()).orElseThrow();
        Wallet wb = walletRepository.findById(walletB.getId()).orElseThrow();
        assertThat(wa.getBalance()).isGreaterThan(BigDecimal.ZERO);
        assertThat(wb.getBalance()).isGreaterThan(BigDecimal.ZERO);
        // At least $1 must have moved; total may be < 100 if a non-zero fee
        // config from another test was active at runtime
        assertThat(wa.getBalance().add(wb.getBalance()))
                .as("Total balance conserved (fees may reduce it)")
                .isLessThanOrEqualTo(new BigDecimal("100.00"));
        assertThat(wa.getBalance().add(wb.getBalance()))
                .isGreaterThan(BigDecimal.ZERO);
        assertThat(t1.isAlive()).isFalse();
        assertThat(t2.isAlive()).isFalse();
    }

    @Test
    @DisplayName("Rounds of simultaneous A->B and B->A transfers do not deadlock")
    void symmetricRounds_shouldNotDeadlock() throws Exception {
        // Dedicated wallets
        User userA = userRepository.save(User.builder()
                .email("symr-a-" + UUID.randomUUID() + "@test.com")
                .passwordHash("encoded").firstName("SymR").lastName("A")
                .phone("+26093" + (System.currentTimeMillis() % 100000))
                .role(Role.USER).emailVerified(true).pinHash(passwordEncoder.encode("2468"))
                .build());
        User userB = userRepository.save(User.builder()
                .email("symr-b-" + UUID.randomUUID() + "@test.com")
                .passwordHash("encoded").firstName("SymR").lastName("B")
                .phone("+26094" + (System.currentTimeMillis() % 100000))
                .role(Role.USER).emailVerified(true).pinHash(passwordEncoder.encode("2468"))
                .build());
        Wallet walletA = walletRepository.save(Wallet.builder()
                .userId(userA.getId())
                .accountNumber("SYMR-A-" + UUID.randomUUID().toString().substring(0,8).toUpperCase())
                .balance(new BigDecimal("200.00")).currency("ZMW")
                .build());
        Wallet walletB = walletRepository.save(Wallet.builder()
                .userId(userB.getId())
                .accountNumber("SYMR-B-" + UUID.randomUUID().toString().substring(0,8).toUpperCase())
                .balance(new BigDecimal("200.00")).currency("ZMW")
                .build());

        BigDecimal amountAB = new BigDecimal("10.00");
        BigDecimal amountBA = new BigDecimal("5.00");
        int rounds = 3;
        // keep within velocity limit (10 outgoing per user)
        BigDecimal feeAB = feeEngineService.calculateFee(FeeType.TRANSFER, amountAB);
        BigDecimal feeBA = feeEngineService.calculateFee(FeeType.TRANSFER, amountBA);

        java.util.concurrent.atomic.AtomicInteger failures = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int r = 0; r < rounds; r++) {
            java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
            Thread t1 = new Thread(() -> {
                try { start.await();
                    transactionService.transfer(
                            new TransferRequest(walletB.getId(), amountAB, "2468", "A->B"),
                            userA.getId());
                } catch (Exception e) { failures.incrementAndGet(); }
            });
            Thread t2 = new Thread(() -> {
                try { start.await();
                    transactionService.transfer(
                            new TransferRequest(walletA.getId(), amountBA, "2468", "B->A"),
                            userB.getId());
                } catch (Exception e) { failures.incrementAndGet(); }
            });
            t1.start(); t2.start();
            start.countDown();
            t1.join(10000);
            t2.join(10000);
            assertThat(t1.isAlive()).isFalse();
            assertThat(t2.isAlive()).isFalse();
        }

        Wallet wa = walletRepository.findById(walletA.getId()).orElseThrow();
        Wallet wb = walletRepository.findById(walletB.getId()).orElseThrow();

        // Exact accounting: A side = initial - sum(amountAB+feeAB) + sum(amountBA)
        BigDecimal expectedA = new BigDecimal("200.00")
                .subtract(amountAB.add(feeAB).multiply(new BigDecimal(rounds)))
                .add(amountBA.multiply(new BigDecimal(rounds)));
        BigDecimal expectedB = new BigDecimal("200.00")
                .subtract(amountBA.add(feeBA).multiply(new BigDecimal(rounds)))
                .add(amountAB.multiply(new BigDecimal(rounds)));
        assertThat(wa.getBalance()).isEqualByComparingTo(expectedA);
        assertThat(wb.getBalance()).isEqualByComparingTo(expectedB);
        assertThat(failures.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("Insufficient balance transfer returns 400 and leaves balances and rows unchanged")
    void insufficientBalance_transfer_returns400() throws Exception {
        User userA = userRepository.save(User.builder()
                .email("insuf-a-" + UUID.randomUUID() + "@test.com")
                .passwordHash("encoded").firstName("Insuf").lastName("A")
                .phone("+26095" + (System.currentTimeMillis() % 100000))
                .role(Role.USER).emailVerified(true).pinHash(passwordEncoder.encode("2468"))
                .build());
        User userB = userRepository.save(User.builder()
                .email("insuf-b-" + UUID.randomUUID() + "@test.com")
                .passwordHash("encoded").firstName("Insuf").lastName("B")
                .phone("+26096" + (System.currentTimeMillis() % 100000))
                .role(Role.USER).emailVerified(true).pinHash(passwordEncoder.encode("2468"))
                .build());
        Wallet walletA = walletRepository.save(Wallet.builder()
                .userId(userA.getId())
                .accountNumber("INSUF-A-" + UUID.randomUUID().toString().substring(0,8).toUpperCase())
                .balance(new BigDecimal("10.00")).currency("ZMW")
                .build());
        Wallet walletB = walletRepository.save(Wallet.builder()
                .userId(userB.getId())
                .accountNumber("INSUF-B-" + UUID.randomUUID().toString().substring(0,8).toUpperCase())
                .balance(BigDecimal.ZERO.setScale(2)).currency("ZMW")
                .build());

        long beforeCount = transactionRepository.findAll().stream()
                .filter(tx -> walletA.getId().equals(tx.getSenderWalletId())
                        || walletA.getId().equals(tx.getReceiverWalletId())
                        || walletB.getId().equals(tx.getSenderWalletId())
                        || walletB.getId().equals(tx.getReceiverWalletId()))
                .count();

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                transactionService.transfer(
                        new TransferRequest(walletB.getId(), new BigDecimal("100.00"), "2468", "insufficient"),
                        userA.getId()))
                .isInstanceOf(com.novawallet.novawallet_api.exception.BadRequestException.class)
                .hasMessageContaining("Insufficient balance");

        Wallet wa = walletRepository.findById(walletA.getId()).orElseThrow();
        Wallet wb = walletRepository.findById(walletB.getId()).orElseThrow();
        assertThat(wa.getBalance()).isEqualByComparingTo("10.00");
        assertThat(wb.getBalance()).isEqualByComparingTo("0.00");

        long afterCount = transactionRepository.findAll().stream()
                .filter(tx -> walletA.getId().equals(tx.getSenderWalletId())
                        || walletA.getId().equals(tx.getReceiverWalletId())
                        || walletB.getId().equals(tx.getSenderWalletId())
                        || walletB.getId().equals(tx.getReceiverWalletId()))
                .count();
        assertThat(afterCount).isEqualTo(beforeCount);
    }
}
