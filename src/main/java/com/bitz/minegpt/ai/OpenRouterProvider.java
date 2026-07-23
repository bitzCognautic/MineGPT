package com.bitz.minegpt.ai;

import com.bitz.minegpt.config.MineGPTConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class OpenRouterProvider implements AIProvider {
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    
    @Override
    public String sendMessage(String message) throws Exception {
        MineGPTConfig config = MineGPTConfig.getInstance();
        
        // Build request JSON
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", config.openrouterModel);
        requestBody.addProperty("max_tokens", config.maxTokens);
        requestBody.addProperty("temperature", config.temperature);
        
        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", message);
        messages.add(userMessage);
        
        requestBody.add("messages", messages);
        
        // Make HTTP request
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + config.openrouterApiKey);
        conn.setRequestProperty("HTTP-Referer", "https://github.com/minegpt");
        conn.setRequestProperty("X-Title", "MineGPT");
        conn.setDoOutput(true);
        
        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // Read response
        int responseCode = conn.getResponseCode();
        BufferedReader reader;
        
        if (responseCode == 200) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }
        
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        if (responseCode != 200) {
            throw new Exception("API Error: " + response);
        }
        
        // Parse response
        JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
        return jsonResponse
            .getAsJsonArray("choices")
            .get(0)
            .getAsJsonObject()
            .getAsJsonObject("message")
            .get("content")
            .getAsString();
    }

    @Override
    public java.util.List<String> getAvailableModels() throws Exception {
        MineGPTConfig config = MineGPTConfig.getInstance();
        URL url = new URL("https://openrouter.ai/api/v1/models");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        // OpenRouter models endpoint might not require auth, but good to send if we have it?
        // Actually public list usually doesn't need auth, but let's check.
        // Documentation says GET /api/v1/models.
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            
            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
            JsonArray data = json.getAsJsonArray("data");
            
            java.util.List<String> models = new java.util.ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                models.add(data.get(i).getAsJsonObject().get("id").getAsString());
            }
             // Sort
            models.sort(String::compareTo);
            return models;
        }
    }
}
