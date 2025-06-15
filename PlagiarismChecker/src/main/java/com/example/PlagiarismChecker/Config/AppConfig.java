package com.example.PlagiarismChecker.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.PlagiarismChecker.Service.CustomCosineSimilarity;

@Configuration
public class AppConfig {
    @Bean
    public CustomCosineSimilarity customCosineSimilarity() {
        return new CustomCosineSimilarity();
    }
}
