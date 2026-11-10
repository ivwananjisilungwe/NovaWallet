package com.novawallet.novawallet_api.transaction.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
class TransactionReferenceConcurrencyIntegrationTest {

    private static final Pattern REF_PATTERN = Pattern.compile("^NWTX\\d{8}\\d{8}$");

    @Autowired
    private TransactionReferenceGenerator generator;

    @Test
    void generatesDistinctReferencesConcurrently() throws Exception {
        int threads = 16;
        int perThread = 250;
        int total = threads * perThread;

        Set<String> refs = ConcurrentHashMap.newKeySet();
        AtomicInteger errors = new AtomicInteger();
        AtomicReference<Throwable> firstError = new AtomicReference<>();

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        String ref = generator.generate();
                        if (!REF_PATTERN.matcher(ref).matches()) {
                            throw new IllegalStateException("Malformed reference: " + ref);
                        }
                        if (!refs.add(ref)) {
                            throw new IllegalStateException("Duplicate reference: " + ref);
                        }
                    }
                } catch (Throwable ex) {
                    errors.incrementAndGet();
                    firstError.compareAndSet(null, ex);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        boolean finished = done.await(120, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(finished).isTrue();
        assertThat(errors.get()).isZero();
        if (firstError.get() != null) {
            throw new AssertionError(firstError.get());
        }
        assertThat(refs).hasSize(total);
        for (String ref : refs) {
            assertThat(ref).matches(REF_PATTERN);
        }
    }
}
