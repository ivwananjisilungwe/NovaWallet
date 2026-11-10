package com.novawallet.novawallet_api.transaction.service;

import com.novawallet.novawallet_api.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionReferenceGeneratorTest {

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionReferenceGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new TransactionReferenceGenerator(transactionRepository);
    }

    @Test
    void generate_matchesFormatAndCurrentDate() {
        when(transactionRepository.nextReferenceSequence()).thenReturn(1L);

        String reference = generator.generate();

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertThat(reference).matches("^NWTX\\d{8}\\d{8}$");
        assertThat(reference).startsWith("NWTX" + today);
        assertThat(reference).hasSize(20);
    }

    @Test
    void generate_zeroPadsSequence() {
        when(transactionRepository.nextReferenceSequence()).thenReturn(7L);

        String reference = generator.generate();

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertThat(reference).isEqualTo("NWTX" + today + "00000007");
    }

    @Test
    void generate_usesRepositorySequenceValue() {
        when(transactionRepository.nextReferenceSequence()).thenReturn(42L);

        String reference = generator.generate();

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertThat(reference).isEqualTo("NWTX" + today + "00000042");
    }

    @Test
    void generate_sequenceZeroThrows() {
        when(transactionRepository.nextReferenceSequence()).thenReturn(0L);

        assertThatThrownBy(() -> generator.generate())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generate_sequenceTooLargeThrows() {
        when(transactionRepository.nextReferenceSequence()).thenReturn(100_000_000L);

        assertThatThrownBy(() -> generator.generate())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generate_nullSequenceThrows() {
        when(transactionRepository.nextReferenceSequence()).thenReturn(null);

        assertThatThrownBy(() -> generator.generate())
                .isInstanceOf(IllegalStateException.class);
    }
}
