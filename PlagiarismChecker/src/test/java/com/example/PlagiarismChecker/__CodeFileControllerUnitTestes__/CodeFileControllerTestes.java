package com.example.PlagiarismChecker.__CodeFileControllerUnitTestes__;

import com.example.PlagiarismChecker.Controller.CodeFileController;
import com.example.PlagiarismChecker.Service.CodeFileService;
import com.example.PlagiarismChecker.Service.SimilarityResult;
import com.example.PlagiarismChecker.model.CodeFile;
/**
 * import com.fasterxml.jackson.databind.ObjectMapper;
 * **/
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CodeFileControllerTestes {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private CodeFileService codeFileService;

	private CodeFile sampleCodeFile;
	private MockMultipartFile mockFile;

	@BeforeEach
	void setUp() {
		// Initialize sample data
		sampleCodeFile = new CodeFile();
		sampleCodeFile.setId(1L);
		sampleCodeFile.setFileName("test.java");
		sampleCodeFile.setLanguage("java");

		mockFile = new MockMultipartFile("file", "test.java", "text/plain", "public class Test {}".getBytes());
	}

	// Tests for POST /api/code-files/upload
	@Test
	void uploadFile_Success() throws Exception {
		// Arrange
		when(codeFileService.uploadFile(any(MockMultipartFile.class), eq("java"))).thenReturn(sampleCodeFile);

		// Act & Assert
		mockMvc.perform(multipart("/api/code-files/upload").file(mockFile).param("language", "java")
				.contentType(MediaType.MULTIPART_FORM_DATA)).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1L)).andExpect(jsonPath("$.fileName").value("test.java"))
				.andExpect(jsonPath("$.language").value("java"));

		verify(codeFileService, times(1)).uploadFile(any(MockMultipartFile.class), eq("java"));
	}

	@Test
	void uploadFile_InvalidFileType() throws Exception {
		// Arrange
		when(codeFileService.uploadFile(any(MockMultipartFile.class), eq("java")))
				.thenThrow(new IllegalArgumentException("Invalid file type"));

		// Act & Assert
		mockMvc.perform(multipart("/api/code-files/upload").file(mockFile).param("language", "java")
				.contentType(MediaType.MULTIPART_FORM_DATA)).andExpect(status().isBadRequest())
				.andExpect(content().string("Invalid file type: Invalid file type"));

		verify(codeFileService, times(1)).uploadFile(any(MockMultipartFile.class), eq("java"));
	}

	@Test
	void uploadFile_FileSizeExceedsLimit() throws Exception {
		// Arrange
		when(codeFileService.uploadFile(any(MockMultipartFile.class), eq("java")))
				.thenThrow(new MaxUploadSizeExceededException(10_000_000));

		// Act & Assert
		mockMvc.perform(multipart("/api/code-files/upload").file(mockFile).param("language", "java")
				.contentType(MediaType.MULTIPART_FORM_DATA)).andExpect(status().isBadRequest())
				.andExpect(content().string("File size exceeds the maximum limit of 10MB"));

		verify(codeFileService, times(1)).uploadFile(any(MockMultipartFile.class), eq("java"));
	}

	@Test
	void uploadFile_MissingLanguage() throws Exception {
		// Act & Assert
		mockMvc.perform(multipart("/api/code-files/upload").file(mockFile).contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(status().isBadRequest());

		verify(codeFileService, never()).uploadFile(any(), any());
	}

	// Tests for GET /api/code-files/compare
	@Test
	void compareFiles_Success() throws Exception {
		// Arrange
		when(codeFileService.calculateSimilarity(1L, 2L)).thenReturn(0.85);

		// Act & Assert
		mockMvc.perform(get("/api/code-files/compare").param("fileId1", "1").param("fileId2", "2")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(content().string("0.85"));

		verify(codeFileService, times(1)).calculateSimilarity(1L, 2L);
	}

	@Test
	void compareFiles_InvalidFileId() throws Exception {
		// Arrange
		when(codeFileService.calculateSimilarity(1L, 999L))
				.thenThrow(new IllegalArgumentException("File with ID 999 not found"));

		// Act & Assert
		mockMvc.perform(get("/api/code-files/compare").param("fileId1", "1").param("fileId2", "999")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(content().string("Invalid comparison request: File with ID 999 not found"));

		verify(codeFileService, times(1)).calculateSimilarity(1L, 999L);
	}

	@Test
	void compareFiles_MissingParameters() throws Exception {
		// Act & Assert
		mockMvc.perform(get("/api/code-files/compare").param("fileId1", "1").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());

		verify(codeFileService, never()).calculateSimilarity(anyLong(), anyLong());
	}

	@Test
	void compareFiles_UnexpectedError() throws Exception {
		// Arrange
		when(codeFileService.calculateSimilarity(1L, 2L)).thenThrow(new RuntimeException("Database connection error"));

		// Act & Assert
		mockMvc.perform(get("/api/code-files/compare").param("fileId1", "1").param("fileId2", "2")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isInternalServerError())
				.andExpect(content().string("Unexpected error during comparison: Database connection error"));

		verify(codeFileService, times(1)).calculateSimilarity(1L, 2L);
	}

	// Tests for GET /api/code-files/compare-all/{fileId}
	@Test
	void compareAgainstAll_Success() throws Exception {
		// Arrange
		SimilarityResult result1 = new SimilarityResult(2L, "other.java", "java", 0.75);
		SimilarityResult result2 = new SimilarityResult(3L, "another.java", "java", 0.60);
		Page<SimilarityResult> pageResult = new PageImpl<>(Arrays.asList(result1, result2), PageRequest.of(0, 10), 2);

		when(codeFileService.compareAgainstAll(eq(1L), any(Pageable.class), eq(null), eq(null))).thenReturn(pageResult);

		// Act & Assert
		mockMvc.perform(get("/api/code-files/compare-all/1").param("page", "0").param("size", "10")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].fileId").value(2L))
				.andExpect(jsonPath("$.content[0].fileName").value("other.java"))
				.andExpect(jsonPath("$.content[0].language").value("java"))
				.andExpect(jsonPath("$.content[0].similarity").value(0.75))
				.andExpect(jsonPath("$.content[1].fileId").value(3L))
				.andExpect(jsonPath("$.content[1].fileName").value("another.java"))
				.andExpect(jsonPath("$.content[1].language").value("java"))
				.andExpect(jsonPath("$.content[1].similarity").value(0.60))
				.andExpect(jsonPath("$.totalElements").value(2));

		verify(codeFileService, times(1)).compareAgainstAll(eq(1L), any(Pageable.class), eq(null), eq(null));
	}

	@Test
	void compareAgainstAll_WithLanguageFilterAndMinSimilarity() throws Exception {
		// Arrange
		SimilarityResult result = new SimilarityResult(2L, "other.java", "java", 0.80);
		Page<SimilarityResult> pageResult = new PageImpl<>(Arrays.asList(result), PageRequest.of(0, 5), 1);

		when(codeFileService.compareAgainstAll(eq(1L), any(Pageable.class), eq("java"), eq(0.7)))
				.thenReturn(pageResult);

		// Act & Assert
		mockMvc.perform(get("/api/code-files/compare-all/1").param("page", "0").param("size", "5")
				.param("languageFilter", "java").param("minSimilarity", "0.7").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].fileId").value(2L))
				.andExpect(jsonPath("$.content[0].fileName").value("other.java"))
				.andExpect(jsonPath("$.content[0].language").value("java"))
				.andExpect(jsonPath("$.content[0].similarity").value(0.80))
				.andExpect(jsonPath("$.totalElements").value(1));

		verify(codeFileService, times(1)).compareAgainstAll(eq(1L), any(Pageable.class), eq("java"), eq(0.7));
	}

	@Test
	void compareAgainstAll_InvalidFileId() throws Exception {
		// Arrange
		when(codeFileService.compareAgainstAll(eq(999L), any(Pageable.class), eq(null), eq(null)))
				.thenThrow(new IllegalArgumentException("File with ID 999 not found"));

		// Act & Assert
		mockMvc.perform(get("/api/code-files/compare-all/999").param("page", "0").param("size", "10")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(content().string("Invalid comparison request: File with ID 999 not found"));

		verify(codeFileService, times(1)).compareAgainstAll(eq(999L), any(Pageable.class), eq(null), eq(null));
	}

	@Test
	void compareAgainstAll_InvalidPaginationParameters() throws Exception {
		// Act & Assert
		mockMvc.perform(get("/api/code-files/compare-all/1").param("page", "-1").param("size", "0")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());

		verify(codeFileService, never()).compareAgainstAll(anyLong(), any(Pageable.class), any(), any());
	}

	// Tests for GET /api/code-files/files
	@Test
	void getAllFiles_Success() throws Exception {
		// Arrange
		CodeFile file1 = new CodeFile();
		file1.setId(1L);
		file1.setFileName("test1.java");
		file1.setLanguage("java");

		CodeFile file2 = new CodeFile();
		file2.setId(2L);
		file2.setFileName("test2.java");
		file2.setLanguage("java");

		List<CodeFile> files = Arrays.asList(file1, file2);
		when(codeFileService.GetAllFiles()).thenReturn(files);

		// Act & Assert
		mockMvc.perform(get("/api/code-files/files").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1L)).andExpect(jsonPath("$[0].fileName").value("test1.java"))
				.andExpect(jsonPath("$[0].language").value("java")).andExpect(jsonPath("$[1].id").value(2L))
				.andExpect(jsonPath("$[1].fileName").value("test2.java"))
				.andExpect(jsonPath("$[1].language").value("java"));

		verify(codeFileService, times(1)).GetAllFiles();
	}

	@Test
	void getAllFiles_EmptyList() throws Exception {
		// Arrange
		when(codeFileService.GetAllFiles()).thenReturn(Arrays.asList());

		// Act & Assert
		mockMvc.perform(get("/api/code-files/files").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());

		verify(codeFileService, times(1)).GetAllFiles();
	}
	
	@Test
    void uploadBatchFiles_ValidFiles_ReturnsSuccess() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "test1.java", "text/plain",
                "public class Test1 {}".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "test2.java", "text/plain",
                "public class Test2 {}".getBytes());
        CodeFile savedFile1 = new CodeFile();
        savedFile1.setId(1L);
        savedFile1.setFileName("test1.java");
        CodeFile savedFile2 = new CodeFile();
        savedFile2.setId(2L);
        savedFile2.setFileName("test2.java");

        when(codeFileService.uploadBatchFiles(List.of(file1, file2), "JAVA"))
                .thenReturn(List.of(savedFile1, savedFile2));

        mockMvc.perform(multipart("/api/code-files/upload/batch")
                        .file(file1)
                        .file(file2)
                        .param("language", "JAVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void compareBatchFiles_ValidRequest_ReturnsResults() throws Exception {
        CodeFileController.BatchCompareRequest request = new CodeFileController.BatchCompareRequest();
        request.setTargetFileId(1L);
        request.setFileIds(List.of(2L, 3L));
        request.setLanguageFilter("JAVA");
        request.setMinSimilarity(20.0);

        SimilarityResult result = new SimilarityResult(2L, "test2.java", "JAVA", 95.0);
        when(codeFileService.compareBatchFiles(1L, List.of(2L, 3L), "JAVA", 20.0))
                .thenReturn(List.of(result));

        mockMvc.perform(post("/api/code-files/compare/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetFileId\":1,\"fileIds\":[2,3],\"languageFilter\":\"JAVA\",\"minSimilarity\":20.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileId").value(2))
                .andExpect(jsonPath("$[0].similarity").value(95.0));
    }
}