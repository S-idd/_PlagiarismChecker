package com.example.PlagiarismChecker.Service;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class CustomCosineSimilarity {

    public double cosineSimilarity(Map<String, Integer> vector1, Map<String, Integer> vector2) {
        if (vector1 == null || vector2 == null || vector1.isEmpty() || vector2.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (Map.Entry<String, Integer> e1 : vector1.entrySet()) {
            String key = e1.getKey();
            int v1 = e1.getValue();
            norm1 += (long) v1 * v1;  // prevent overflow
            Integer v2 = vector2.get(key);
            if (v2 != null) {
                dotProduct += (long) v1 * v2;
            }
        }

        for (Integer v2 : vector2.values()) {
            norm2 += (long) v2 * v2;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}