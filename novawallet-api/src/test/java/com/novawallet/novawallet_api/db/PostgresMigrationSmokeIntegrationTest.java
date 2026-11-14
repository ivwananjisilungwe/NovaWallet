package com.novawallet.novawallet_api.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * PostgreSQL 16 smoke/integrity checks for the Flyway-managed schema.
 *
 * <p>Runs only under the {@code postgres} tag / {@code pgtest} Maven profile
 * against a real PostgreSQL database injected via env vars or properties
 * (see {@code src/test/resources/application-pgtest.yml}): e.g.
 * {@code PGTEST_JDBC_URL}/{@code PGTEST_HOST,PORT,DB,USER,PASSWORD} or
 * Spring {@code SPRING_DATASOURCE_URL/USERNAME/PASSWORD}.
 *
 * <p>Intended for disposable CI/script databases (fresh V1→head). Default H2
 * suite remains unchanged and excludes this tag.</p>
 */
@Tag("postgres")
@SpringBootTest
@ActiveProfiles("pgtest")
class PostgresMigrationSmokeIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationsAppliedToHeadAndSchemaIsValid() {
        // V12 is a Java migration; head is V16 (SQL).
        MigrationInfo current = flyway.info().current();
        assertThat(current).as("current Flyway version").isNotNull();
        assertThat(current.getVersion().getVersion()).isEqualTo("16");
        assertThat(current.getState()).isEqualTo(MigrationState.SUCCESS);

        assertThatCode(() -> flyway.validate())
                .as("flyway validate against migrated PostgreSQL schema")
                .doesNotThrowAnyException();
    }

    @Test
    void v12TransactionReferenceSequenceExistsAndIsUsable() {
        Integer regclass = jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.transaction_reference_seq') IS NOT NULL::int",
                Integer.class);
        assertThat(regclass).as("transaction_reference_seq exists").isEqualTo(1);

        Long next = jdbcTemplate.queryForObject(
                "SELECT nextval('transaction_reference_seq')", Long.class);
        assertThat(next).as("nextval(transaction_reference_seq)").isNotNull().isPositive();
    }

    @Test
    void v16RevokedAccessTokensTableHasExpectedIntegrityObjects() {
        assertThat(tableExists("revoked_access_tokens"))
                .as("revoked_access_tokens table exists")
                .isTrue();

        // Primary key on jti.
        Integer pkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                        "WHERE table_schema = 'public' " +
                        "AND table_name = 'revoked_access_tokens' " +
                        "AND constraint_type = 'PRIMARY KEY'",
                Integer.class);
        assertThat(pkCount).as("revoked_access_tokens primary key count").isEqualTo(1);

        // Foreign key to users(id).
        Integer fkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints tc " +
                        "JOIN information_schema.key_column_usage kcu " +
                        "  ON tc.constraint_name = kcu.constraint_name " +
                        " AND tc.table_schema = kcu.table_schema " +
                        "WHERE tc.table_schema = 'public' " +
                        "AND tc.table_name = 'revoked_access_tokens' " +
                        "AND tc.constraint_type = 'FOREIGN KEY' " +
                        "AND kcu.column_name = 'user_id'",
                Integer.class);
        assertThat(fkCount).as("revoked_access_tokens user_id foreign key count").isGreaterThanOrEqualTo(1);

        // Required secondary indexes (expires_at for cleanup, user_id for forensics).
        assertThat(indexExists("idx_revoked_access_tokens_expires_at"))
                .as("idx_revoked_access_tokens_expires_at exists")
                .isTrue();
        assertThat(indexExists("idx_revoked_access_tokens_user_id"))
                .as("idx_revoked_access_tokens_user_id exists")
                .isTrue();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = 'public' AND table_name = ?",
                Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes " +
                        "WHERE schemaname = 'public' AND indexname = ?",
                Integer.class, indexName);
        return count != null && count > 0;
    }
}
