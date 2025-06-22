package com.example.PlagiarismChecker.DTO;

import java.time.LocalDateTime;

public class CodeFileDTO {
    private Long id;
    private String fileName;
    private String language;
    private LocalDateTime createdAt;

    public CodeFileDTO() {
    }

    public CodeFileDTO(Long id, String fileName, String language, LocalDateTime createdAt) {
        this.id = id;
        this.fileName = fileName;
        this.language = language;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}