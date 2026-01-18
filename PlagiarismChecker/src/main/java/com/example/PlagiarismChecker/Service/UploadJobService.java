package com.example.PlagiarismChecker.Service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;

@Service
public class UploadJobService {
    
    private final Map<String, JobStatus> jobStatuses = new ConcurrentHashMap<>();
    
    public void createJob(String jobId, int totalFiles) {
        JobStatus status = new JobStatus();
        status.setJobId(jobId);
        status.setTotalFiles(totalFiles);
        status.setProcessedFiles(0);
        status.setFailedFiles(0);
        status.setStatus("QUEUED");
        status.setStartTime(System.currentTimeMillis());
        jobStatuses.put(jobId, status);
    }
    
    public void updateProgress(String jobId, int processed, int failed) {
        JobStatus status = jobStatuses.get(jobId);
        if (status != null) {
            status.setProcessedFiles(processed);
            status.setFailedFiles(failed);
            
            if (processed + failed >= status.getTotalFiles()) {
                status.setStatus("COMPLETED");
                status.setEndTime(System.currentTimeMillis());
            } else {
                status.setStatus("PROCESSING");
            }
        }
    }
    
    public void markFailed(String jobId, String error) {
        JobStatus status = jobStatuses.get(jobId);
        if (status != null) {
            status.setStatus("FAILED");
            status.setErrorMessage(error);
            status.setEndTime(System.currentTimeMillis());
        }
    }
    
    public JobStatus getStatus(String jobId) {
        return jobStatuses.get(jobId);
    }
    
    public void cleanupOldJobs(long olderThanMillis) {
        long cutoff = System.currentTimeMillis() - olderThanMillis;
        jobStatuses.entrySet().removeIf(entry -> 
            entry.getValue().getEndTime() != null && 
            entry.getValue().getEndTime() < cutoff
        );
    }
    
    @Data
    public static class JobStatus {
        private String jobId;
        private int totalFiles;
        private int processedFiles;
        private int failedFiles;
        private String status; // QUEUED, PROCESSING, COMPLETED, FAILED
        private long startTime;
        private Long endTime;
        private String errorMessage;
        
        public double getProgress() {
            if (totalFiles == 0) return 0.0;
            return (double) (processedFiles + failedFiles) / totalFiles * 100.0;
        }
    }
}