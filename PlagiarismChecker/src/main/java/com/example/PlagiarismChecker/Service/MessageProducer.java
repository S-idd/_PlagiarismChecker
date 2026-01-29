package com.example.PlagiarismChecker.Service;

import com.example.PlagiarismChecker.DTO.BatchUploadMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducer.class);
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private UploadJobService uploadJobService;

    /**
     * Sends batch upload message - writes all files to temp directory
     * and sends ONE message to RabbitMQ
     * 
     * Target: <200ms for 2400 files
     */
    public String sendBatchUploadMessage(List<MultipartFile> files, String language) throws IOException {
        String jobId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        
        logger.info("Starting batch upload for job {}: {} files", jobId, files.size());
        
        // Create temporary directory for this batch
        Path tempDir = Files.createTempDirectory("batch_" + jobId);
        tempDir.toFile().deleteOnExit();
        
        List<BatchUploadMessage.FileMetadata> fileMetadataList = new ArrayList<>();
        
        // Write all files to temp directory in parallel
        files.parallelStream().forEach(file -> {
            try {
                String tempFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                Path tempFilePath = tempDir.resolve(tempFileName);
                
                // Write file bytes to temp location
                try (FileOutputStream fos = new FileOutputStream(tempFilePath.toFile())) {
                    fos.write(file.getBytes());
                }
                
                synchronized (fileMetadataList) {
                    fileMetadataList.add(new BatchUploadMessage.FileMetadata(
                        tempFilePath.toString(),
                        file.getOriginalFilename(),
                        file.getSize()
                    ));
                }
                
            } catch (IOException e) {
                logger.error("Failed to write temp file for {}: {}", file.getOriginalFilename(), e.getMessage());
                throw new RuntimeException(e);
            }
        });
        
        // Create batch message
        BatchUploadMessage batchMessage = new BatchUploadMessage(
            jobId,
            language,
            fileMetadataList,
            System.currentTimeMillis()
        );
        
        // Create job status
        uploadJobService.createJob(jobId, files.size());
        
        // Send ONE message to RabbitMQ
        rabbitTemplate.convertAndSend("uploadQueue", batchMessage);
        
        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("Batch upload queued for job {} in {}ms", jobId, elapsed);
        
        return jobId;
    }
    
    /**
     * Legacy method - kept for backward compatibility
     * Use sendBatchUploadMessage for better performance
     */
    @Deprecated
    public void sendUploadMessage(MultipartFile file, String language) throws IOException {
        File tempFile = File.createTempFile("upload_", file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }

        BatchUploadMessage.FileMetadata metadata = new BatchUploadMessage.FileMetadata(
            tempFile.getAbsolutePath(),
            file.getOriginalFilename(),
            file.getSize()
        );
        
        String jobId = UUID.randomUUID().toString();
        List<BatchUploadMessage.FileMetadata> files = new ArrayList<>();
        files.add(metadata);
        
        BatchUploadMessage message = new BatchUploadMessage(
            jobId,
            language,
            files,
            System.currentTimeMillis()
        );
        
        uploadJobService.createJob(jobId, 1);
        rabbitTemplate.convertAndSend("uploadQueue", message);
        tempFile.deleteOnExit();
    }
}