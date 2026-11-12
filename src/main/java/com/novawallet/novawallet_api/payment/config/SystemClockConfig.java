package com.novawallet.novawallet_api.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Supplies the application {@link Clock} used by time-driven payment components
 * (currently {@code PayoutReconciliationService}).
 *
 * <p>Indirection exists so reconciliation timing — claim leases, the backoff
 * ladder (1m, 2m, 5m, 10m, 30m, hourly) and the 24h parking horizon — can be
 * driven deterministically in tests with a fixed/mutable clock. Production uses
 * the system default zone (Africa/Lusaka, CAT).</p>
 */
@Configuration
public class SystemClockConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
