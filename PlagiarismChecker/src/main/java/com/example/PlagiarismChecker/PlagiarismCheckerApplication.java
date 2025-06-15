package com.example.PlagiarismChecker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EntityScan("com.example.PlagiarismChecker.model")
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class PlagiarismCheckerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlagiarismCheckerApplication.class, args);
	}
}
	