package com.example.PlagiarismChecker.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
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
import org.springframework.web.multipart.MultipartFile;
import com.example.PlagiarismChecker.Service.SimilarityResult;
import com.example.PlagiarismChecker.DTO.CodeFileSummary;
import com.example.PlagiarismChecker.Repository.CodeFileRepository;
import com.example.PlagiarismChecker.Service.CodeFileService;
import com.example.PlagiarismChecker.Service.MessageProducer;
import com.example.PlagiarismChecker.model.CodeFile;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.PositiveOrZero;
import com.example.PlagiarismChecker.DTO.BatchCompareRequest;

@RestController
@RequestMapping("/api/code-files")
public class CodeFileController implements Serializable{

	@Autowired
	private CodeFileService codeFileService;
	
	@Autowired
	private CodeFileRepository codeFileRepository;
	
	
	
	@Autowired
    private MessageProducer producer;

	@PostMapping("/upload")
	public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam String language) {
	    try (InputStream inputStream = file.getInputStream()) {
	        CodeFile savedFile = codeFileService.uploadFileStream(inputStream, file.getOriginalFilename(), language);
	        return ResponseEntity.ok(savedFile);
	    } catch (IOException e) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to upload file: " + e.getMessage());
	    } catch (IllegalArgumentException e) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid file type or size: " + e.getMessage());
	    } catch (ConstraintViolationException e) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Validation error: " + e.getMessage());
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

//	@GetMapping("/files")
//	public ResponseEntity<List<CodeFile>> getAllFiles() {
//		List<CodeFile> files = codeFileService.GetAllFiles();
//		return ResponseEntity.ok(files);
//	}
	  @GetMapping("/files")
	    public ResponseEntity<Page<CodeFileSummary>> getAllFiles(Pageable pageable) {
//	        Page<CodeFileSummary> files = codeFileService.GetAllFilesASAP(pageable);
//	        return ResponseEntity.ok(files);
		  return ResponseEntity.ok(codeFileRepository.findAllBy(pageable));
	    }

//	@PostMapping("/upload/batch")
//    public ResponseEntity<String> uploadBatchFilesAsync(@RequestParam("files") List<MultipartFile> files,
//            @RequestParam String language) throws IOException {
//        // Validate input
//        if (files == null || files.isEmpty()) {
//            return ResponseEntity.badRequest().body("No files provided for batch upload");
//        }
//
//        // Queue each file for asynchronous processing
//        for (MultipartFile file : files) {
//            producer.sendUploadMessage(file, language);
//        }
//        
//
//        return ResponseEntity.ok("Batch upload queued successfully");
//    }
	@PostMapping("/upload/batch")
	public ResponseEntity<?> uploadBatchFilesAsync(@RequestParam("files") List<MultipartFile> files,
	                                               @RequestParam String language) throws IOException {
	    // Validate input
	    if (files == null || files.isEmpty()) {
	        return ResponseEntity.badRequest().body("No files provided for batch upload");
	    }

	    for (MultipartFile file : files) {
	        if (file.isEmpty() || file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
	            return ResponseEntity.badRequest().body("One or more files are empty or have no name");
	        }
	        if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
	            return ResponseEntity.badRequest().body("File " + file.getOriginalFilename() + " exceeds 10MB limit");
	        }
	    }

	    // Queue each valid file for async processing
	    for (MultipartFile file : files) {
	        producer.sendUploadMessage(file, language);
	    }

	    return ResponseEntity.ok("Batch upload queued successfully: " + files.size() + " files");
	}


//	@PostMapping("/compare/batch")
//	public ResponseEntity<List<SimilarityResult>> compareBatchFiles(@RequestBody BatchCompareRequest request) {
//		List<SimilarityResult> results = codeFileService.compareBatchFiles(request.getTargetFileId(),
//				request.getFileIds(), request.getLanguageFilter(), request.getMinSimilarity());
//		
//		return ResponseEntity.ok(results);
//	}
	

	// DTO for batch comparison request
	
}

