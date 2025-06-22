package com.example.PlagiarismChecker.__CodeFileControllerUnitTestes__;


import com.example.PlagiarismChecker.Controller.CodeFileController;
import com.example.PlagiarismChecker.Service.CodeFileService;
import com.example.PlagiarismChecker.Service.MessageProducer;
import com.example.PlagiarismChecker.Service.SimilarityResult;
import com.example.PlagiarismChecker.model.CodeFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CodeFileControllerTestes {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CodeFileService codeFileService;

    @MockBean
    private MessageProducer messageProducer;

    @Autowired
    private ObjectMapper objectMapper;

    private CodeFile codeFile;
    private MockMultipartFile mockFile;

    @BeforeEach
    void setUp() {
        codeFile = new CodeFile();
        codeFile.setId(1L);
        codeFile.setFileName("test.java");
        codeFile.setLanguage("java");

        mockFile = new MockMultipartFile(
                "file",
                "test.java",
                "text/plain",
                "public class Test {}".getBytes()
        );
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testUploadFileSuccess() throws Exception {
        when(codeFileService.uploadFileStream(any(), eq("test.java"), eq("java"))).thenReturn(codeFile);

        mockMvc.perform(multipart("/api/code-files/upload")
                        .file(mockFile)
                        .param("language", "java")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.filename").value("test.java"))
                .andExpect(jsonPath("$.language").value("java"));

        verify(codeFileService, times(1)).uploadFileStream(any(), eq("test.java"), eq("java"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testUploadFileIOException() throws Exception {
        when(codeFileService.uploadFileStream(any(), anyString(), anyString())).thenThrow(new IOException("IO error"));

        mockMvc.perform(multipart("/api/code-files/upload")
                        .file(mockFile)
                        .param("language", "java")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Failed to upload file: IO error"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testUploadFileInvalidArgument() throws Exception {
        when(codeFileService.uploadFileStream(any(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Invalid file type"));

        mockMvc.perform(multipart("/api/code-files/upload")
                        .file(mockFile)
                        .param("language", "java")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid file type or size: Invalid file type"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testUploadFileConstraintViolation() throws Exception {
        when(codeFileService.uploadFileStream(any(), anyString(), anyString()))
                .thenThrow(new ConstraintViolationException("Validation failed", null));

        mockMvc.perform(multipart("/api/code-files/upload")
                        .file(mockFile)
                        .param("language", "java")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Validation error: Validation failed"));
    }

    @Test
    @WithAnonymousUser
    void testUploadFileUnauthorized() throws Exception {
        mockMvc.perform(multipart("/api/code-files/upload")
                        .file(mockFile)
                        .param("language", "java")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"Realm\""));
    }

    @Test
    void testUploadFileWithHttpBasic() throws Exception {
        when(codeFileService.uploadFileStream(any(), eq("test.java"), eq("java"))).thenReturn(codeFile);

        mockMvc.perform(multipart("/api/code-files/upload")
                        .file(mockFile)
                        .param("language", "java")
                        .with(httpBasic("admin", "root"))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testCompareFilesSuccess() throws Exception {
        when(codeFileService.calculateSimilarity(1L, 2L)).thenReturn(0.85);

        mockMvc.perform(get("/api/code-files/compare")
                        .param("fileId1", "1")
                        .param("fileId2", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("0.85"));

        verify(codeFileService, times(1)).calculateSimilarity(1L, 2L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testCompareFilesInvalidRequest() throws Exception {
        when(codeFileService.calculateSimilarity(anyLong(), anyLong()))
                .thenThrow(new IllegalArgumentException("Invalid file IDs"));

        mockMvc.perform(get("/api/code-files/compare")
                        .param("fileId1", "1")
                        .param("fileId2", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid comparison request: Invalid file IDs"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testCompareFilesRuntimeException() throws Exception {
        when(codeFileService.calculateSimilarity(anyLong(), anyLong()))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/api/code-files/compare")
                        .param("fileId1", "1")
                        .param("fileId2", "2"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Unexpected error during comparison: Unexpected error"));
    }

    @Test
    @WithAnonymousUser
    void testCompareFilesUnauthorized() throws Exception {
        mockMvc.perform(get("/api/code-files/compare")
                        .param("fileId1", "1")
                        .param("fileId2", "2"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"Realm\""));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testCompareAgainstAllSuccess() throws Exception {
        SimilarityResult result = new SimilarityResult(2L, "other.java", "java", 0.75);
        Page<SimilarityResult> page = new PageImpl<>(Arrays.asList(result), PageRequest.of(0, 10), 1);
        when(codeFileService.compareAgainstAll(eq(1L), any(), eq("java"), eq(0.5))).thenReturn(page);

        mockMvc.perform(get("/api/code-files/compare-all/1")
                        .param("page", "0")
                        .param("size", "10")
                        .param("languageFilter", "java")
                        .param("minSimilarity", "0.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fileId").value(2L))
                .andExpect(jsonPath("$.content[0].filename").value("other.java"))
                .andExpect(jsonPath("$.content[0].similarity").value(0.75))
                .andExpect(jsonPath("$.content[0].language").value("java"));

        verify(codeFileService, times(1)).compareAgainstAll(eq(1L), any(), eq("java"), eq(0.5));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testGetAllFilesSuccess() throws Exception {
        when(codeFileService.GetAllFiles()).thenReturn(Arrays.asList(codeFile));

        mockMvc.perform(get("/api/code-files/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].filename").value("test.java"))
                .andExpect(jsonPath("$[0].language").value("java"));

        verify(codeFileService, times(1)).GetAllFiles();
    }

    @Test
    @WithAnonymousUser
    void testGetAllFilesUnauthorized() throws Exception {
        mockMvc.perform(get("/api/code-files/files"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"Realm\""));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testUploadBatchFilesSuccess() throws Exception {
        List<MockMultipartFile> files = Arrays.asList(mockFile);
        doNothing().when(messageProducer).sendUploadMessage(any(), eq("java"));

        mockMvc.perform(multipart("/api/code-files/upload/batch")
                        .file(mockFile)
                        .param("language", "java")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().string("Batch upload queued successfully"));

        verify(messageProducer, times(1)).sendUploadMessage(any(), eq("java"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testUploadBatchFilesEmpty() throws Exception {
        mockMvc.perform(multipart("/api/code-files/upload/batch")
                        .param("language", "java")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No files provided for batch upload"));
    }

    @Test
    @WithAnonymousUser
    void testUploadBatchFilesUnauthorized() throws Exception {
        mockMvc.perform(multipart("/api/code-files/upload/batch")
                        .file(mockFile)
                        .param("language", "java")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"Realm\""));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testCompareBatchFilesSuccess() throws Exception {
        CodeFileController.BatchCompareRequest request = new CodeFileController.BatchCompareRequest();
        request.setTargetFileId(1L);
        request.setFileIds(Arrays.asList(2L, 3L));
        request.setLanguageFilter("java");
        request.setMinSimilarity(0.5);

        SimilarityResult result = new SimilarityResult(2L, "other.java", "java", 0.75);
        when(codeFileService.compareBatchFiles(eq(1L), eq(Arrays.asList(2L, 3L)), eq("java"), eq(0.5)))
                .thenReturn(Arrays.asList(result));

        mockMvc.perform(post("/api/code-files/compare/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileId").value(2L))
                .andExpect(jsonPath("$[0].filename").value("other.java"))
                .andExpect(jsonPath("$[0].similarity").value(0.75))
                .andExpect(jsonPath("$[0].language").value("java"));

        verify(codeFileService, times(1)).compareBatchFiles(eq(1L), eq(Arrays.asList(2L, 3L)), eq("java"), eq(0.5));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void testCompareBatchFilesForbidden() throws Exception {
        CodeFileController.BatchCompareRequest request = new CodeFileController.BatchCompareRequest();
        request.setTargetFileId(1L);
        request.setFileIds(Arrays.asList(2L, 3L));

        mockMvc.perform(post("/api/code-files/compare/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void testCompareBatchFilesUnauthorized() throws Exception {
        CodeFileController.BatchCompareRequest request = new CodeFileController.BatchCompareRequest();
        request.setTargetFileId(1L);
        request.setFileIds(Arrays.asList(2L, 3L));

        mockMvc.perform(post("/api/code-files/compare/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"Realm\""));
    }

    @Test
    void testCorsPreflightRequest() throws Exception {
        mockMvc.perform(options("/api/code-files/files")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"));
    }
}