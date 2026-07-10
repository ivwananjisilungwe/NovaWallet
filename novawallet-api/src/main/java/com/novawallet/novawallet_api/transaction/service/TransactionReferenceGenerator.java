package com.novawallet.novawallet_api.transaction.service;

import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TransactionReferenceGenerator {

    private static final Logger log = LoggerFactory.getLogger(TransactionReferenceGenerator.class);
    private static final String PREFIX = "NWTX";
    private static final int SEQ_WIDTH = 8;

    private final AtomicLong counter = new AtomicLong(0);
    private volatile String currentDate;

    private final TransactionRepository transactionRepository;

    public TransactionReferenceGenerator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @PostConstruct
    public void initCounterFromDatabase() {
        String today = formattedDate();
        currentDate = today;
        String prefix = PREFIX + today;
        Optional<String> maxRef = transactionRepository.findMaxReferenceByPrefix(prefix + "%");
        if (maxRef.isPresent()) {
            String ref = maxRef.get();
            long seq = Long.parseLong(ref.substring(prefix.length()));
            counter.set(seq);
            log.info("Seeded reference counter to {} from existing reference '{}'", seq, ref);
        } else {
            counter.set(0);
            log.info("No existing references for today ({}), counter starts at 0", today);
        }
    }

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
