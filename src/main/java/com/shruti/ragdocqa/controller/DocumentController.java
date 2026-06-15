package com.shruti.ragdocqa.controller;

import com.shruti.ragdocqa.service.DocumentService;
import com.shruti.ragdocqa.service.QueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
public class DocumentController {

    private static final Logger log = Logger.getLogger(DocumentController.class.getName());

    private final DocumentService documentService;
    private final QueryService queryService;

    public DocumentController(DocumentService documentService, QueryService queryService) {
        this.documentService = documentService;
        this.queryService = queryService;
    }

    @PostMapping("/documents/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file) {

        try {
            log.info("Received upload request for file: " +
                    (file != null ? file.getOriginalFilename() : "null"));

            if (file == null || file.isEmpty()) {
                log.warning("Upload failed: File is empty or null");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Please upload a file", "success", false));
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
                log.warning("Upload failed: Invalid file type - " + filename);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Please upload a valid PDF file", "success", false));
            }

            int chunksStored = documentService.ingestPdf(file);

            log.info("Upload successful: " + filename + " (" + chunksStored + " chunks)");
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Document ingested successfully",
                    "fileName", filename,
                    "chunksStored", chunksStored
            ));

        } catch (Exception e) {
            log.log(Level.SEVERE, "Error during document upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to process document: " + e.getMessage()
                    ));
        }
    }

    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> query(
            @RequestBody Map<String, String> request) {

        try {
            log.info("Received query request");

            String question = request.get("question");
            if (question == null || question.isBlank()) {
                log.warning("Query failed: Question is empty");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Question cannot be empty", "success", false));
            }

            log.info("Processing question: " + question);
            String answer = queryService.answer(question);

            log.info("Query successful, answer length: " + answer.length());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "answer", answer,
                    "question", question
            ));

        } catch (Exception e) {
            log.log(Level.SEVERE, "Error during query processing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to process query: " + e.getMessage()
                    ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "rag-doc-qa",
                "timestamp", System.currentTimeMillis()
        ));
    }
}