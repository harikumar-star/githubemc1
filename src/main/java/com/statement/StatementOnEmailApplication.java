package com.statement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Enable scheduled tasks
public class StatementOnEmailApplication {

	public static void main(String[] args) {
		SpringApplication.run(StatementOnEmailApplication.class, args);
	}

}
