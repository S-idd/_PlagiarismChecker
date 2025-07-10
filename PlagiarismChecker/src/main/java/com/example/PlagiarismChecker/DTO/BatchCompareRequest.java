package com.example.PlagiarismChecker.DTO;

import java.util.List;


public  class BatchCompareRequest {
	private Long targetFileId;
	private List<Long> fileIds;
	private String languageFilter;
	private Double minSimilarity;

	public Long getTargetFileId() {
		return targetFileId;
	}

	public void setTargetFileId(Long targetFileId) {
		this.targetFileId = targetFileId;
	}

	public List<Long> getFileIds() {
		return fileIds;
	}

	public void setFileIds(List<Long> fileIds) {
		this.fileIds = fileIds;
	}

	public String getLanguageFilter() {
		return languageFilter;
	}

	public void setLanguageFilter(String languageFilter) {
		this.languageFilter = languageFilter;
	}

	public Double getMinSimilarity() {
		return minSimilarity;
	}

	public void setMinSimilarity(Double minSimilarity) {
		this.minSimilarity = minSimilarity;
	}
}