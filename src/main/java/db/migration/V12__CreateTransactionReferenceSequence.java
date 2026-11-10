package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

/**
 * Creates the database-backed sequence used to allocate transaction reference
 * suffixes (FIN-12).
 *
 * <p>The sequence is seeded above the maximum suffix found among historical
 * references matching {@code ^NWTX[0-9]{8}[0-9]{8}$}. Historical rows are
 * never rewritten.</p>
 */
public class V12__CreateTransactionReferenceSequence extends BaseJavaMigration {

    private static final Logger log = LoggerFactory.getLogger(V12__CreateTransactionReferenceSequence.class);

    private static final Pattern REFERENCE_PATTERN = Pattern.compile("^NWTX[0-9]{8}[0-9]{8}$");
    private static final long MAX_CAPACITY = 99_999_999L;
    private static final int FETCH_SIZE = 1000;

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        // Fail closed when the JDBC connection cannot stream safely. PostgreSQL
        // (and Flyway here) runs migrations with auto-commit disabled; if it is
        // on, server-side cursor streaming is not available and the historical
        // scan would buffer the whole table client-side.
        if (connection.getAutoCommit()) {
            throw new IllegalStateException(
                    "Cannot stream transaction references safely: connection auto-commit is enabled");
        }

        // 1. Stream references and compute the maximum historical suffix.
        //    The fetch size is set and verified before any SELECT is executed.
        long maxSuffix = 0L;
        int ignoredRows = 0;
        try (Statement stmt = connection.createStatement()) {
            try {
                stmt.setFetchSize(FETCH_SIZE);
            } catch (SQLException e) {
                throw new IllegalStateException("JDBC driver does not support setting a fetch size for streaming", e);
            }
            int actualFetchSize = stmt.getFetchSize();
            if (actualFetchSize <= 0) {
                throw new IllegalStateException(
                        "JDBC driver did not accept a nonzero fetch size for streaming (fetchSize="
                                + actualFetchSize + ")");
            }

            try (ResultSet rs = stmt.executeQuery("SELECT reference FROM transactions")) {
                while (rs.next()) {
                    String reference = rs.getString(1);
                    if (reference != null && REFERENCE_PATTERN.matcher(reference).matches()) {
                        long suffix = Long.parseLong(reference.substring(12));
                        if (suffix > maxSuffix) {
                            maxSuffix = suffix;
                        }
                    } else {
                        ignoredRows++;
                    }
                }
            }
        }

        if (ignoredRows > 0) {
            log.warn("Ignored {} transaction rows whose reference does not match ^NWTX[0-9]{8}[0-9]{8}$ while seeding transaction_reference_seq", ignoredRows);
        }

        if (maxSuffix >= MAX_CAPACITY) {
            throw new IllegalStateException("No remaining capacity for transaction reference sequence (maxSuffix=" + maxSuffix + ")");
        }

        // 2. Create the sequence (fail closed if it already exists).
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE SEQUENCE transaction_reference_seq START WITH 1 INCREMENT BY 1");
        }

        // 3. Seed the sequence above the historical maximum suffix.
        long nextValue = maxSuffix + 1;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER SEQUENCE transaction_reference_seq RESTART WITH " + nextValue);
        }

        log.info("Seeded transaction_reference_seq to start at {}", nextValue);
    }
}