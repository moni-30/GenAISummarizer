package com.internal.genaidemo.controller;

import com.internal.genaidemo.service.DocumentSummarizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin
@RestController
@RequestMapping("/api/documents")
public class DocumentController {


    @Autowired
    private DocumentSummarizationService documentSummarizationService;

    @PostMapping("/summarize")
    public ResponseEntity<String> summarizeDocument(@RequestParam("file") MultipartFile file) {
        try {
            String summary = documentSummarizationService.summarizeDocument1(file);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to process document: " + e.getMessage());
        }
    }

}
