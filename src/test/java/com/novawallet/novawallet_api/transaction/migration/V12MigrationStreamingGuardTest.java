package com.novawallet.novawallet_api.transaction.migration;

import db.migration.V12__CreateTransactionReferenceSequence;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V12MigrationStreamingGuardTest {

    @Mock
    private Context context;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    @Test
    void failsWhenAutoCommitTrueAndDoesNotCreateOrAlterSequence() throws Exception {
        when(context.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);

        V12__CreateTransactionReferenceSequence migration = new V12__CreateTransactionReferenceSequence();

        assertThatThrownBy(() -> migration.migrate(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auto-commit");

        verify(connection).getAutoCommit();
        verify(connection, never()).createStatement();
    }

    @Test
    void failsWhenFetchSizeIsNonPositive() throws Exception {
        when(context.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(false);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.getFetchSize()).thenReturn(0);

        V12__CreateTransactionReferenceSequence migration = new V12__CreateTransactionReferenceSequence();

        assertThatThrownBy(() -> migration.migrate(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fetch size");

        verify(statement).setFetchSize(1000);
        verify(statement).getFetchSize();
        verify(statement, never()).execute(org.mockito.ArgumentMatchers.argThat(
                (String sql) -> sql != null && sql.toUpperCase().contains("CREATE SEQUENCE")));
        verify(statement, never()).execute(org.mockito.ArgumentMatchers.argThat(
                (String sql) -> sql != null && sql.toUpperCase().contains("ALTER SEQUENCE")));
    }

    @Test
    void failsWhenSetFetchSizeThrows() throws Exception {
        when(context.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(false);
        when(connection.createStatement()).thenReturn(statement);
        org.mockito.Mockito.doThrow(new SQLException("unsupported")).when(statement).setFetchSize(1000);

        V12__CreateTransactionReferenceSequence migration = new V12__CreateTransactionReferenceSequence();

        assertThatThrownBy(() -> migration.migrate(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fetch size");

        verify(statement, never()).execute(org.mockito.ArgumentMatchers.argThat(
                (String sql) -> sql != null && sql.toUpperCase().contains("CREATE SEQUENCE")));
        verify(statement, never()).execute(org.mockito.ArgumentMatchers.argThat(
                (String sql) -> sql != null && sql.toUpperCase().contains("ALTER SEQUENCE")));
    }

    @Test
    void validPathChecksAndSetsNonzeroFetchSizeBeforeSelect() throws Exception {
        when(context.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(false);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.getFetchSize()).thenReturn(1000);
        when(statement.executeQuery("SELECT reference FROM transactions")).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        V12__CreateTransactionReferenceSequence migration = new V12__CreateTransactionReferenceSequence();

        migration.migrate(context);

        InOrder order = inOrder(statement);
        order.verify(statement).setFetchSize(1000);
        order.verify(statement).getFetchSize();
        order.verify(statement).executeQuery("SELECT reference FROM transactions");
    }
}