package com.novawallet.novawallet_api.wallet.service;

import com.novawallet.novawallet_api.wallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class AccountNumberGenerator {

    private static final Logger log = LoggerFactory.getLogger(AccountNumberGenerator.class);
    private static final String PREFIX = "NW";
    private static final int DIGITS = 10;
    private static final int MAX_ATTEMPTS = 10;

    private final WalletRepository walletRepository;

    public AccountNumberGenerator(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public String generate() {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String accountNumber = PREFIX + generateRandomDigits();
            if (!walletRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }
            log.warn("Account number collision: {}", accountNumber);
        }
        throw new IllegalStateException("Failed to generate unique account number after " + MAX_ATTEMPTS + " attempts");
    }

    private String generateRandomDigits() {
        StringBuilder sb = new StringBuilder(DIGITS);
        for (int i = 0; i < DIGITS; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }
}
