package com.example.PlagiarismChecker.__CodeFileServiceUnitTestes__;

import org.junit.jupiter.api.Test;

import com.example.PlagiarismChecker.Service.SimilarityResult;

import static org.junit.jupiter.api.Assertions.*;

class SimilarityResultTest {

    @Test
    void testSimilarityResultConstructorAndGetters() {
        SimilarityResult result = new SimilarityResult(1L, "test.java", "JAVA", 0.85);

        assertEquals(1L, result.getFileId());
        assertEquals("test.java", result.getFileName());
        assertEquals("JAVA", result.getLanguage());
        assertEquals(0.85, result.getSimilarity(), 0.01);
    }

    @Test
    void testSimilarityResultSetters() {
        SimilarityResult result = new SimilarityResult(1L, "test.java", "JAVA", 0.85);

        result.setfileId(2L);
        result.setfileName("other.java");
        result.setLanguage("PYTHON");
        result.setSimilarity(0.90);

        assertEquals(2L, result.getFileId());
        assertEquals("other.java", result.getFileName());
        assertEquals("PYTHON", result.getLanguage());
        assertEquals(0.90, result.getSimilarity(), 0.01);
    }
}
