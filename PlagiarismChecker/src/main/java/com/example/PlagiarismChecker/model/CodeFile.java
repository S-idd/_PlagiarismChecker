package com.example.PlagiarismChecker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;




import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "code_files")
public class CodeFile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "File name cannot be blank")
	@Column(nullable = false)
	private String fileName;

	@NotBlank(message = "Content cannot be blank")
	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	@NotBlank(message = "Language cannot be blank")
	@Column(nullable = false)
	private String language;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "trigram_vector", columnDefinition = "JSONB")
	private Map<String, Integer> trigram_vector;

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
		this.trigram_vector=trigram_vector;
	}
}