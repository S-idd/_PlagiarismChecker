package com.example.PlagiarismChecker.Service;

import java.io.Serializable;

public class SimilarityResult implements Serializable {
    private Long fileId;
    private String fileName;
    private String language;
    private double similarity;

    public SimilarityResult(Long fileId, String fileName, String language, double similarity) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.language = language;
        this.similarity = similarity;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
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

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }
}
