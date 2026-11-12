package com.novawallet.novawallet_api.notification.schedule;

import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Verifies that {@link BalanceRecalculationJob} is contained in production.
 * <p>
 * With the {@code prod} profile active, {@code app.scheduler.balance-recalc.enabled}
 * defaults to {@code false} (see {@code application-prod.yml}), so the
 * {@code @ConditionalOnProperty(matchIfMissing = false)} guard on the job class
 * prevents the bean from being registered and its scheduled method from ever
 * running. Consequently {@link TransactionRepository#recalculateMissingBalances()}
 * is never invoked while the job is disabled.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        // Test-only logback config: console-only so tests run without
        // /var/log/novawallet/api.log (production keeps its own logback-spring.xml).
        "logging.config=classpath:logback-test.xml",
        // Embedded H2 so the context boots without a real PostgreSQL database.
        "spring.datasource.url=jdbc:h2:mem:balancerecalcjobdisabledtest",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        // Skip Flyway (migrations target PostgreSQL) and schema validation.
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        // Dummy values for production props that have no default.
        // Valid Base64 JWT secret (same value used by application-test.yml).
        "jwt.secret=dGVzdC1vbmx5LWp3dC1zZWNyZXQta2V5LWZvci1ub3Zhd2FsbGV0LWF1dG9tYXRlZC10ZXN0cy1ub3QtZm9yLXByb2R1Y3Rpb24=",
        "app.admin.password=test-admin-password-do-not-use-in-production",
        "app.kyc.upload-dir=target/test-uploads/kyc",
        "spring.mail.host=localhost",
        "spring.mail.port=25"
})
class BalanceRecalculationJobDisabledInProdTest {

    @Autowired
    private ApplicationContext applicationContext;

    @SpyBean
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("BalanceRecalculationJob bean is not registered in the prod profile")
    void balanceRecalculationJob_shouldNotBeRegisteredInProd() {
        assertThat(applicationContext.containsBean("balanceRecalculationJob"))
                .as("BalanceRecalculationJob must not be a bean when disabled in prod")
                .isFalse();
        assertThatThrownBy(() -> applicationContext.getBean(BalanceRecalculationJob.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    @DisplayName("recalculateMissingBalances is never called while the job is disabled")
    void recalculateMissingBalances_shouldNeverBeCalledWhenJobDisabled() {
        verify(transactionRepository, never()).recalculateMissingBalances();
    }
}
