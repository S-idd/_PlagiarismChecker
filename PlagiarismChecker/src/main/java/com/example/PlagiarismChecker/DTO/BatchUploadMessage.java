package com.example.PlagiarismChecker.DTO;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchUploadMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String jobId;
    private String language;
    private List<FileMetadata> files;
    private long timestamp;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileMetadata implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String tempFilePath;
        private String originalFileName;
        private long fileSize;
    }
}