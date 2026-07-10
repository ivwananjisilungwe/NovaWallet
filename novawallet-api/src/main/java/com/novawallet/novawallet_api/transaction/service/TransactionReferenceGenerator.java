package com.novawallet.novawallet_api.transaction.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TransactionReferenceGenerator {

    private static final Logger log = LoggerFactory.getLogger(TransactionReferenceGenerator.class);
    private static final String PREFIX = "NWTX";
    private static final int SEQ_WIDTH = 8;

    private final AtomicLong counter = new AtomicLong(0);
    private volatile String currentDate;

    /**
     * Generates a unique transaction reference in the format
     * {@code NWTX{yyyyMMdd}{00000001}}.
     * <p>
     * The sequence resets daily (different date prefix). Atomic under
     * concurrent access via {@link AtomicLong} and the double-checked
     * locking pattern for date changes.
     */
    public String generate() {
        String today = formattedDate();
        if (!today.equals(currentDate)) {
            synchronized (this) {
                if (!today.equals(currentDate)) {
                    counter.set(0);
                    currentDate = today;
                    log.debug("Transaction reference date changed to {}", today);
                }
            }
        }
        long seq = counter.incrementAndGet();
        return PREFIX + today + String.format("%0" + SEQ_WIDTH + "d", seq);
    }

    private String formattedDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}
