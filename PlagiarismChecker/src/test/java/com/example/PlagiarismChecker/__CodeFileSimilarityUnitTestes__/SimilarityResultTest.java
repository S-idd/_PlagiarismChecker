package com.example.PlagiarismChecker.__CodeFileSimilarityUnitTestes__;

import com.example.PlagiarismChecker.Service.SimilarityResult;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class SimilarityResultTest {
    @Test
    void test() {
        // Arrange
        SimilarityResult result = new SimilarityResult(1L, "test.java", "JAVA", 0.75);

        // Assert
        assertThat(result.getFileId()).isEqualTo(1L);
        assertThat(result.getFileName()).isEqualTo("test.java");
        assertThat(result.getLanguage()).isEqualTo("JAVA");
        assertThat(result.getSimilarity()).isEqualTo(0.75);
    }
}