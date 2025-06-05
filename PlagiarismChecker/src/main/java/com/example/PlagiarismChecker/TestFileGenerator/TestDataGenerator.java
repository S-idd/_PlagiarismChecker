package com.example.PlagiarismChecker.TestFileGenerator;

/**
 * Never Run This File In Your 8 GB RAM PC Or Laptop Your System Will Be Crashed In seconds 
 * 
 * */
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.FileSystemResource;

public class TestDataGenerator {

    private static final String[] LANGUAGES = {"JAVA", "PYTHON", "CPP", "GO", "JAVASCRIPT", "RUBY"};
    private static final String[] EXTENSIONS = {".java", ".py", ".cpp", ".go", ".js", ".rb"};
    private static final int[] FILE_COUNTS = {30, 20, 20, 10, 10, 10};

    public static void main(String[] args) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        String uploadUrl = "http://localhost:8080/api/code-files/upload";

//        for (int i = 0; i < LANGUAGES.length; i++) {
//            String language = LANGUAGES[i];
//            String extension = EXTENSIONS[i];
//            int count = FILE_COUNTS[i];
//
//            for (int j = 1; j <= count; j++) {
//                // Generate a random file
//                String fileName = "TestFile_" + language + "_" + j + extension;
//                File tempFile = File.createTempFile("test_", extension);
//                String content = generateRandomContent(1024 * new Random().nextInt(500)); // 1KB to 500KB
//                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
//                    fos.write(content.getBytes(StandardCharsets.UTF_8));
//                }
//
//                // Prepare the multipart request
//                HttpHeaders headers = new HttpHeaders();
//                headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//
//                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//                body.add("file", new FileSystemResource(tempFile));
//                body.add("language", language);
//
//                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
//
//                // Upload the file
//                try {
//                    String response = restTemplate.postForObject(uploadUrl, requestEntity, String.class);
//                    System.out.println("Uploaded: " + fileName + " - Response: " + response);
//                } catch (Exception e) {
//                    System.err.println("Failed to upload " + fileName + ": " + e.getMessage());
//                }
//
//                // Delete the temp file
//                tempFile.delete();
//            }
//        }
    }

//    private static String generateRandomContent(int bytes) {
//        StringBuilder content = new StringBuilder();
//        Random random = new Random();
//        String chars = "abcdefghijklmnopqrstuvwxyz\n\t ";
//        for (int i = 0; i < bytes; i++) {
//            content.append(chars.charAt(random.nextInt(chars.length())));
//        }
//        return content.toString();
//    }
}