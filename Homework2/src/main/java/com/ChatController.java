package com;

import org.apache.jena.rdf.model.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<String> documentCache = new ArrayList<>();
    private final List<double[]> vectorCache = new ArrayList<>();
    private boolean isDatabaseLoaded = false;

    @PostMapping("/ask")
    public ResponseEntity<?> askChatbot(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");

        if (userMessage == null || userMessage.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Message cannot be empty."));
        }

        try {
            loadKnowledgeBaseIfEmpty();

            double[] questionVector = getEmbedding(userMessage);

            String retrievedContext = findMostSimilarFacts(questionVector, 6);

            String systemPrompt = "You are a helpful assistant. Use ONLY the following facts to answer the user's question. "
                    + "If the answer cannot be explicitly found in the facts, say 'I don't know.'\n\n"
                    + "FACTS:\n" + retrievedContext;

            System.out.println("\n========== WHAT THE MODEL SEES ==========");
            System.out.println("USER ASKED: " + userMessage);
            System.out.println("SYSTEM PROMPT:\n" + systemPrompt);
            System.out.println("=========================================\n");

            Map<String, Object> ollamaRequest = Map.of(
                    "model", "llama3.2:3b",
                    "prompt", userMessage,
                    "system", systemPrompt,
                    "stream", false
            );

            String requestBody = objectMapper.writeValueAsString(ollamaRequest);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> jsonResponse = objectMapper.readValue(response.body(), Map.class);
            String aiText = (String) jsonResponse.get("response");

            return ResponseEntity.ok(Map.of("status", "success", "reply", aiText));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private double[] getEmbedding(String text) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "model", "nomic-embed-text",
                "input", text
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/embed"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> jsonResponse = objectMapper.readValue(response.body(), Map.class);

        List<List<Number>> embeddings = (List<List<Number>>) jsonResponse.get("embeddings");
        List<Number> vector = embeddings.get(0);

        double[] result = new double[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            result[i] = vector.get(i).doubleValue();
        }
        return result;
    }

    private synchronized void loadKnowledgeBaseIfEmpty() {
        if (isDatabaseLoaded) return;

        try {
            Path path = Paths.get("book-scenario.rdf");
            if (!Files.exists(path)) {
                System.out.println("book-scenario.rdf not found.");
                isDatabaseLoaded = true;
                return;
            }

            Model model = ModelFactory.createDefaultModel();
            try (InputStream in = new FileInputStream(path.toFile())) {
                model.read(in, null, "RDF/XML");
            }

            StmtIterator iter = model.listStatements();
            while (iter.hasNext()) {
                Statement stmt = iter.nextStatement();

                String subjectName = stmt.getSubject().getLocalName();
                String predicateName = stmt.getPredicate().getLocalName();
                RDFNode objectNode = stmt.getObject();

                if (subjectName == null) subjectName = stmt.getSubject().toString();
                if (predicateName == null) predicateName = stmt.getPredicate().toString();

                String objectName;
                if (objectNode.isLiteral()) {
                    objectName = objectNode.asLiteral().getString();
                } else {
                    objectName = objectNode.asResource().getLocalName();
                    if (objectName == null) objectName = objectNode.toString();
                }

                if ("type".equals(predicateName)) continue;
                String fact;
                if ("hasTheme".equals(predicateName)) {
                    fact = String.format("The book '%s' features the theme of %s.", subjectName, objectName);
                } else if ("suitableForLevel".equals(predicateName)) {
                    fact = String.format("The book '%s' is suitable for the reading level: %s.", subjectName, objectName);
                } else if ("hasReadingLevel".equals(predicateName)) {
                    fact = String.format("The user '%s' has an %s reading level.", subjectName, objectName);
                } else if ("prefersTheme".equals(predicateName)) {
                    fact = String.format("The user '%s' prefers to read the %s theme.", subjectName, objectName);
                } else if ("author".equals(predicateName)) {
                    fact = String.format("The book '%s' was written by the author %s.", subjectName, objectName);
                } else {
                    fact = String.format("The relationship between '%s' and '%s' is '%s'.", subjectName, objectName, predicateName);
                }

                double[] vector = getEmbedding(fact);
                documentCache.add(fact);
                vectorCache.add(vector);
            }

            System.out.println("Successfully vectorized " + documentCache.size() + " highly semantic facts!");
            isDatabaseLoaded = true;

        } catch (Exception e) {
            System.err.println("Failed to vectorize knowledge base: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String findMostSimilarFacts(double[] questionVector, int topK) {
        if (vectorCache.isEmpty()) return "No custom facts available.";

        Map<String, Double> scores = new HashMap<>();
        for (int i = 0; i < vectorCache.size(); i++) {
            double score = cosineSimilarity(questionVector, vectorCache.get(i));
            scores.put(documentCache.get(i), score);
        }

        return scores.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining("\n"));
    }

    private double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}