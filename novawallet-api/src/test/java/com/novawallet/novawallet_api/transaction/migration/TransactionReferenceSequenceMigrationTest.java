package com.novawallet.novawallet_api.transaction.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionReferenceSequenceMigrationTest {

    private static String newDbUrl() {
        return "jdbc:h2:mem:refseq-" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
    }

    @Test
    void seedsSequenceAboveHistoricalMaxSuffix() throws Exception {
        String url = newDbUrl();
        String user = "sa";
        String password = "";

        // Migrate up to V11 first.
        Flyway flywayToV11 = Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .target("11")
                .load();
        flywayToV11.migrate();

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        Set<String> historicalRefs = new HashSet<>();
        historicalRefs.add("NWTX" + today + "00000018");
        historicalRefs.add("NWTX" + yesterday + "00000042");
        historicalRefs.add("NWTX" + today + "00000007");
        historicalRefs.add("NWTXLEGACY-1");

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(true);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO transactions (id, reference, type, amount, status, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, 'SUCCESSFUL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")) {
                int i = 0;
                for (String ref : historicalRefs) {
                    ps.setObject(1, UUID.randomUUID());
                    ps.setString(2, ref);
                    ps.setString(3, "DEPOSIT");
                    ps.setBigDecimal(4, new BigDecimal("10.00"));
                    ps.addBatch();
                    i++;
                }
                ps.executeBatch();
            }
        }

        // Migrate to latest (runs V12).
        Flyway flywayLatest = Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .load();
        flywayLatest.migrate();

        // First nextval returns 43 (max suffix 42 + 1).
        long first = nextval(url, user, password);
        assertThat(first).isEqualTo(43L);

        // Historical rows unchanged.
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM transactions")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(4);
            }
            Set<String> stored = new HashSet<>();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT reference FROM transactions")) {
                while (rs.next()) {
                    stored.add(rs.getString(1));
                }
            }
            assertThat(stored).containsExactlyInAnyOrderElementsOf(historicalRefs);
        }

        // 50 subsequent nextval values never equal any historical reference.
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            long v = nextval(url, user, password);
            seen.add(v);
            String candidate = "NWTX" + today + String.format("%08d", v);
            assertThat(historicalRefs).doesNotContain(candidate);
        }
        assertThat(seen).hasSize(50);
    }

    @Test
    void emptyDatabaseFirstNextvalIsOne() throws Exception {
        String url = newDbUrl();
        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        long first = nextval(url, "sa", "");
        assertThat(first).isEqualTo(1L);
    }

    @Test
    void scaleCorrectnessAcrossStreamingChunks() throws Exception {
        String url = newDbUrl();
        String user = "sa";
        String password = "";

        // Migrate up to V11 first.
        Flyway flywayToV11 = Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .target("11")
                .load();
        flywayToV11.migrate();

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        int validCount = 20000;
        int ignoredCount = 20000;

        long start = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO transactions (id, reference, type, amount, status, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, 'SUCCESSFUL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")) {
                int batch = 0;
                for (int i = 1; i <= validCount; i++) {
                    ps.setObject(1, UUID.randomUUID());
                    ps.setString(2, "NWTX" + today + String.format("%08d", i));
                    ps.setString(3, "DEPOSIT");
                    ps.setBigDecimal(4, new BigDecimal("10.00"));
                    ps.addBatch();
                    if (++batch % 1000 == 0) {
                        ps.executeBatch();
                    }
                }
                for (int i = 1; i <= ignoredCount; i++) {
                    ps.setObject(1, UUID.randomUUID());
                    ps.setString(2, "IGNORED-" + i);
                    ps.setString(3, "DEPOSIT");
                    ps.setBigDecimal(4, new BigDecimal("10.00"));
                    ps.addBatch();
                    if (++batch % 1000 == 0) {
                        ps.executeBatch();
                    }
                }
                ps.executeBatch();
            }
            conn.commit();
        }

        // Migrate to latest (runs V12).
        Flyway flywayLatest = Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .load();
        flywayLatest.migrate();

        long first = nextval(url, user, password);
        System.out.println("SCALE_RUNTIME_MS=" + (System.currentTimeMillis() - start));
        assertThat(first).isEqualTo((long) validCount + 1);

        // Historical rows unchanged.
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM transactions")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(validCount + ignoredCount);
            }
        }
    }

    private static long nextval(String url, String user, String password) throws Exception {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT nextval('transaction_reference_seq')")) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
