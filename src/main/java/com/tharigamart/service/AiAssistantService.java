package com.tharigamart.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tharigamart.dto.AiReply;
import com.tharigamart.dto.ProductDTO;
import com.tharigamart.dto.ProductPageResponse;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI shopping assistant. Reads the OpenAI API key from the OPENAI_API_KEY
 * environment variable (never from the frontend) and answers product
 * questions while recommending catalogue products. If the key is not set it
 * gracefully falls back to keyword-based recommendations, so the assistant
 * always works.
 */
@Service
public class AiAssistantService {

    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";
    private static final int MAX_CONTEXT_PRODUCTS = 200;
    private static final int MAX_RECOMMENDATIONS = 6;

    private final ProductService productService;
    private final ObjectMapper objectMapper;

    public AiAssistantService(ProductService productService, ObjectMapper objectMapper) {
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    public AiReply chat(String rawMessage) {
        String message = rawMessage == null ? "" : rawMessage.trim();
        List<ProductDTO> active = productService.listActive();
        List<ProductDTO> recommendations = recommend(message, active);

        String apiKey = System.getenv("OPENAI_API_KEY");
        boolean aiEnabled = apiKey != null && !apiKey.isBlank();
        String reply = aiEnabled ? askOpenAi(apiKey, message, active)
                : fallbackReply(recommendations);
        return new AiReply(reply, recommendations, aiEnabled);
    }

    /** Keyword-based product recommendation derived from the user's message. */
    private List<ProductDTO> recommend(String message, List<ProductDTO> active) {
        Set<Long> seen = new LinkedHashSet<>();
        List<ProductDTO> results = new ArrayList<>();
        for (String keyword : extractKeywords(message)) {
            try {
                ProductPageResponse page = productService.search(keyword, null, null, null, "newest", 0, 10);
                if (page != null) {
                    for (ProductDTO p : page.getContent()) {
                        if (seen.add(p.getId())) {
                            results.add(p);
                        }
                    }
                }
            } catch (Exception ignored) {
                // a failed keyword search must never break the assistant
            }
            if (results.size() >= MAX_RECOMMENDATIONS) {
                break;
            }
        }
        if (results.isEmpty()) {
            // No keyword hit: fall back to featured, then newest products.
            results = active.stream()
                    .filter(ProductDTO::isFeatured)
                    .limit(MAX_RECOMMENDATIONS)
                    .collect(Collectors.toList());
            if (results.isEmpty()) {
                results = active.stream().limit(MAX_RECOMMENDATIONS).collect(Collectors.toList());
            }
        }
        return results.stream().limit(MAX_RECOMMENDATIONS).collect(Collectors.toList());
    }

    private List<String> extractKeywords(String message) {
        Set<String> stop = Set.of(
                "the", "a", "an", "and", "or", "of", "for", "to", "in", "on", "at", "is", "are",
                "was", "were", "what", "which", "who", "do", "does", "did", "i", "you", "my", "your",
                "me", "with", "that", "this", "any", "some", "best", "good", "please", "recommend",
                "suggest", "show", "want", "need", "have", "can", "could", "would", "like", "buy",
                "get", "about", "there", "very", "much", "many");
        List<String> out = new ArrayList<>();
        for (String word : message.toLowerCase().split("[^a-z]+")) {
            if (word.length() >= 3 && !stop.contains(word) && !out.contains(word)) {
                out.add(word);
            }
        }
        return out;
    }

    private String askOpenAi(String apiKey, String message, List<ProductDTO> active) {
        try {
            String catalog = active.stream()
                    .limit(MAX_CONTEXT_PRODUCTS)
                    .map(p -> "- " + p.getName() + " | " + nvl(p.getCategoryName())
                            + " | INR " + price(p) + " | in stock: " + p.getStock())
                    .collect(Collectors.joining("\n"));

            String systemPrompt = "You are the friendly THARIGA MART AI shopping assistant. "
                    + "The store sells stationery: pens, pencils, notebooks, erasers, rulers, "
                    + "markers, school bags, geometry boxes and art supplies. Answer the customer's "
                    + "question helpfully and recommend specific products from the catalogue below. "
                    + "When recommending, mention the product name and price. Keep answers concise "
                    + "(2-4 sentences). If asked about something unrelated to stationery, politely "
                    + "steer the conversation back to stationery.\n\nCURRENT CATALOGUE:\n" + catalog;

            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", MODEL);
            ArrayNode messages = root.putArray("messages");
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", systemPrompt);
            ObjectNode user = messages.addObject();
            user.put("role", "user");
            user.put("content", message.isBlank() ? "Recommend some stationery products" : message);
            root.put("temperature", 0.7);
            root.put("max_tokens", 600);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_CHAT_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode content = objectMapper.readTree(response.body())
                        .path("choices").path(0).path("message").path("content");
                if (!content.isMissingNode() && !content.asText().isBlank()) {
                    return content.asText().trim();
                }
            }
            return fallbackReply(recommend(message, active))
                    + " (The AI service returned an error, so I used the in-store picks.)";
        } catch (Exception e) {
            // Network, timeout or parse error - never fail the whole request.
            return "I'm sorry, the AI assistant is temporarily unavailable right now. "
                    + "Meanwhile, here are some popular stationery picks: "
                    + recommend(message, active).stream()
                    .map(ProductDTO::getName)
                    .limit(3)
                    .collect(Collectors.joining(", ")) + ".";
        }
    }

    private String fallbackReply(List<ProductDTO> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "I'd love to help! Browse our stationery catalogue and tell me what you're looking for.";
        }
        String names = recommendations.stream()
                .map(p -> p.getName() + " (" + price(p) + ")")
                .collect(Collectors.joining(", "));
        return "Here are some stationery picks I found for you: " + names
                + ". Set the OPENAI_API_KEY environment variable and restart the app for full AI-powered answers.";
    }

    private String price(ProductDTO p) {
        return p.getPrice() == null ? "0" : p.getPrice().toPlainString();
    }

    private String nvl(String value) {
        return value == null ? "Stationery" : value;
    }
}
