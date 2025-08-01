package com.example.PlagiarismChecker.Service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class MessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendUploadMessage(MultipartFile file, String language) throws IOException {
        File tempFile = File.createTempFile("upload_", file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }

        Map<String, Object> message = new HashMap<>();
        message.put("filePath", tempFile.getAbsolutePath());
        message.put("fileName", file.getOriginalFilename());
        message.put("language", language);

        rabbitTemplate.convertAndSend("uploadQueue", message);
        // tempFile.deleteOnExit(); // Optional cleanup
    }
}
