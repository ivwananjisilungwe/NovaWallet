package com.novawallet.novawallet_api.token.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V16 (Phase 1C Batch 1): the {@code revoked_access_tokens} table must be
 * created on a fresh database and must be addable on top of a pre-V16 schema.
 */
class V16RevokedAccessTokensMigrationTest {

    private static String newDbUrl() {
        return "jdbc:h2:mem:revoked-" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
    }

    private static Flyway configure(String url, String target) {
        org.flywaydb.core.api.configuration.FluentConfiguration fluent = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration");
        if (target != null) {
            fluent.target(target);
        }
        return fluent.load();
    }

    @Test
    void freshMigrationCreatesRevokedAccessTokensTable() throws Exception {
        String url = newDbUrl();
        configure(url, null).migrate();

        try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
            // Table exists and is empty.
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM revoked_access_tokens")) {
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isZero();
                }
            }

            // Primary key constraint exists.
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                            + "WHERE TABLE_NAME = 'REVOKED_ACCESS_TOKENS' "
                            + "AND CONSTRAINT_TYPE = 'PRIMARY KEY'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(1);
                }
            }

            // Foreign key to users(id) exists.
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS "
                            + "WHERE UNIQUE_CONSTRAINT_NAME = 'PK_REVOKED_ACCESS_TOKENS' "
                            + "   OR CONSTRAINT_NAME LIKE 'FK_REVOKED_ACCESS_TOKENS_USER%'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(1);
                }
            }

            // Indexes exist.
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES "
                            + "WHERE TABLE_NAME = 'REVOKED_ACCESS_TOKENS'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    // PK index + 2 secondary indexes (plus H2's own)
                    assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(2);
                }
            }
        }
    }

    @Test
    void upgradeFromV15AddsTheTable() throws Exception {
        String url = newDbUrl();

        // Migrate up to V15 first.
        configure(url, "15").migrate();

        // Table must not exist yet.
        try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
            boolean exists;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                            + "WHERE TABLE_NAME = 'REVOKED_ACCESS_TOKENS'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    exists = rs.getInt(1) > 0;
                }
            }
            assertThat(exists).isFalse();
        }

        // Migrate to latest (runs V16).
        configure(url, null).migrate();

        try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM revoked_access_tokens")) {
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isZero();
                }
            }
        }
    }

    @Test
    void duplicateJtiIsRejectedByPrimaryKey() throws Exception {
        String url = newDbUrl();
        configure(url, null).migrate();

        UUID jti = UUID.randomUUID();
        UUID userId;
        try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
            // Insert a user to satisfy the FK.
            userId = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (id, email, password_hash, first_name, last_name, phone, role, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")) {
                ps.setObject(1, userId);
                ps.setString(2, "mig-" + userId + "@test.com");
                ps.setString(3, "encoded");
                ps.setString(4, "Mig");
                ps.setString(5, "User");
                ps.setString(6, "+260971234567");
                ps.setString(7, "USER");
                ps.executeUpdate();
            }

            // First revocation row inserts.
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO revoked_access_tokens (jti, user_id, revoked_at, expires_at) "
                            + "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")) {
                ps.setObject(1, jti);
                ps.setObject(2, userId);
                ps.executeUpdate();
            }

            // Second insert with the same jti violates the PK.
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO revoked_access_tokens (jti, user_id, revoked_at, expires_at) "
                            + "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")) {
                ps.setObject(1, jti);
                ps.setObject(2, userId);
                try {
                    ps.executeUpdate();
                    org.junit.jupiter.api.Assertions.fail("Expected primary-key violation");
                } catch (java.sql.SQLException e) {
                    // Expected: duplicate key.
                    assertThat(e.getMessage().toLowerCase()).containsAnyOf("unique", "primary", "duplicate");
                }
            }
        }
    }
}
