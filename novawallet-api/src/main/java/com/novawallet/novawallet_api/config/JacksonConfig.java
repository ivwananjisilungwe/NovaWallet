package com.novawallet.novawallet_api.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Jackson configuration for input sanitization.
 * Strips HTML tags and trims whitespace from all incoming string fields
 * as defense-in-depth against stored XSS and log injection.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Module stringSanitizerModule() {
        SimpleModule module = new SimpleModule("StringSanitizer");
        module.addDeserializer(String.class, new JsonDeserializer<>() {
            @Override
            public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String raw = p.getValueAsString();
                if (raw == null) {
                    return null;
                }
                return sanitize(raw);
            }
        });
        return module;
    }

    private static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        // Strip HTML tags and common XSS vectors
        String sanitized = input
                .replaceAll("<[^>]*>", "")
                .replaceAll("(?i)javascript:", "")
                .replaceAll("(?i)on\\w+\\s*=", "")
                .trim();
        return sanitized;
    }
}
