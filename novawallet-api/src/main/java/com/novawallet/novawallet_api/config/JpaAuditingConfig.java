package com.novawallet.novawallet_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Enables @CreatedDate / @LastModifiedDate on entities. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
