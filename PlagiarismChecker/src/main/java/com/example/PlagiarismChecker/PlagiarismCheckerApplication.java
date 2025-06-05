package com.example.PlagiarismChecker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EntityScan("com.example.PlagiarismChecker.model")
@EnableCaching
public class PlagiarismCheckerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlagiarismCheckerApplication.class, args);
	}

}
