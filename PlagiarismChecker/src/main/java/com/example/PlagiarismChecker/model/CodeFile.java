package com.example.PlagiarismChecker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "code_files", 
    indexes = {
        @Index(name = "idx_language", columnList = "language"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_content_hash", columnList = "content_hash"),
        @Index(name = "idx_language_created", columnList = "language,created_at")
    }
)
public class CodeFile implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * CRITICAL: Changed from IDENTITY to SEQUENCE for batch insert support
     * IDENTITY disables Hibernate batch processing
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "code_file_seq")
    @SequenceGenerator(name = "code_file_seq", sequenceName = "code_file_sequence", allocationSize = 50)
    private Long id;

    @NotBlank(message = "File name cannot be blank")
    @Column(nullable = false, length = 500)
    private String fileName;

    @NotBlank(message = "Content cannot be blank")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @NotBlank(message = "Language cannot be blank")
    @Column(nullable = false, length = 50)
    private String language;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /**
     * Trigram vector stored as JSONB for efficient storage
     * Initially NULL, populated by async job after upload
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigram_vector", columnDefinition = "JSONB")
    private Map<String, Integer> trigram_vector;

    /**
     * Content hash for duplicate detection
     * Not unique to allow re-uploads if needed
     */
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    /**
     * Flag to track if trigrams have been generated
     */
    @Column(name = "trigrams_generated", nullable = false)
    private boolean trigramsGenerated = false;

    // Constructors
    public CodeFile() {
        this.createdAt = LocalDateTime.now();
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Map<String, Integer> Gettrigram_vector() {
        return trigram_vector;
    }

    public void Settrigram_vector(Map<String, Integer> trigram_vector) {
        this.trigram_vector = trigram_vector;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public boolean getTrigramsGenerated() {
        return trigramsGenerated;
    }

    public void setTrigramsGenerated(boolean trigramsGenerated) {
        this.trigramsGenerated = trigramsGenerated;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}