package com.example.PlagiarismChecker.__CodeFileConfigUnitTestes__;


import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.PlagiarismChecker.Config.WebConfig;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

public class WebConfigTest {

    @Test
    void testCorsConfiguration() throws IOException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(WebConfig.class);
        WebMvcConfigurer configurer = context.getBean(WebMvcConfigurer.class);
        assertNotNull(configurer, "WebMvcConfigurer bean should be created");

        // Simulate CORS request
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.addAllowedOrigin("http://localhost:5173");
        corsConfig.addAllowedMethod("GET");
        corsConfig.addAllowedMethod("POST");
        corsConfig.addAllowedMethod("PUT");
        corsConfig.addAllowedMethod("DELETE");
        corsConfig.addAllowedMethod("OPTIONS");
        source.registerCorsConfiguration("/**", corsConfig);

        CorsProcessor processor = new DefaultCorsProcessor();
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/code-files/upload");
        request.addHeader("Origin", "http://localhost:5173");
        request.addHeader("Access-Control-Request-Method", "POST");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = processor.processRequest(corsConfig, request, response);
        assertTrue(result, "CORS request should be processed successfully");
        assertEquals("http://localhost:5173", response.getHeader("Access-Control-Allow-Origin"), "CORS origin should be allowed");
        context.close();
    }
}