package com.novawallet.novawallet_api.config;

import com.novawallet.novawallet_api.wallet.repository.WalletRepository;
import com.novawallet.novawallet_api.wallet.service.AccountNumberGenerator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public AccountNumberGenerator testAccountNumberGenerator() {
        WalletRepository walletRepository = mock(WalletRepository.class);
        when(walletRepository.existsByAccountNumber("NW0000000001")).thenReturn(false);
        return new AccountNumberGenerator(walletRepository);
    }
}
