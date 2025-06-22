package com.example.PlagiarismChecker.__CodeFileServiceUnitTestes__;



import org.junit.jupiter.api.Test;

import com.example.PlagiarismChecker.Service.CustomCosineSimilarity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;;

class CustomCosineSimilarityTest {

    private final CustomCosineSimilarity cosineSimilarity = new CustomCosineSimilarity();

    @Test
    void testCosineSimilaritySuccess() {
        Map<String, Integer> vector1 = new HashMap<>(Map.of("abc", 1, "def", 2));
        Map<String, Integer> vector2 = new HashMap<>(Map.of("abc", 2, "def", 1));

        double result = cosineSimilarity.cosineSimilarity(vector1, vector2);
        assertEquals(0.8, result, 0.01) ;// Expected: (1*2 + 2*1)/(sqrt(5)*sqrt(5)) = 0.8
    }

    @Test
    void testCosineSimilarityEmptyVector() {
        Map<String, Integer> vector1 = new HashMap<>();
        Map<String, Integer> vector2 = new HashMap<>(Map.of("abc", 1));

        double result = cosineSimilarity.cosineSimilarity(vector1, vector2);
        assertEquals(0.0, result);
    }

    @Test
    void testCosineSimilarityNullVector() {
        double result = cosineSimilarity.cosineSimilarity(null, new HashMap<>(Map.of("abc", 1)));
        assertEquals(0.0, result);
    }

    @Test
    void testCosineSimilarityZeroNorm() {
        Map<String, Integer> vector1 = new HashMap<>(Map.of("abc", 0));
        Map<String, Integer> vector2 = new HashMap<>(Map.of("abc", 0));

        double result = cosineSimilarity.cosineSimilarity(vector1, vector2);
        assertEquals(0.0, result);
    }
}
