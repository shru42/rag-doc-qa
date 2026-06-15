package com.shruti.ragdocqa.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class QueryService {

    private static final Logger log = Logger.getLogger(QueryService.class.getName());

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    @Value("${app.rag.top-k:5}")
    private int topK;

    public QueryService(VectorStore vectorStore, ChatClient chatClient) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
    }

    @Cacheable(value = "queryCache", key = "#question")
    public String answer(String question) {
        log.info("Cache miss — running RAG pipeline for: " + question);

        try {
            // Step 1: Search for relevant documents
            log.info("Searching for relevant chunks with topK=" + topK);
            List<Document> relevantChunks = vectorStore.similaritySearch(
                    SearchRequest.builder().query(question).topK(topK).build()
            );

            log.info("Found " + relevantChunks.size() + " relevant chunks");

            if (relevantChunks.isEmpty()) {
                log.warning("No relevant chunks found for question: " + question);
                return "I couldn't find relevant information in the uploaded documents. Please make sure you have uploaded documents first.";
            }

            // Step 2: Build context from chunks
            String context = relevantChunks.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n---\n\n"));

            log.info("Context built with " + context.length() + " characters");

            // Step 3: Call AI model
            log.info("Calling AI model to generate answer...");
            String answer = null;
            
            try {
                answer = chatClient.prompt()
                        .user(buildPrompt(question, context))
                        .call()
                        .content();
            } catch (Exception aiException) {
                log.log(Level.SEVERE, "AI model call failed", aiException);
                throw new RuntimeException("Failed to generate answer from AI model. Please check your API key and configuration. Error: " + aiException.getMessage(), aiException);
            }

            if (answer == null || answer.isBlank()) {
                log.warning("AI model returned empty response");
                throw new RuntimeException("AI model returned an empty response. Please check your API configuration.");
            }

            log.info("Answer generated successfully for: " + question);
            return answer;

        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "Error in RAG pipeline for question: " + question, e);
            throw e;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Unexpected error in RAG pipeline", e);
            throw new RuntimeException("An unexpected error occurred while processing your question: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String question, String context) {
        return """
                You are a helpful assistant that answers questions strictly based on the provided context.
                If the answer is not in the context, say "I don't have enough information to answer that."
                Do not make up information.

                Context:
                %s

                Question: %s

                Answer:
                """.formatted(context, question);
    }
}