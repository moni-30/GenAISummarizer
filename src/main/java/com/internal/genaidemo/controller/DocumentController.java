package com.internal.genaidemo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.internal.genaidemo.service.DocumentSummarizationService;
import com.internal.genaidemo.service.DocumentSummaryCustomiseService;

@CrossOrigin
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentSummarizationService documentSummarizationService;
    
    @Autowired
    private DocumentSummaryCustomiseService summaryService;

    @PostMapping("/summarize")
    public ResponseEntity<String> summarizeDocument(@RequestParam("file") MultipartFile file) {
        try {
            String summary = summaryService.summarizeDocument(file);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to process document: " + e.getMessage());
        }
    }

}
