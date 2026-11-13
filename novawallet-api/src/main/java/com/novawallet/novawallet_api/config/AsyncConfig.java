package com.novawallet.novawallet_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Thread pool config for @Async audit/notification delivery. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
