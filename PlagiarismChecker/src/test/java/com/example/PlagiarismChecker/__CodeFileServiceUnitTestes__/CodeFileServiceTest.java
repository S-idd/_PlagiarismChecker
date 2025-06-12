
package com.example.PlagiarismChecker.__CodeFileServiceUnitTestes__;

import com.example.PlagiarismChecker.model.CodeFile;
import com.example.PlagiarismChecker.Repository.CodeFileRepository;
import com.example.PlagiarismChecker.Service.CodeFileService;
import com.example.PlagiarismChecker.Service.CustomCosineSimilarity;
import com.example.PlagiarismChecker.Service.SimilarityResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CodeFileServiceTest {

    @Mock
    private CodeFileRepository codeFileRepository;

    @Mock
    private Validator validator;

    @Mock
    private CustomCosineSimilarity cosineSimilarity;

    private CodeFileService codeFileService;

    private CodeFile codeFile;
    private MockMultipartFile mockFile;

    @BeforeEach
    void setUp() {
        codeFileService = new CodeFileService(codeFileRepository, validator, cosineSimilarity);

        codeFile = new CodeFile();
        codeFile.setId(1L);
        codeFile.setFileName("test.java");
        codeFile.setContent("public class Test {}");
        codeFile.setLanguage("JAVA");
        codeFile.setCreatedAt(LocalDateTime.of(2025, 6, 7, 10, 0));
        Map<String, Integer> trigrams = new HashMap<>();
        trigrams.put("pub", 1);
        trigrams.put("lic", 1);
        codeFile.Settrigram_vector(trigrams);

        mockFile = new MockMultipartFile(
                "file",
                "test.java",
                "text/plain",
                "public class Test {}".getBytes()
        );
    }

    @Test
    void getAllFiles_Success() {
        List<CodeFile> files = Arrays.asList(codeFile);
        when(codeFileRepository.findAll()).thenReturn(files);

        List<CodeFile> result = codeFileService.GetAllFiles();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFileName()).isEqualTo("test.java");
        verify(codeFileRepository, times(1)).findAll();
    }

    @Test
    void getAllFiles_EmptyList() {
        when(codeFileRepository.findAll()).thenReturn(Arrays.asList());

        List<CodeFile> result = codeFileService.GetAllFiles();

        assertThat(result).isEmpty();
        verify(codeFileRepository, times(1)).findAll();
    }

    @Test
    void migrateExistingFiles_Success() {
        CodeFile file = new CodeFile();
        file.setId(1L);
        file.setFileName("test.java");
        file.setContent("public class Test {}");
        file.setLanguage("JAVA");
        List<CodeFile> files = Arrays.asList(file);
        when(codeFileRepository.findAll()).thenReturn(files);
        when(codeFileRepository.save(any(CodeFile.class))).thenReturn(file);

        codeFileService.migrateExistingFiles();

        verify(codeFileRepository, times(1)).findAll();
        verify(codeFileRepository, times(1)).save(any(CodeFile.class));
        assertThat(file.Gettrigram_vector()).isNotNull();
    }

    @Test
    void uploadFile_Success() throws IOException {
        when(validator.validate(any(CodeFile.class))).thenReturn(Collections.emptySet());
        when(codeFileRepository.save(any(CodeFile.class))).thenAnswer(invocation -> {
            CodeFile savedFile = invocation.getArgument(0);
            savedFile.setId(1L);
            return savedFile;
        });

        CodeFile result = codeFileService.uploadFile(mockFile, "java");

        assertThat(result.getFileName()).isEqualTo("test.java");
        assertThat(result.getLanguage()).isEqualTo("JAVA");
        assertThat(result.Gettrigram_vector()).isNotEmpty();
        verify(validator, times(1)).validate(any(CodeFile.class));
        verify(codeFileRepository, times(1)).save(any(CodeFile.class));
    }

    @Test
    void uploadFile_NullFile_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            codeFileService.uploadFile(null, "java");
        });
        assertThat(exception.getMessage()).isEqualTo("File cannot be empty");
        verify(codeFileRepository, never()).save(any());
    }

    @Test
    void uploadFile_InvalidExtension_ThrowsException() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "content".getBytes()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            codeFileService.uploadFile(invalidFile, "java");
        });
        assertThat(exception.getMessage()).contains("Invalid file extension");
        verify(codeFileRepository, never()).save(any());
    }

    @Test
    void uploadFile_FileSizeExceedsLimit_ThrowsException() {
        MultipartFile largeFile = mock(MultipartFile.class);
        when(largeFile.getOriginalFilename()).thenReturn("test.java");
        when(largeFile.getSize()).thenReturn(11 * 1024 * 1024L); // 11MB

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            codeFileService.uploadFile(largeFile, "java");
        });
        assertThat(exception.getMessage()).isEqualTo("File size exceeds 10MB limit");
        verify(codeFileRepository, never()).save(any());
    }

    @Test
    void uploadFile_ValidationFailure_ThrowsException() throws IOException {
        @SuppressWarnings("unchecked")
        ConstraintViolation<CodeFile> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("File name cannot be blank");
        when(validator.validate(any(CodeFile.class))).thenReturn(Set.of(violation));

        ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> {
            codeFileService.uploadFile(mockFile, "java");
        });
        assertThat(exception.getConstraintViolations()).hasSize(1);
        assertThat(exception.getConstraintViolations().iterator().next().getMessage())
                .isEqualTo("File name cannot be blank");
        verify(codeFileRepository, never()).save(any());
    }

    @Test
    void isValidExtension_ValidExtension_ReturnsTrue() {
        boolean result = codeFileService.isValidExtension("test.java", "JAVA");

        assertThat(result).isTrue();
    }

    @Test
    void isValidExtension_InvalidExtension_ReturnsFalse() {
        boolean result = codeFileService.isValidExtension("test.txt", "JAVA");

        assertThat(result).isFalse();
    }

    @Test
    void isValidExtension_UnsupportedLanguage_ReturnsFalse() {
        boolean result = codeFileService.isValidExtension("test.java", "PHP");

        assertThat(result).isFalse();
    }

    @Test
    void calculateSimilarity_Success() {
        CodeFile file2 = new CodeFile();
        file2.setId(2L);
        file2.setFileName("other.java");
        file2.setLanguage("JAVA");
        Map<String, Integer> trigrams2 = new HashMap<>();
        trigrams2.put("pub", 1);
        trigrams2.put("lic", 1);
        file2.Settrigram_vector(trigrams2);

        when(codeFileRepository.findById(1L)).thenReturn(Optional.of(codeFile));
        when(codeFileRepository.findById(2L)).thenReturn(Optional.of(file2));
        when(cosineSimilarity.cosineSimilarity(any(), any())).thenReturn(0.95);

        double similarity = codeFileService.calculateSimilarity(1L, 2L);

        assertThat(similarity).isGreaterThan(0);
        verify(codeFileRepository, times(1)).findById(1L);
        verify(codeFileRepository, times(1)).findById(2L);
    }

    @Test
    void calculateSimilarity_FileNotFound_ThrowsException() {
        when(codeFileRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            codeFileService.calculateSimilarity(1L, 2L);
        });
        assertThat(exception.getMessage()).isEqualTo("File not found: 1");
        verify(codeFileRepository, times(1)).findById(1L);
    }

    @Test
    void calculateSimilarity_NullTrigramVector_ThrowsException() {
        CodeFile file1 = new CodeFile();
        file1.setId(1L);
        file1.Settrigram_vector(null);
        CodeFile file2 = new CodeFile();
        file2.setId(2L);
        file2.Settrigram_vector(Map.of("pub", 1));

        when(codeFileRepository.findById(1L)).thenReturn(Optional.of(file1));
        when(codeFileRepository.findById(2L)).thenReturn(Optional.of(file2));

        assertThrows(IllegalStateException.class, () -> codeFileService.calculateSimilarity(1L, 2L));
    }

    @Test
    void compareAgainstAll_Success() {
        CodeFile file2 = new CodeFile();
        file2.setId(2L);
        file2.setFileName("other.java");
        file2.setLanguage("JAVA");
        Map<String, Integer> trigrams2 = new HashMap<>();
        trigrams2.put("pub", 1);
        file2.Settrigram_vector(trigrams2);

        List<CodeFile> files = Arrays.asList(file2);
        when(codeFileRepository.findById(1L)).thenReturn(Optional.of(codeFile));
        when(codeFileRepository.findByLanguage(eq("JAVA"), any(Pageable.class))).thenReturn(new PageImpl<>(files));
        when(cosineSimilarity.cosineSimilarity(any(), any())).thenReturn(0.95);

        Page<SimilarityResult> result = codeFileService.compareAgainstAll(1L, PageRequest.of(0, 10), "java", 0.5);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFileId()).isEqualTo(2L);
        assertThat(result.getContent().get(0).getSimilarity()).isGreaterThanOrEqualTo(0.5);
        verify(codeFileRepository, times(1)).findById(1L);
        verify(codeFileRepository, times(1)).findByLanguage(eq("JAVA"), any(Pageable.class));
    }

    @Test
    void compareAgainstAll_FileNotFound_ThrowsException() {
        when(codeFileRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            codeFileService.compareAgainstAll(1L, PageRequest.of(0, 10), null, null);
        });
        assertThat(exception.getMessage()).isEqualTo("File not found: 1");
        verify(codeFileRepository, times(1)).findById(1L);
    }

    @Test
    void compareAgainstAll_UnsupportedLanguage_ThrowsException() {
        when(codeFileRepository.findById(1L)).thenReturn(Optional.of(codeFile));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            codeFileService.compareAgainstAll(1L, PageRequest.of(0, 10), "PHP", null);
        });
        assertThat(exception.getMessage()).contains("Unsupported language: PHP");
        verify(codeFileRepository, times(1)).findById(1L);
    }

    @Test
    void generateTrigrams_Success() {
        Map<String, Integer> trigrams = codeFileService.generateTrigrams("public class Test {}", "JAVA");

        assertThat(trigrams).isNotEmpty();
        assertThat(trigrams).containsKey("pub");
        assertThat(trigrams).containsKey("lic");
    }

    @Test
    void generateTrigrams_ShortContent_ReturnsEmptyMap() {
        Map<String, Integer> trigrams = codeFileService.generateTrigrams("ab", "JAVA");

        assertThat(trigrams).isEmpty();
    }

    @Test
    void normalizeContent_Success() {
        String normalized = codeFileService.normalizeContent("public class Test { // comment\n}", "JAVA");

        assertThat(normalized).isEqualTo("test");
    }

    @Test
    void normalizeContent_NullContent_ReturnsEmptyString() {
        String normalized = codeFileService.normalizeContent(null, "JAVA");

        assertThat(normalized).isEmpty();
    }

    @Test
    void deleteAllFiles_Success() {
        codeFileService.deleteAllFiles();

        verify(codeFileRepository, times(1)).deleteAll();
    }

    @Test
    void testCompareBatchFiles() {
        CodeFile file1 = new CodeFile();
        file1.setId(1L);
        file1.setFileName("file1.java");
        file1.setLanguage("JAVA");
        file1.setContent("public class Test { void method() { System.out.println(\"Test\"); } }");
        Map<String, Integer> trigrams1 = new HashMap<>();
        trigrams1.put("pub", 1);
        trigrams1.put("lic", 1);
        file1.Settrigram_vector(trigrams1);

        CodeFile file2 = new CodeFile();
        file2.setId(2L);
        file2.setFileName("file2.java");
        file2.setLanguage("JAVA");
        file2.setContent("public class Test { void method() { System.out.println(\"Test2\"); } }");
        Map<String, Integer> trigrams2 = new HashMap<>();
        trigrams2.put("pub", 1);
        trigrams2.put("lic", 1);
        file2.Settrigram_vector(trigrams2);

        CodeFile file3 = new CodeFile();
        file3.setId(3L);
        file3.setFileName("file3.java");
        file3.setLanguage("JAVA");
        file3.setContent("public class Test { void method() { System.out.println(\"Test3\"); } }");
        Map<String, Integer> trigrams3 = new HashMap<>();
        trigrams3.put("pub", 1);
        trigrams3.put("lic", 1);
        file3.Settrigram_vector(trigrams3);

        when(codeFileRepository.findById(1L)).thenReturn(Optional.of(file1));
        when(codeFileRepository.findAllById(Arrays.asList(2L, 3L)))
               .thenReturn(Arrays.asList(file2, file3));
        when(cosineSimilarity.cosineSimilarity(any(), any())).thenReturn(0.95);

        List<SimilarityResult> results = codeFileService.compareBatchFiles(1L, Arrays.asList(2L, 3L), "JAVA", 0.0);

        System.out.println("Results: " + results);

        assertFalse(results.isEmpty(), "Results should not be empty");
        assertEquals(2, results.size(), "Should return two results");
        assertTrue(results.stream().anyMatch(r -> r.getFileId().equals(2L)), "Should contain file2");
        assertTrue(results.stream().anyMatch(r -> r.getFileId().equals(3L)), "Should contain file3");
        assertTrue(results.get(0).getSimilarity() >= 0.0, "Similarity should be non-negative");
    }

    @Test
    void testCompareBatchFilesNullIds() {
        assertThrows(IllegalArgumentException.class, () -> 
            codeFileService.compareBatchFiles(1L, null, "JAVA", 0.0), 
            "Should throw IllegalArgumentException for null fileIds");
    }

    @Test
    void testCompareBatchFilesNonExistentTargetFile() {
        when(codeFileRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> 
            codeFileService.compareBatchFiles(1L, Arrays.asList(2L), "JAVA", 0.0), 
            "Should throw IllegalArgumentException for non-existent target file");
    }
}
