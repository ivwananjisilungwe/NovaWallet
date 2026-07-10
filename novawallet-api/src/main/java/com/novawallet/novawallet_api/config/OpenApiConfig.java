package com.novawallet.novawallet_api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";


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
                )
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        SCHEME_NAME,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description("Paste your JWT token here. Obtain one via POST /v1/auth/login")
                                )
                )
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(SCHEME_NAME)
                );
    }
}