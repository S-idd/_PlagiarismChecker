package com.example.PlagiarismChecker.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;


import com.example.PlagiarismChecker.DTO.BatchUploadMessage;
import com.example.PlagiarismChecker.Repository.CodeFileRepository;
import com.example.PlagiarismChecker.model.CodeFile;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class MessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumer.class);
    private static final int BATCH_SIZE = 50; // Process 50 files at a time
    private static final int PARALLEL_THREADS = 10; // 10 parallel threads
    
    @Autowired
    private CodeFileService codeFileService;
    
    @Autowired
    private CodeFileRepository codeFileRepository;
    
    @Autowired
    private UploadJobService uploadJobService;
    
    @Autowired
    private JdbcBatchInsertService jdbcBatchInsertService;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(PARALLEL_THREADS);

    /**
     * Processes batch messages with parallel processing
     * Target: 2400 files in 30-60 seconds
     */
    @RabbitListener(queues = "uploadQueue", concurrency = "5") // 5 concurrent consumers
    public void processBatchMessage(BatchUploadMessage batchMessage) {
        String jobId = batchMessage.getJobId();
        String language = batchMessage.getLanguage();
        List<BatchUploadMessage.FileMetadata> files = batchMessage.getFiles();
        
        logger.info("Processing batch job {} with {} files", jobId, files.size());
        long startTime = System.currentTimeMillis();
        
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        
        try {
            // Split files into batches of 50
            List<List<BatchUploadMessage.FileMetadata>> batches = splitIntoBatches(files, BATCH_SIZE);
            
            for (List<BatchUploadMessage.FileMetadata> batch : batches) {
                // Process each batch in parallel
                List<CompletableFuture<CodeFile>> futures = batch.stream()
                    .map(fileMetadata -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return processFile(fileMetadata, language);
                        } catch (Exception e) {
                            logger.error("Failed to process file {}: {}", 
                                fileMetadata.getOriginalFileName(), e.getMessage());
                            failedCount.incrementAndGet();
                            return null;
                        }
                    }, executorService))
                    .collect(Collectors.toList());
                
                // Wait for batch to complete
                List<CodeFile> codeFiles = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(cf -> cf != null)
                    .collect(Collectors.toList());
                
                // Bulk insert to database
                if (!codeFiles.isEmpty()) {
                    bulkInsertCodeFiles(codeFiles);
                    processedCount.addAndGet(codeFiles.size());
                }
                
                // Update job progress
                uploadJobService.updateProgress(jobId, processedCount.get(), failedCount.get());
                
                logger.info("Job {}: Processed batch - Total: {}/{}", 
                    jobId, processedCount.get() + failedCount.get(), files.size());
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("Job {} completed in {}ms. Success: {}, Failed: {}", 
                jobId, elapsed, processedCount.get(), failedCount.get());
            
        } catch (Exception e) {
            logger.error("Fatal error processing job {}: {}", jobId, e.getMessage(), e);
            uploadJobService.markFailed(jobId, e.getMessage());
        } finally {
            // Cleanup temp files
            cleanupTempFiles(files);
        }
    }
    
    /**
     * Process single file - reads, normalizes, creates CodeFile entity
     * NO trigram generation - done lazily on first comparison
     */
    private CodeFile processFile(BatchUploadMessage.FileMetadata metadata, String language) throws IOException {
        File tempFile = new File(metadata.getTempFilePath());
        
        if (!tempFile.exists()) {
            throw new IOException("Temp file not found: " + metadata.getTempFilePath());
        }
        
        try (FileInputStream inputStream = new FileInputStream(tempFile)) {
            String content = readFileContent(inputStream);
            String normalizedContent = codeFileService.normalizeContent(content, language);
            
            if (normalizedContent.isEmpty()) {
                throw new IllegalArgumentException("Empty content after normalization");
            }
            
            // Generate content hash for duplicate detection
            String contentHash = generateContentHash(normalizedContent);
            
            // Create CodeFile entity WITHOUT trigrams (lazy generation)
            CodeFile codeFile = new CodeFile();
            codeFile.setFileName(metadata.getOriginalFileName());
            codeFile.setContent(normalizedContent);
            codeFile.setLanguage(language);
            codeFile.setCreatedAt(LocalDateTime.now());
            codeFile.setContentHash(contentHash);
            codeFile.setTrigramsGenerated(false); // Will be generated on-demand
            
            logger.debug("Processed file: {} (no trigrams yet)", metadata.getOriginalFileName());
            return codeFile;
            
        } catch (Exception e) {
            logger.error("Error processing file {}: {}", metadata.getOriginalFileName(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * Generate SHA-256 hash of content for duplicate detection
     */
    private String generateContentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Bulk insert using JDBC for maximum performance
     * 10x faster than JPA saveAll
     */
    private void bulkInsertCodeFiles(List<CodeFile> codeFiles) {
        try {
            jdbcBatchInsertService.batchInsertIgnoreDuplicates(codeFiles);
            logger.debug("JDBC bulk inserted {} files", codeFiles.size());
        } catch (Exception e) {
            logger.error("JDBC bulk insert failed, falling back to JPA: {}", e.getMessage());
            // Fallback to JPA if JDBC fails
            codeFileRepository.saveAll(codeFiles);
        }
    }
    
    /**
     * Read file content as string
     */
    private String readFileContent(FileInputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        byte[] buffer = new byte[8192];
        int bytesRead;
        
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            content.append(new String(buffer, 0, bytesRead));
        }
        
        return content.toString();
    }
    
    /**
     * Split list into batches
     */
    private <T> List<List<T>> splitIntoBatches(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }
    
    /**
     * Cleanup temporary files after processing
     */
    private void cleanupTempFiles(List<BatchUploadMessage.FileMetadata> files) {
        files.forEach(metadata -> {
            try {
                File tempFile = new File(metadata.getTempFilePath());
                if (tempFile.exists()) {
                    // Delete temp file
                    tempFile.delete();
                    
                    // Delete temp directory if empty
                    File parentDir = tempFile.getParentFile();
                    if (parentDir.exists() && parentDir.list().length == 0) {
                        parentDir.delete();
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to cleanup temp file {}: {}", 
                    metadata.getTempFilePath(), e.getMessage());
            }
        });
    }
}