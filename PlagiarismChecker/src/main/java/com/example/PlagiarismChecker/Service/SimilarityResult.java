package com.example.PlagiarismChecker.Service;

import java.io.Serializable;

public class SimilarityResult implements Serializable{
    private Long fileId;
    private String fileName;
    private String Language;
    private double similarity;

    public SimilarityResult(Long fileId, String fileName, String Language,double similarity) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.Language=Language;
        this.similarity = similarity;
    }

    public Long getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public double getSimilarity() {
        return similarity;
    }
    
    public String getLanguage() {
    	return Language;
    }
    
    public void setLanguage(String Language) {
    	this.Language=Language;
    }
   
    public void setfileId(Long fileId) {
    	this.fileId=fileId;
    }
    
    public void setfileName(String fileName) {
    	this.fileName=fileName;
    }
    
    public void setSimilarity(double similarity) {
    	this.similarity=similarity;
    }
}
