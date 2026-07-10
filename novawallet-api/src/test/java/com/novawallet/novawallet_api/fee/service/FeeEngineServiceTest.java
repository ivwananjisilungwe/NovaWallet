package com.novawallet.novawallet_api.fee.service;

import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import com.novawallet.novawallet_api.fee.dto.FeeEstimateResponse;
import com.novawallet.novawallet_api.fee.entity.FeeConfiguration;
import com.novawallet.novawallet_api.fee.enums.FeeType;
import com.novawallet.novawallet_api.fee.repository.FeeConfigurationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeeEngineServiceTest {

    @Mock
    private FeeConfigurationRepository feeConfigurationRepository;

    private FeeEngineService feeEngineService;

    @BeforeEach
    void setUp() {
        feeEngineService = new FeeEngineService(feeConfigurationRepository);
    }

    // ==================== calculateFee ====================

    @Nested
    class CalculateFee {

        @Test
        void shouldCalculatePercentageOnlyFee() {
            FeeConfiguration config = FeeConfiguration.builder()
                    .transactionType(FeeType.TRANSFER)
                    .percentageFee(new BigDecimal("1.0000"))  // 1%
                    .flatFee(BigDecimal.ZERO)
                    .minFee(null)
                    .maxFee(null)
                    .active(true)
                    .build();

            when(feeConfigurationRepository
                    .findByTransactionTypeAndActiveTrue(FeeType.TRANSFER))
                    .thenReturn(Optional.of(config));

            BigDecimal fee = feeEngineService.calculateFee(FeeType.TRANSFER, new BigDecimal("200.00"));

            // 1% of 200 = 2.00
            assertThat(fee).isEqualByComparingTo("2.00");
        }

        @Test
        void shouldCalculateFlatFeeOnly() {
            FeeConfiguration config = FeeConfiguration.builder()
                    .transactionType(FeeType.WITHDRAWAL)
                    .percentageFee(BigDecimal.ZERO)
                    .flatFee(new BigDecimal("1.50"))
                    .minFee(null)
                    .maxFee(null)
                    .active(true)
                    .build();

            when(feeConfigurationRepository
                    .findByTransactionTypeAndActiveTrue(FeeType.WITHDRAWAL))
                    .thenReturn(Optional.of(config));

            BigDecimal fee = feeEngineService.calculateFee(FeeType.WITHDRAWAL, new BigDecimal("100.00"));

            // 0% + 1.50 = 1.50
            assertThat(fee).isEqualByComparingTo("1.50");
        }

        @Test
        void shouldCalculatePercentagePlusFlatFee() {
            FeeConfiguration config = FeeConfiguration.builder()
                    .transactionType(FeeType.TRANSFER)
                    .percentageFee(new BigDecimal("0.5000"))  // 0.5%
                    .flatFee(new BigDecimal("0.25"))
                    .minFee(null)
                    .maxFee(null)
                    .active(true)
                    .build();

            when(feeConfigurationRepository
                    .findByTransactionTypeAndActiveTrue(FeeType.TRANSFER))
                    .thenReturn(Optional.of(config));

            BigDecimal fee = feeEngineService.calculateFee(FeeType.TRANSFER, new BigDecimal("1000.00"));

            // 0.5% of 1000 = 5.00 + 0.25 = 5.25
            assertThat(fee).isEqualByComparingTo("5.25");
        }

        @Test
        void shouldClampToMinFee() {
            FeeConfiguration config = FeeConfiguration.builder()
                    .transactionType(FeeType.TRANSFER)
                    .percentageFee(new BigDecimal("0.5000"))  // 0.5%
                    .flatFee(BigDecimal.ZERO)
                    .minFee(new BigDecimal("2.00"))
                    .maxFee(null)
                    .active(true)
                    .build();

            when(feeConfigurationRepository
                    .findByTransactionTypeAndActiveTrue(FeeType.TRANSFER))
                    .thenReturn(Optional.of(config));

            // 0.5% of 50 = 0.25 → clamped to min 2.00
            BigDecimal fee = feeEngineService.calculateFee(FeeType.TRANSFER, new BigDecimal("50.00"));

            assertThat(fee).isEqualByComparingTo("2.00");
        }

        @Test
        void shouldClampToMaxFee() {
            FeeConfiguration config = FeeConfiguration.builder()
                    .transactionType(FeeType.TRANSFER)
                    .percentageFee(new BigDecimal("1.0000"))  // 1%
                    .flatFee(BigDecimal.ZERO)
                    .minFee(null)
                    .maxFee(new BigDecimal("20.00"))
                    .active(true)
                    .build();

            when(feeConfigurationRepository
                    .findByTransactionTypeAndActiveTrue(FeeType.TRANSFER))
                    .thenReturn(Optional.of(config));

            // 1% of 5000 = 50.00 → clamped to max 20.00
            BigDecimal fee = feeEngineService.calculateFee(FeeType.TRANSFER, new BigDecimal("5000.00"));

            assertThat(fee).isEqualByComparingTo("20.00");
        }

        @Test
        void shouldReturnZeroWhenNoActiveConfigFound() {
            when(feeConfigurationRepository
                    .findByTransactionTypeAndActiveTrue(FeeType.DEPOSIT))
                    .thenReturn(Optional.empty());

            BigDecimal fee = feeEngineService.calculateFee(FeeType.DEPOSIT, new BigDecimal("100.00"));

            assertThat(fee).isEqualByComparingTo("0.00");
        }

        @Test
        void shouldHandleInactiveConfigAsIfMissing() {
            FeeConfiguration config = FeeConfiguration.builder()
                    .transactionType(FeeType.WITHDRAWAL)
                    .percentageFee(new BigDecimal("1.0000"))
                    .flatFee(BigDecimal.ZERO)
                    .active(false)
                    .build();

            // When config is inactive, findByTransactionTypeAndActiveTrue returns empty
            when(feeConfigurationRepository
                    .findByTransactionTypeAndActiveTrue(FeeType.WITHDRAWAL))
                    .thenReturn(Optional.empty());

            BigDecimal fee = feeEngineService.calculateFee(FeeType.WITHDRAWAL, new BigDecimal("100.00"));

            assertThat(fee).isEqualByComparingTo("0.00");
            verify(feeConfigurationRepository)
                    .findByTransactionTypeAndActiveTrue(FeeType.WITHDRAWAL);
        }

        @Test
        void shouldRoundToTwoDecimals() {
            FeeConfiguration config = FeeConfiguration.builder()
                    .transactionType(FeeType.TRANSFER)
                    .percentageFee(new BigDecimal("0.3333"))  // 1/3%
                    .flatFee(BigDecimal.ZERO)
                    .minFee(null)
                    .maxFee(null)
                    .active(true)
                    .build();

            when(feeConfigurationRepository
                    .findByTransactionTypeAndActiveTrue(FeeType.TRANSFER))
                    .thenReturn(Optional.of(config));

            // 0.3333% of 100.00 = 0.3333 → rounded to 0.33 (HALF_EVEN)
            BigDecimal fee = feeEngineService.calculateFee(FeeType.TRANSFER, new BigDecimal("100.00"));

            assertThat(fee).isEqualByComparingTo("0.33");
        }
    }

    // ==================== estimateFee ====================

    @Nested
    class EstimateFee {

        @Test
        void shouldReturnDetailedEstimate() {
            FeeConfiguration config = FeeConfiguration.builder()
                    .transactionType(FeeType.TRANSFER)
                    .percentageFee(new BigDecimal("1.0000"))
                    .flatFee(new BigDecimal("0.50"))
                    .minFee(new BigDecimal("1.00"))
                    .maxFee(new BigDecimal("50.00"))
                    .active(true)
                    .build();

            when(feeConfigurationRepository
                    .findByTransactionTypeAndActiveTrue(FeeType.TRANSFER))
                    .thenReturn(Optional.of(config));

            FeeEstimateResponse estimate = feeEngineService.estimateFee(FeeType.TRANSFER, new BigDecimal("200.00"));

            assertThat(estimate.transactionType()).isEqualTo("TRANSFER");
            assertThat(estimate.amount()).isEqualByComparingTo("200.00");
            assertThat(estimate.percentageFee()).isEqualByComparingTo("1.0000");
            assertThat(estimate.flatFee()).isEqualByComparingTo("0.50");
            assertThat(estimate.minFee()).isEqualByComparingTo("1.00");
            assertThat(estimate.maxFee()).isEqualByComparingTo("50.00");
            // 1% of 200 = 2.00 + 0.50 = 2.50 (within 1.00–50.00)
            assertThat(estimate.totalFee()).isEqualByComparingTo("2.50");
        }

        @Test
        void shouldReturnZeroFeeEstimateWhenNoConfig() {
            when(feeConfigurationRepository
                    .findByTransactionTypeAndActiveTrue(FeeType.DEPOSIT))
                    .thenReturn(Optional.empty());

            FeeEstimateResponse estimate = feeEngineService.estimateFee(FeeType.DEPOSIT, new BigDecimal("100.00"));

            assertThat(estimate.transactionType()).isEqualTo("DEPOSIT");
            assertThat(estimate.totalFee()).isEqualByComparingTo("0.00");
            assertThat(estimate.percentageFee()).isEqualByComparingTo("0.00");
            assertThat(estimate.flatFee()).isEqualByComparingTo("0.00");
        }
    }

    // ==================== getActiveConfiguration ====================

    @Nested
    class GetActiveConfiguration {

        @Test
        void shouldReturnActiveConfig() {
            FeeConfiguration config = FeeConfiguration.builder()
                    .transactionType(FeeType.TRANSFER)
                    .percentageFee(new BigDecimal("1.0000"))
                    .flatFee(BigDecimal.ZERO)
                    .active(true)
                    .build();

            when(feeConfigurationRepository
                    .findByTransactionTypeAndActiveTrue(FeeType.TRANSFER))
                    .thenReturn(Optional.of(config));

            FeeConfiguration result = feeEngineService.getActiveConfiguration(FeeType.TRANSFER);

            assertThat(result).isNotNull();
            assertThat(result.getTransactionType()).isEqualTo(FeeType.TRANSFER);
        }

        @Test
        void shouldThrowWhenNoActiveConfig() {
            when(feeConfigurationRepository
                    .findByTransactionTypeAndActiveTrue(FeeType.WITHDRAWAL))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> feeEngineService.getActiveConfiguration(FeeType.WITHDRAWAL))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No active fee configuration for WITHDRAWAL");
        }
    }

    // ==================== getConfigurationById ====================

    @Nested
    class GetConfigurationById {

        @Test
        void shouldReturnConfigWhenFound() {
            UUID id = UUID.randomUUID();
            FeeConfiguration config = FeeConfiguration.builder()
                    .transactionType(FeeType.TRANSFER)
                    .percentageFee(BigDecimal.ONE)
                    .flatFee(BigDecimal.ZERO)
                    .build();

            when(feeConfigurationRepository.findById(id)).thenReturn(Optional.of(config));

            FeeConfiguration result = feeEngineService.getConfigurationById(id);

            assertThat(result).isNotNull();
        }

        @Test
        void shouldThrowWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(feeConfigurationRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> feeEngineService.getConfigurationById(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Fee configuration not found");
        }
    }

    // ==================== updateConfiguration ====================

    @Nested
    class UpdateConfiguration {

        @Test
        void shouldUpdateAndReturnConfig() {
            UUID id = UUID.randomUUID();
            FeeConfiguration existing = FeeConfiguration.builder()
                    .transactionType(FeeType.TRANSFER)
                    .percentageFee(new BigDecimal("1.0000"))
                    .flatFee(BigDecimal.ZERO)
                    .active(true)
                    .build();

            FeeConfiguration updated = FeeConfiguration.builder()
                    .transactionType(FeeType.WITHDRAWAL)
                    .percentageFee(new BigDecimal("0.5000"))
                    .flatFee(new BigDecimal("1.00"))
                    .minFee(new BigDecimal("0.50"))
                    .maxFee(new BigDecimal("25.00"))
                    .active(false)
                    .build();

            when(feeConfigurationRepository.findById(id)).thenReturn(Optional.of(existing));
            when(feeConfigurationRepository.save(any(FeeConfiguration.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            FeeConfiguration result = feeEngineService.updateConfiguration(id, updated);

            assertThat(result.getTransactionType()).isEqualTo(FeeType.WITHDRAWAL);
            assertThat(result.getPercentageFee()).isEqualByComparingTo("0.5000");
            assertThat(result.getFlatFee()).isEqualByComparingTo("1.00");
            assertThat(result.getMinFee()).isEqualByComparingTo("0.50");
            assertThat(result.getMaxFee()).isEqualByComparingTo("25.00");
            assertThat(result.isActive()).isFalse();

            verify(feeConfigurationRepository).save(existing);
        }

        @Test
        void shouldThrowWhenUpdatingNonExistent() {
            UUID id = UUID.randomUUID();
            when(feeConfigurationRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    feeEngineService.updateConfiguration(id, mock(FeeConfiguration.class)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== getAllConfigurations ====================

    @Nested
    class GetAllConfigurations {

        @Test
        void shouldReturnAllConfigs() {
            List<FeeConfiguration> configs = List.of(
                    FeeConfiguration.builder().transactionType(FeeType.TRANSFER)
                            .percentageFee(BigDecimal.ONE).flatFee(BigDecimal.ZERO).build(),
                    FeeConfiguration.builder().transactionType(FeeType.WITHDRAWAL)
                            .percentageFee(BigDecimal.ONE).flatFee(BigDecimal.ZERO).build()
            );

            when(feeConfigurationRepository.findAll()).thenReturn(configs);

            List<FeeConfiguration> result = feeEngineService.getAllConfigurations();

            assertThat(result).hasSize(2);
        }
    }
}
