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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CodeFileServiceTest {

    @Mock
    private CodeFileRepository codeFileRepository;

    @Mock
    private Validator validator;

    @Mock
    private CustomCosineSimilarity cosineSimilarity;

    @InjectMocks
    private CodeFileService codeFileService;

    private CodeFile codeFile;
    private MockMultipartFile mockFile;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        codeFile = new CodeFile();
        codeFile.setId(1L);
        codeFile.setFileName("test.java");
        codeFile.setContent("public class Test {}");
        codeFile.setLanguage("JAVA");
        codeFile.setCreatedAt(LocalDateTime.now());
        codeFile.Settrigram_vector(new HashMap<>(Map.of("pub", 1, "lic", 1)));

        mockFile = new MockMultipartFile(
                "file",
                "test.java",
                "text/plain",
                "public class Test {}".getBytes()
        );

        // Set SUPPORTED_LANGUAGES via reflection to avoid static block issues
        Map<String, String[]> supportedLanguages = new HashMap<>();
        supportedLanguages.put("JAVA", new String[]{".java"});
        supportedLanguages.put("PYTHON", new String[]{".py", ".ipynb"});
        supportedLanguages.put("CPP", new String[]{".cpp", ".h", ".hpp"});
        supportedLanguages.put("GO", new String[]{".go"});
        supportedLanguages.put("RUBY", new String[]{".rb"});
        supportedLanguages.put("ADA", new String[]{".ada", ".adb", ".ads"});
        supportedLanguages.put("JAVASCRIPT", new String[]{".js"});
        supportedLanguages.put("TYPESCRIPT", new String[]{".ts"});
        ReflectionTestUtils.setField(codeFileService, "SUPPORTED_LANGUAGES", supportedLanguages);
    }

    @Test
    void testGetAllFiles() {
        when(codeFileRepository.findAll()).thenReturn(Arrays.asList(codeFile));

        List<CodeFile> files = codeFileService.GetAllFiles();
        assertEquals(1, files.size());
        assertEquals("test.java", files.get(0).getFileName());

        verify(codeFileRepository, times(1)).findAll();
    }

    @Test
    void testUploadFileStreamSuccess() throws IOException {
        when(validator.validate(any(CodeFile.class))).thenReturn(Collections.emptySet());
        when(codeFileRepository.save(any(CodeFile.class))).thenReturn(codeFile);

        CodeFile result = codeFileService.uploadFileStream(
                new ByteArrayInputStream("public class Test {}".getBytes()),
                "test.java",
                "java"
        );

        assertNotNull(result);
        assertEquals("test.java", result.getFileName());
        assertEquals("JAVA", result.getLanguage());
        assertFalse(result.Gettrigram_vector().isEmpty());

        verify(validator, times(1)).validate(any(CodeFile.class));
        verify(codeFileRepository, times(1)).save(any(CodeFile.class));
    }

    @Test
    void testUploadFileStreamNullInputStream() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> codeFileService.uploadFileStream(null, "test.java", "java")
        );
        assertEquals("File input stream cannot be null", exception.getMessage());
    }

    @Test
    void testUploadFileStreamInvalidExtension() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> codeFileService.uploadFileStream(
                        new ByteArrayInputStream("content".getBytes()),
                        "test.txt",
                        "java"
                )
        );
        assertEquals("Invalid file extension for language JAVA. Supported extensions: .java", exception.getMessage());
    }

    @Test
    void testUploadFileStreamTooLarge() throws IOException {
        ByteArrayInputStream largeStream = new ByteArrayInputStream(new byte[11 * 1024 * 1024]);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> codeFileService.uploadFileStream(largeStream, "test.java", "java")
        );
        assertEquals("File size exceeds 10MB limit", exception.getMessage());
    }

    @Test
    void testUploadFileStreamEmptyContent() throws IOException {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> codeFileService.uploadFileStream(
                        new ByteArrayInputStream("// comment".getBytes()),
                        "test.java",
                        "java"
                )
        );
        assertEquals("File content is empty after normalization for file: test.java", exception.getMessage());
    }

    @Test
    void testUploadFileStreamConstraintViolation() throws IOException {
        ConstraintViolation<CodeFile> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Validation failed");
        when(validator.validate(any(CodeFile.class))).thenReturn(Set.of(violation));

        ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class,
                () -> codeFileService.uploadFileStream(
                        new ByteArrayInputStream("public class Test {}".getBytes()),
                        "test.java",
                        "java"
                )
        );
        assertEquals("Validation failed", exception.getConstraintViolations().iterator().next().getMessage());
    }

    @Test
    void testCalculateSimilaritySuccess() {
        when(codeFileRepository.findById(1L)).thenReturn(Optional.of(codeFile));
        when(codeFileRepository.findById(2L)).thenReturn(Optional.of(codeFile));
        when(cosineSimilarity.cosineSimilarity(anyMap(), anyMap())).thenReturn(0.85);

        double similarity = codeFileService.calculateSimilarity(1L, 2L);
        assertEquals(85.0, similarity, 0.01);

        verify(codeFileRepository, times(2)).findById(anyLong());
        verify(cosineSimilarity, times(1)).cosineSimilarity(anyMap(), anyMap());
    }

    @Test
    void testCalculateSimilarityFileNotFound() {
        when(codeFileRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> codeFileService.calculateSimilarity(1L, 2L)
        );
        assertEquals("File not found: 1", exception.getMessage());
    }

    @Test
    void testCalculateSimilarityEmptyTrigramVector() {
        CodeFile emptyTrigramFile = new CodeFile();
        emptyTrigramFile.setId(2L);
        emptyTrigramFile.setFileName("test2.java");
        emptyTrigramFile.setContent("public class Test2 {}");
        emptyTrigramFile.setLanguage("JAVA");
        emptyTrigramFile.Settrigram_vector(new HashMap<>());

        when(codeFileRepository.findById(1L)).thenReturn(Optional.of(codeFile));
        when(codeFileRepository.findById(2L)).thenReturn(Optional.of(emptyTrigramFile));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> codeFileService.calculateSimilarity(1L, 2L)
        );
        assertEquals("Trigram vector not found or empty for file ID: 2", exception.getMessage());
    }

    @Test
    void testCompareAgainstAllSuccess() {
        CodeFile otherFile = new CodeFile();
        otherFile.setId(2L);
        otherFile.setFileName("other.java");
        otherFile.setLanguage("JAVA");
        otherFile.setContent("public class Other {}");
        otherFile.Settrigram_vector(new HashMap<>(Map.of("pub", 1)));

        Page<CodeFile> page = new PageImpl<>(Arrays.asList(otherFile));
        when(codeFileRepository.findById(1L)).thenReturn(Optional.of(codeFile));
        when(codeFileRepository.findByLanguage(eq("JAVA"), any(Pageable.class))).thenReturn(page);
        when(cosineSimilarity.cosineSimilarity(anyMap(), anyMap())).thenReturn(0.75);

        Page<SimilarityResult> result = codeFileService.compareAgainstAll(1L, PageRequest.of(0, 10), "java", 0.5);
        assertEquals(1, result.getContent().size());
        assertEquals(2L, result.getContent().get(0).getFileId());
        assertEquals(75.0, result.getContent().get(0).getSimilarity(), 0.01);

        verify(codeFileRepository, times(1)).findById(1L);
        verify(codeFileRepository, times(1)).findByLanguage(eq("JAVA"), any(Pageable.class));
        verify(cosineSimilarity, times(1)).cosineSimilarity(anyMap(), anyMap());
    }

    @Test
    void testCompareAgainstAllInvalidLanguage() {
        when(codeFileRepository.findById(1L)).thenReturn(Optional.of(codeFile));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> codeFileService.compareAgainstAll(1L, PageRequest.of(0, 10), "INVALID", 0.5)
        );
        assertEquals("Unsupported language: INVALID. Supported languages: " + String.join(", ", new String[]{"JAVA", "PYTHON", "CPP", "GO", "RUBY", "ADA", "JAVASCRIPT", "TYPESCRIPT"}), exception.getMessage());
    }

    @Test
    void testCompareBatchFilesSuccess() {
        CodeFile otherFile = new CodeFile();
        otherFile.setId(2L);
        otherFile.setFileName("other.java");
        otherFile.setLanguage("JAVA");
        otherFile.setContent("public class Other {}");
        otherFile.Settrigram_vector(new HashMap<>(Map.of("pub", 1)));

        when(codeFileRepository.findById(1L)).thenReturn(Optional.of(codeFile));
        when(codeFileRepository.findAllById(Arrays.asList(2L))).thenReturn(Arrays.asList(otherFile));
        when(cosineSimilarity.cosineSimilarity(anyMap(), anyMap())).thenReturn(0.75);

        List<SimilarityResult> results = codeFileService.compareBatchFiles(1L, Arrays.asList(2L), "java", 0.5);
        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).getFileId());
        assertEquals(75.0, results.get(0).getSimilarity(), 0.01);

        verify(codeFileRepository, times(1)).findById(1L);
        verify(codeFileRepository, times(1)).findAllById(Arrays.asList(2L));
        verify(cosineSimilarity, times(1)).cosineSimilarity(anyMap(), anyMap());
    }

    @Test
    void testCompareBatchFilesEmptyFileIds() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> codeFileService.compareBatchFiles(1L, new ArrayList<>(), "java", 0.5)
        );
        assertEquals("fileIds must not be null or empty", exception.getMessage());
    }

    @Test
    void testCompareBatchFilesTooManyIds() {
        List<Long> tooManyIds = new ArrayList<>();
        for (long i = 1; i <= 101; i++) {
            tooManyIds.add(i);
        }

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> codeFileService.compareBatchFiles(1L, tooManyIds, "java", 0.5)
        );
        assertEquals("Too many file IDs; max is 100", exception.getMessage());
    }

    @Test
    void testNormalizeContentJava() {
        String content = "public class Test {\n// comment\n}";
        String normalized = codeFileService.normalizeContent(content, "JAVA");
        assertEquals("class test", normalized);
    }

    @Test
    void testGenerateTrigrams() {
        String content = "class test";
        Map<String, Integer> trigrams = codeFileService.generateTrigrams(content, "JAVA");
        assertTrue(trigrams.containsKey("cla"));
        assertTrue(trigrams.containsKey("ass"));
        assertTrue(trigrams.containsKey("tes"));
        assertEquals(1, trigrams.get("cla").intValue());
    }

    @Test
    void testUploadBatchFilesSuccess() throws IOException {
        when(validator.validate(any(CodeFile.class))).thenReturn(Collections.emptySet());
        when(codeFileRepository.saveAll(anyList())).thenReturn(Arrays.asList(codeFile));

        List<CodeFile> results = codeFileService.uploadBatchFiles(Arrays.asList(mockFile), "java");
        assertEquals(1, results.size());
        assertEquals("test.java", results.get(0).getFileName());

        verify(validator, times(1)).validate(any(CodeFile.class));
        verify(codeFileRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testUploadBatchFilesEmpty() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> codeFileService.uploadBatchFiles(new ArrayList<>(), "java")
        );
        assertEquals("No files provided for batch upload", exception.getMessage());
    }

    @Test
    void testDeleteAllFiles() {
        codeFileService.deleteAllFiles();
        verify(codeFileRepository, times(1)).deleteAll();
    }
}