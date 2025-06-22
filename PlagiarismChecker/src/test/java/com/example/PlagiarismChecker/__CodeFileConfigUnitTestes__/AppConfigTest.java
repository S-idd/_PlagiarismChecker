package com.example.PlagiarismChecker.__CodeFileConfigUnitTestes__;

import com.example.PlagiarismChecker.Config.AppConfig;
import com.example.PlagiarismChecker.Service.CustomCosineSimilarity;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AppConfigTest {

    @Test
    void testCustomCosineSimilarityBeanCreation() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        CustomCosineSimilarity cosineSimilarity = context.getBean(CustomCosineSimilarity.class);
        assertNotNull(cosineSimilarity, "CustomCosineSimilarity bean should be created");
        context.close();
    }
}
