package com.example.PlagiarismChecker.controller;

import com.example.PlagiarismChecker.DTO.BatchCompareRequest;
import com.example.PlagiarismChecker.Service.CodeFileService;
import com.example.PlagiarismChecker.Service.SimilarityResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/code-files")
public class ComparisonController {

    @Autowired
    private CodeFileService codeFileService;
    @PostMapping("/compare/batch")
    public ResponseEntity<List<SimilarityResult>> compareBatchFiles(@RequestBody BatchCompareRequest request) {
        try {
            System.out.println("Batch comparing file ID " + request.getTargetFileId() + " against files: " + request.getFileIds());
            
            List<SimilarityResult> results = codeFileService.compareBatchFiles(
                request.getTargetFileId(),
                request.getFileIds(),
                request.getLanguageFilter(),
                request.getMinSimilarity()
            );
            
            return ResponseEntity.ok(results);
        } catch (Exception ex) {
            ex.printStackTrace();  // 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(null);  
        }
    }

}

