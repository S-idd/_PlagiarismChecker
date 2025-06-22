package com.example.PlagiarismChecker.Service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UploadConsumer {

    private static final Logger logger = LoggerFactory.getLogger(UploadConsumer.class);

    @Autowired
    private CodeFileService codeFileService;

    @RabbitListener(queues = "uploadQueue")
    public void processUploadMessage(Map<String, Object> message) {
        String filePath = (String) message.get("filePath");
        String fileName = (String) message.get("fileName");
        String language = (String) message.get("language");

        File tempFile = new File(filePath);
        if (tempFile.exists()) {
            try (FileInputStream inputStream = new FileInputStream(tempFile)) {
                codeFileService.uploadFileStream(inputStream, fileName, language);
                logger.info("Successfully processed and saved file: {}", fileName);
            } catch (IOException e) {
                logger.error("Failed to process queued file {}: {}", fileName, e.getMessage(), e);
            } finally {
                tempFile.delete(); // Clean up temporary file
            }
        } else {
            logger.error("Temporary file {} not found at path: {}", fileName, filePath);
        }
    }
}
