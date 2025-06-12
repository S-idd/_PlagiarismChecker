package com.example.PlagiarismChecker.__CodeFileServiceUnitTestes__;



import com.example.PlagiarismChecker.model.CodeFile;
import com.example.PlagiarismChecker.Repository.CodeFileRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import com.example.PlagiarismChecker.Service.SimilarityResult;
import com.example.PlagiarismChecker.Service.CodeFileService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


@ExtendWith(MockitoExtension.class)
public class CodeFileServiceTest {

	 @MockBean
    private CodeFileRepository codeFileRepository;

	 @MockBean
    private Validator validator;

    @InjectMocks
    private CodeFileService codeFileService;

    private CodeFile codeFile;
    private MockMultipartFile mockFile;

    @BeforeEach
    void setUp() {
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
        // Arrange
        List<CodeFile> files = Arrays.asList(codeFile);
        when(codeFileRepository.findAll()).thenReturn(files);

        // Act
        List<CodeFile> result = codeFileService.GetAllFiles();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFileName()).isEqualTo("test.java");
        verify(codeFileRepository, times(1)).findAll();
    }

    @Test
    void getAllFiles_EmptyList() {
        // Arrange
        when(codeFileRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<CodeFile> result = codeFileService.GetAllFiles();

        // Assert
        assertThat(result).isEmpty();
        verify(codeFileRepository, times(1)).findAll();
    }

    @Test
    void migrateExistingFiles_Success() {
        // Arrange
        CodeFile file = new CodeFile();
        file.setId(1L);
        file.setFileName("test.java");
        file.setContent("public class Test {}");
        file.setLanguage("JAVA");
        List<CodeFile> files = Arrays.asList(file);
        when(codeFileRepository.findAll()).thenReturn(files);
        when(codeFileRepository.save(any(CodeFile.class))).thenReturn(file);

        // Act
        codeFileService.migrateExistingFiles();

        // Assert
        verify(codeFileRepository, times(1)).findAll();
        verify(codeFileRepository, times(1)).save(any(CodeFile.class));
        assertThat(file.Gettrigram_vector()).isNotNull();
    }

    @Test
    void uploadFile_Success() throws IOException {
        // Arrange
        when(validator.validate(any(CodeFile.class))).thenReturn(Collections.emptySet());
        when(codeFileRepository.save(any(CodeFile.class))).thenReturn(codeFile);

        // Act
        CodeFile result = codeFileService.uploadFile(mockFile, "java");

        // Assert
        assertThat(result.getFileName()).isEqualTo("test.java");
        assertThat(result.getLanguage()).isEqualTo("JAVA");
        assertThat(result.Gettrigram_vector()).isNotEmpty();
        verify(validator, times(1)).validate(any(CodeFile.class));
        verify(codeFileRepository, times(1)).save(any(CodeFile.class));
    }

    @Test
    void uploadFile_NullFile_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            codeFileService.uploadFile(null, "java");
        });
        assertThat(exception.getMessage()).isEqualTo("File cannot be empty");
        verify(codeFileRepository, never()).save(any());
    }

    @Test
    void uploadFile_InvalidExtension_ThrowsException() {
        // Arrange
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "content".getBytes()
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            codeFileService.uploadFile(invalidFile, "java");
        });
        assertThat(exception.getMessage()).contains("Invalid file extension");
        verify(codeFileRepository, never()).save(any());
    }

    @Test
    void uploadFile_FileSizeExceedsLimit_ThrowsException() {
        // Arrange
        MultipartFile largeFile = mock(MultipartFile.class);
        when(largeFile.getOriginalFilename()).thenReturn("test.java");
        when(largeFile.getSize()).thenReturn(11 * 1024 * 1024L); // 11MB

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            codeFileService.uploadFile(largeFile, "java");
        });
        assertThat(exception.getMessage()).isEqualTo("File size exceeds 10MB limit");
        verify(codeFileRepository, never()).save(any());
    }

    @Test
    void uploadFile_ValidationFailure_ThrowsException() throws IOException {
        // Arrange
        @SuppressWarnings("unchecked")
        ConstraintViolation<CodeFile> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("File name cannot be blank");
        when(validator.validate(any(CodeFile.class))).thenReturn(Set.of(violation));

        // Act & Assert
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
        // Act
        boolean result = codeFileService.isValidExtension("test.java", "JAVA");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isValidExtension_InvalidExtension_ReturnsFalse() {
        // Act
        boolean result = codeFileService.isValidExtension("test.txt", "JAVA");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isValidExtension_UnsupportedLanguage_ReturnsFalse() {
        // Act
        boolean result = codeFileService.isValidExtension("test.java", "PHP");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void calculateSimilarity_Success() {
        // Arrange
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

        // Act
        double similarity = codeFileService.calculateSimilarity(1L, 2L);

        // Assert
        assertThat(similarity).isGreaterThan(0);
        verify(codeFileRepository, times(1)).findById(1L);
        verify(codeFileRepository, times(1)).findById(2L);
    }

    @Test
    void calculateSimilarity_FileNotFound_ThrowsException() {
        // Arrange
        when(codeFileRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
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
    @MockitoSettings(strictness = Strictness.LENIENT)
    @Test
    void compareAgainstAll_Success() {
        // Arrange
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

        // Act
        Page<SimilarityResult> result = codeFileService.compareAgainstAll(1L, PageRequest.of(0, 10), "java", 0.5);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFileId()).isEqualTo(2L);
        assertThat(result.getContent().get(0).getSimilarity()).isGreaterThanOrEqualTo(0.5);
        verify(codeFileRepository, times(1)).findById(1L);
        when(codeFileRepository.findByLanguage(eq("JAVA"), any(Pageable.class))).thenReturn(new PageImpl<>(files));
    }

    @Test
    void compareAgainstAll_FileNotFound_ThrowsException() {
        // Arrange
        when(codeFileRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            codeFileService.compareAgainstAll(1L, PageRequest.of(0, 10), null, null);
        });
        assertThat(exception.getMessage()).isEqualTo("File not found: 1");
        verify(codeFileRepository, times(1)).findById(1L);
    }

    @Test
    void compareAgainstAll_UnsupportedLanguage_ThrowsException() {
        // Arrange
        when(codeFileRepository.findById(1L)).thenReturn(Optional.of(codeFile));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            codeFileService.compareAgainstAll(1L, PageRequest.of(0, 10), "PHP", null);
        });
        assertThat(exception.getMessage()).contains("Unsupported language: PHP");
        verify(codeFileRepository, times(1)).findById(1L);
    }

    @Test
    void generateTrigrams_Success() {
        // Act
        Map<String, Integer> trigrams = codeFileService.generateTrigrams("public class Test {}", "JAVA");

        // Assert
        assertThat(trigrams).isNotEmpty();
        assertThat(trigrams).containsKey("pub");
        assertThat(trigrams).containsKey("lic");
    }

    @Test
    void generateTrigrams_ShortContent_ReturnsEmptyMap() {
        // Act
        Map<String, Integer> trigrams = codeFileService.generateTrigrams("ab", "JAVA");

        // Assert
        assertThat(trigrams).isEmpty();
    }

    @Test
    void normalizeContent_Success() {
        // Act
        String normalized = codeFileService.normalizeContent("public class Test { // comment\n}", "JAVA");

        // Assert
        assertThat(normalized).isEqualTo("test");
    }

    @Test
    void normalizeContent_NullContent_ReturnsEmptyString() {
        // Act
        String normalized = codeFileService.normalizeContent(null, "JAVA");

        // Assert
        assertThat(normalized).isEmpty();
    }

    @Test
    void deleteAllFiles_Success() {
        // Act
        codeFileService.deleteAllFiles();

        // Assert
        verify(codeFileRepository, times(1)).deleteAll();
    }
    
    @Test
    void testCompareBatchFiles() {
        // Arrange
        CodeFile file1 = new CodeFile();
        file1.setId(1L);
        file1.setFileName("file1.java");
        file1.setLanguage("JAVA");
        file1.setContent("public class Test { void method() { System.out.println(\"Test\"); } }");

        CodeFile file2 = new CodeFile();
        file2.setId(2L);
        file2.setFileName("file2.java");
        file2.setLanguage("JAVA");
        file2.setContent("public class Test { void method() { System.out.println(\"Test2\"); } }");

        CodeFile file3 = new CodeFile();
        file3.setId(3L);
        file3.setFileName("file3.java");
        file3.setLanguage("JAVA");
        file3.setContent("public class Test { void method() { System.out.println(\"Test3\"); } }");

        // Mock repository
        Mockito.when(codeFileRepository.findById(1L)).thenReturn(Optional.of(file1));
        Mockito.when(codeFileRepository.findAllByIdInAndLanguage(Arrays.asList(2L, 3L), "JAVA"))
               .thenReturn(Arrays.asList(file2, file3));

        // Act
        List<SimilarityResult> results = codeFileService.compareBatchFiles(1L, Arrays.asList(2L, 3L), "JAVA", 0.0);

        // Assert
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
        Mockito.when(codeFileRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> 
        codeFileService.compareBatchFiles(1L, Arrays.asList(2L), "JAVA", 0.0), 
            "Should throw IllegalArgumentException for non-existent target file");
    }
}