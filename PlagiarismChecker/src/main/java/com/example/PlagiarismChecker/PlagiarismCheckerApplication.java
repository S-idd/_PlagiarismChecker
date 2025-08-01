package com.example.PlagiarismChecker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.web.config.EnableSpringDataWebSupport;



@SpringBootApplication
@EntityScan("com.example.PlagiarismChecker.model")
@EnableCaching 
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class PlagiarismCheckerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlagiarismCheckerApplication.class, args);
	}
	
	
}
//import com.fasterxml.jackson.core.JacksonException;
//import com.fasterxml.jackson.core.JsonFactory;

//import jakarta.annotation.PostConstruct;

//@PostConstruct
//public void logJacksonVersion() {
////    System.out.println("Jackson Core Version: " + JsonFactory.class.getPackage().getImplementationVersion());
////    System.out.println("Jackson Databind Version: " + JacksonException.class.getPackage().getImplementationVersion());
//}
	