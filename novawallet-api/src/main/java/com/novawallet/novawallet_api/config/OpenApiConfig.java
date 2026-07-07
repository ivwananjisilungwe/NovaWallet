package com.novawallet.novawallet_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {


    @Bean
    public OpenAPI novaWalletOpenAPI() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title("NovaWallet API")
                                .description(
                                        "Digital wallet backend API documentation"
                                )
                                .version("v0.0.1")
                );
    }
}