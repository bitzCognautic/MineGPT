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

public class AnthropicProvider implements AIProvider {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    
    @Override
    public String sendMessage(String message) throws Exception {
        MineGPTConfig config = MineGPTConfig.getInstance();
        
        // Build request JSON
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", config.anthropicModel);
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
        conn.setRequestProperty("x-api-key", config.anthropicApiKey);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
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
            .getAsJsonArray("content")
            .get(0)
            .getAsJsonObject()
            .get("text")
            .getAsString();
    }

    @Override
    public java.util.List<String> getAvailableModels() throws Exception {
        MineGPTConfig config = MineGPTConfig.getInstance();
        URL url = new URL("https://api.anthropic.com/v1/models");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("x-api-key", config.anthropicApiKey);
        conn.setRequestProperty("anthropic-version", "2023-06-01"); // Required header
        
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
            return models;
        }
    }
}
