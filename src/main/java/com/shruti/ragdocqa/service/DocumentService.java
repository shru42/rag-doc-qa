package com.shruti.ragdocqa.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class DocumentService {

    private static final Logger log = Logger.getLogger(DocumentService.class.getName());

    private final VectorStore vectorStore;

    @Value("${app.rag.chunk-size:500}")
    private int chunkSize;

    @Value("${app.rag.chunk-overlap:50}")
    private int chunkOverlap;

    public DocumentService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public int ingestPdf(MultipartFile file) {
        try {
            log.info("Starting PDF ingestion for file: " + file.getOriginalFilename() +
                    " (size: " + file.getSize() + " bytes)");
            
            // Step 1: Extract text from PDF
            String rawText = extractTextFromPdf(file);
            
            if (rawText == null || rawText.isBlank()) {
                log.warning("Extracted text is empty for file: " + file.getOriginalFilename());
                throw new RuntimeException("The PDF appears to be empty or contains no extractable text.");
            }
            
            log.info("Extracted " + rawText.length() + " characters from PDF");
            
            // Step 2: Chunk the text
            List<Document> chunks = chunkText(rawText, file.getOriginalFilename());
            
            if (chunks.isEmpty()) {
                log.warning("No chunks created from file: " + file.getOriginalFilename());
                throw new RuntimeException("Failed to create text chunks from the PDF.");
            }
            
            log.info("Created " + chunks.size() + " chunks from the document");
            
            // Step 3: Store in vector database
            try {
                vectorStore.add(chunks);
                log.info("Successfully stored " + chunks.size() + " chunks in vector store");
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to store chunks in vector database", e);
                throw new RuntimeException("Failed to store document in vector database. Please check database connection. Error: " + e.getMessage(), e);
            }
            
            log.info("Successfully ingested " + chunks.size() + " chunks from " + file.getOriginalFilename());
            return chunks.size();
            
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "Error during PDF ingestion", e);
            throw e;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Unexpected error during PDF ingestion", e);
            throw new RuntimeException("An unexpected error occurred while processing the PDF: " + e.getMessage(), e);
        }
    }

    private String extractTextFromPdf(MultipartFile file) {
        try {
            log.info("Extracting text from PDF: " + file.getOriginalFilename());
            PDDocument doc = Loader.loadPDF(file.getBytes());
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            doc.close();
            log.info("Text extraction completed");
            return text;
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to parse PDF", e);
            throw new RuntimeException("Failed to parse PDF file. The file may be corrupted or password-protected. Error: " + e.getMessage(), e);
        }
    }

    private List<Document> chunkText(String text, String sourceFileName) {
        log.info("Chunking text with chunk-size=" + chunkSize + ", overlap=" + chunkOverlap);
        List<Document> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String chunkContent = text.substring(start, end).trim();
            if (!chunkContent.isBlank()) {
                chunks.add(new Document(chunkContent, Map.of(
                        "source", sourceFileName,
                        "chunkIndex", index++
                )));
            }
            start += (chunkSize - chunkOverlap);
        }
        log.info("Created " + chunks.size() + " chunks");
        return chunks;
    }
}