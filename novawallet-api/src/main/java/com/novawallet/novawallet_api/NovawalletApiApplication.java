package com.novawallet.novawallet_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NovawalletApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(NovawalletApiApplication.class, args);
	}

}
