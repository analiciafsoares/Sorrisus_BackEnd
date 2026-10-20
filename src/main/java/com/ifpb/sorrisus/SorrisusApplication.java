package com.ifpb.sorrisus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SorrisusApplication {

	public static void main(String[] args) {
		SpringApplication.run(SorrisusApplication.class, args);
	}

}
