package com.novawallet.novawallet_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Spring Boot application entry point for NovaWallet API. */
@SpringBootApplication
@EnableScheduling
public class NovawalletApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(NovawalletApiApplication.class, args);
	}

}
