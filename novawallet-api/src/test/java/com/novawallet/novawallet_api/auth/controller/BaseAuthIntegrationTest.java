package com.novawallet.novawallet_api.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novawallet.novawallet_api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseAuthIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    protected String testEmail;
    protected String testPhone;
    protected static int counter = 0;

    @BeforeEach
    void setUp() {
        counter++;
        testEmail = "test" + counter + "@example.com";
        testPhone = "+26097123" + String.format("%04d", counter + 1000);
    }
}
