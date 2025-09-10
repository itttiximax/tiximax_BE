package com.tiximax.txm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class TiximaxApplication {

	public static void main(String[] args) {
		SpringApplication.run(TiximaxApplication.class, args);
	}

}
