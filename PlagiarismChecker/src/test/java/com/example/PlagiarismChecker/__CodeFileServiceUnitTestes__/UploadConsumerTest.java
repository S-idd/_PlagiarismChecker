package com.example.PlagiarismChecker.__CodeFileServiceUnitTestes__;



import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.PlagiarismChecker.Service.CodeFileService;
import com.example.PlagiarismChecker.Service.UploadConsumer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UploadConsumerTest {

    @Mock
    private CodeFileService codeFileService;

    @InjectMocks
    private UploadConsumer uploadConsumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessUploadMessageSuccess() throws Exception {
        File tempFile = File.createTempFile("upload_", ".java");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write("public class Test {}".getBytes());
        }

        Map<String, Object> message = new HashMap<>();
        message.put("filePath", tempFile.getAbsolutePath());
        message.put("fileName", "test.java");
        message.put("language", "java");

        uploadConsumer.processUploadMessage(message);

        verify(codeFileService, times(1)).uploadFileStream(any(), eq("test.java"), eq("java"));
        assertFalse(tempFile.exists(), "Temporary file should be deleted");
    }

    @Test
    void testProcessUploadMessageFileNotFound() throws IOException {
        Map<String, Object> message = new HashMap<>();
        message.put("filePath", "/nonexistent/path/test.java");
        message.put("fileName", "test.java");
        message.put("language", "java");

        uploadConsumer.processUploadMessage(message);

        verify(codeFileService, never()).uploadFileStream(any(), anyString(), anyString());
    }

    @Test
    void testProcessUploadMessageIOException() throws Exception {
        File tempFile = File.createTempFile("upload_", ".java");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write("public class Test {}".getBytes());
        }

        Map<String, Object> message = new HashMap<>();
        message.put("filePath", tempFile.getAbsolutePath());
        message.put("fileName", "test.java");
        message.put("language", "java");

        doThrow(new IOException("IO error")).when(codeFileService).uploadFileStream(any(), eq("test.java"), eq("java"));

        uploadConsumer.processUploadMessage(message);

        verify(codeFileService, times(1)).uploadFileStream(any(), eq("test.java"), eq("java"));
        assertFalse(tempFile.exists(), "Temporary file should be deleted");
    }
}
