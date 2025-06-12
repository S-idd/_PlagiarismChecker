package com.example.PlagiarismChecker.Service;

import java.util.Map;

import org.springframework.stereotype.Component;
@Component
public class CustomCosineSimilarity {

    public double cosineSimilarity(Map<String, Integer> vector1, Map<String, Integer> vector2) {
        if (vector1 == null || vector2 == null || vector1.isEmpty() || vector2.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (Map.Entry<String, Integer> entry : vector1.entrySet()) {
            String key = entry.getKey();
            int value1 = entry.getValue();
            norm1 += value1 * value1;
            Integer value2 = vector2.getOrDefault(key, 0);
            dotProduct += value1 * value2;
        }

        for (Map.Entry<String, Integer> entry : vector2.entrySet()) {
            int value2 = entry.getValue();
            norm2 += value2 * value2;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}