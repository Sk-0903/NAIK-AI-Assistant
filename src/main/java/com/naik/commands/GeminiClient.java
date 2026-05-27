package com.naik.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

public class GeminiClient {
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private GeminiClient() {
    }

    public static class GeminiResponse {
        private final String assistantResponse;
        private final String action;
        private final String actionTarget;

        public GeminiResponse(String assistantResponse, String action, String actionTarget) {
            this.assistantResponse = assistantResponse;
            this.action = action;
            this.actionTarget = actionTarget;
        }

        public String getAssistantResponse() {
            return assistantResponse;
        }

        public String getAction() {
            return action;
        }

        public String getActionTarget() {
            return actionTarget;
        }
    }

    public static GeminiResponse generateResponse(String prompt) {
        String apiKey = loadApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return null; // Fall back to local rule-based matching
        }

        String model = loadModel();
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        String jsonBody = buildRequestBody(prompt);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String responseBody = response.body();

                // Parse structured JSON response inside the text field
                String textJson = JsonParser.getJsonStringValue(responseBody, "text");
                if (textJson != null) {
                    String assistantResponse = JsonParser.getJsonStringValue(textJson, "assistantResponse");
                    String action = JsonParser.getJsonStringValue(textJson, "action");
                    String actionTarget = JsonParser.getJsonStringValue(textJson, "actionTarget");

                    if (assistantResponse != null) {
                        return new GeminiResponse(
                                assistantResponse,
                                action != null ? action.toUpperCase() : "NONE",
                                actionTarget
                        );
                    }
                }

                // If not structured, extract whatever plain text is in the text field
                String plainText = JsonParser.getJsonStringValue(responseBody, "text");
                if (plainText != null && !plainText.isBlank()) {
                    return new GeminiResponse(plainText, "NONE", null);
                }
            } else {
                System.err.println("Gemini API call returned status: " + response.statusCode() + " - " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Network error connecting to Gemini API: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
        }

        return null; // Fall back to local rule-based matching
    }

    private static String loadApiKey() {
        String key = System.getenv("GEMINI_API_KEY");
        if (key != null && !key.isBlank()) {
            return key;
        }

        File file = new File("config.properties");
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(fis);
                String fileKey = props.getProperty("gemini.api.key");
                if (fileKey != null && !fileKey.isBlank()) {
                    return fileKey;
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private static String loadModel() {
        String model = System.getenv("GEMINI_MODEL");
        if (model != null && !model.isBlank()) {
            return model;
        }

        File file = new File("config.properties");
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(fis);
                String fileModel = props.getProperty("gemini.model");
                if (fileModel != null && !fileModel.isBlank()) {
                    return fileModel;
                }
            } catch (IOException ignored) {
            }
        }
        return "gemini-1.5-flash";
    }

    private static String buildRequestBody(String prompt) {
        String escapedPrompt = escapeJsonString(prompt);

        return "{"
                + "\"contents\": [{"
                + "  \"parts\": [{\"text\": \"" + escapedPrompt + "\"}]"
                + "}],"
                + "\"generationConfig\": {"
                + "  \"responseMimeType\": \"application/json\","
                + "  \"responseSchema\": {"
                + "    \"type\": \"OBJECT\","
                + "    \"properties\": {"
                + "      \"assistantResponse\": {"
                + "        \"type\": \"STRING\","
                + "        \"description\": \"Friendly, clear, and concise conversational response to say out loud to the user.\""
                + "      },"
                + "      \"action\": {"
                + "        \"type\": \"STRING\","
                + "        \"enum\": [\"OPEN_APP\", \"OPEN_URL\", \"SEARCH_WEB\", \"SEARCH_YOUTUBE\", \"SEARCH_WIKIPEDIA\", \"NONE\"],"
                + "        \"description\": \"Choose OPEN_APP if the user explicitly asks to open or launch a local program/app/software. Choose OPEN_URL if they want to visit a web domain (e.g. youtube.com). Choose SEARCH_WEB if the query requires detailed browsing, search results, or real-time web information (e.g., looking up shops, reviews, news, current events). For simple factual questions, math, greeting, jokes, or basic explanations that can be answered directly, choose NONE and provide the answer directly in assistantResponse.\""
                + "      },"
                + "      \"actionTarget\": {"
                + "        \"type\": \"STRING\","
                + "        \"description\": \"The argument for the selected action (e.g. clean app name, website domain, search query).\""
                + "      }"
                + "    },"
                + "    \"required\": [\"assistantResponse\", \"action\"]"
                + "  }"
                + "},"
                + "\"systemInstruction\": {"
                + "  \"parts\": [{\"text\": \"You are NAIK, a highly active, reactive, intelligent voice assistant for the user Keshav on his Windows desktop. For simple factual questions, greeting, math, jokes, or basic explanations, select 'NONE' for action and answer directly inside 'assistantResponse'. For queries that require browsing, real-time web results, local recommendations, or when the user explicitly requests to search/google something, select 'SEARCH_WEB' for action and set the search query as actionTarget. When he asks to open an app (e.g. notepad, calculator, visual studio code, chrome, instagram, whatsapp), select 'OPEN_APP' and set the clean app name as actionTarget.\"}]"
                + "}"
                + "}";
    }

    private static String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
