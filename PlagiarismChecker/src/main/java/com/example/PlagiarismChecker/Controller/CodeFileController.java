package com.example.PlagiarismChecker.Controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import com.example.PlagiarismChecker.Service.SimilarityResult;
import com.example.PlagiarismChecker.Service.CodeFileService;
import com.example.PlagiarismChecker.model.CodeFile;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.PositiveOrZero;

@RestController
@RequestMapping("/api/code-files")
public class CodeFileController {

	@Autowired
	private CodeFileService codeFileService;

	@PostMapping("/upload")
	public ResponseEntity<?> uploadFile(@RequestParam MultipartFile file, @RequestParam String language) {
		try {
			CodeFile savedFile = codeFileService.uploadFile(file, language);
			return ResponseEntity.ok(savedFile);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to upload file: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid file type: " + e.getMessage());
		} catch (ConstraintViolationException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Validation error: " + e.getMessage());
		} catch (MaxUploadSizeExceededException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File size exceeds the maximum limit of 10MB");
		}
	}

	@GetMapping("/compare")
	public ResponseEntity<?> compareFiles(@RequestParam Long fileId1, @RequestParam Long fileId2) {
		try {
			double similarity = codeFileService.calculateSimilarity(fileId1, fileId2);
			return ResponseEntity.ok(similarity);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid comparison request: " + e.getMessage());
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Unexpected error during comparison: " + e.getMessage());
		}
	}

	@GetMapping("/compare-all/{fileId}")
	public ResponseEntity<Page<SimilarityResult>> compareAgainstAll(@PathVariable Long fileId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String languageFilter,
			@RequestParam(required = false) @PositiveOrZero Double minSimilarity) {
		Pageable pageable = PageRequest.of(page, size);
		Page<SimilarityResult> results = codeFileService.compareAgainstAll(fileId, pageable, languageFilter,
				minSimilarity);
		return ResponseEntity.ok(results);
	}

	@GetMapping("/files")
	public ResponseEntity<List<CodeFile>> getAllFiles() {
		List<CodeFile> files = codeFileService.GetAllFiles();
		return ResponseEntity.ok(files);
	}

	/**
	 * These Method ~uploadBatchFiles ,compareBatchFiles ~ Are In Testing Phase And
	 * Need to Be Tested Very Efficiently So These Are Pushed Into The TestingENV
	 * Branch
	 */

	@PostMapping("/upload/batch")
	public ResponseEntity<List<CodeFile>> uploadBatchFiles(@RequestParam("files") List<MultipartFile> files,
			@RequestParam("language") String language) throws IOException {
		List<CodeFile> savedFiles = codeFileService.uploadBatchFiles(files, language);
		return ResponseEntity.ok(savedFiles);
	}

	@PostMapping("/compare/batch")
	public ResponseEntity<List<SimilarityResult>> compareBatchFiles(@RequestBody BatchCompareRequest request) {
		List<SimilarityResult> results = codeFileService.compareBatchFiles(request.getTargetFileId(),
				request.getFileIds(), request.getLanguageFilter(), request.getMinSimilarity());
		return ResponseEntity.ok(results);
	}

	// DTO for batch comparison request
	public static class BatchCompareRequest {
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
}