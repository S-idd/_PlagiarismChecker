package com.example.PlagiarismChecker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("com.example.PlagiarismChecker.model")
public class PlagiarismCheckerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlagiarismCheckerApplication.class, args);
	}

}
