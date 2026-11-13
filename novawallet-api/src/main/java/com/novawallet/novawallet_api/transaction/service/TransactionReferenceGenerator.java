package com.novawallet.novawallet_api.transaction.service;

import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates human-readable transaction references in the format
 * {@code NWTXyyyyMMdd00000001}.
 *
 * <p>The sequence component is allocated by the database-backed
 * {@code transaction_reference_seq} (see migration V12). This avoids the
 * previous JVM-local {@code AtomicLong}, which could collide across
 * application instances or test contexts sharing the same database. The
 * sequence is global and monotonically increasing; the date prefix changes
 * daily, while the database unique constraint on {@code transactions.reference}
 * remains the final guard against duplicates.</p>
 */
@Component
public class TransactionReferenceGenerator {

    private static final Logger log = LoggerFactory.getLogger(TransactionReferenceGenerator.class);
    private static final String PREFIX = "NWTX";
    private static final int SEQ_WIDTH = 8;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TransactionRepository transactionRepository;

    public TransactionReferenceGenerator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Generates a unique transaction reference.
     *
     * <p>The numeric suffix is allocated from the database-backed
     * {@code transaction_reference_seq}. The sequence is global and monotonic;
     * its lifetime capacity is 99,999,999 references. Once exhausted, generation
     * fails rather than emitting a malformed reference.</p>
     *
     * @throws IllegalStateException if the database sequence cannot be allocated
     *         or its value is outside the supported range [1, 99999999]
     */
    public String generate() {
        Long sequence = transactionRepository.nextReferenceSequence();
        if (sequence == null) {
            throw new IllegalStateException("Database did not return a transaction reference sequence value");
        }
        if (sequence < 1L || sequence > 99_999_999L) {
            throw new IllegalStateException("Transaction reference sequence value out of range: " + sequence);
        }
        String today = LocalDate.now().format(DATE_FORMATTER);
        String reference = PREFIX + today + String.format("%0" + SEQ_WIDTH + "d", sequence);
        log.debug("Generated transaction reference {}", reference);
        return reference;
    }
}
