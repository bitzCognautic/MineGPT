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

public class GoogleProvider implements AIProvider {
    private static final String API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    
    @Override
    public String sendMessage(String message) throws Exception {
        MineGPTConfig config = MineGPTConfig.getInstance();
        
        // Build request JSON
        JsonObject requestBody = new JsonObject();
        
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", message);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        
        requestBody.add("contents", contents);
        
        // Generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", config.temperature);
        generationConfig.addProperty("maxOutputTokens", config.maxTokens);
        requestBody.add("generationConfig", generationConfig);
        
        // Make HTTP request
        String urlString = String.format(API_URL_TEMPLATE, config.googleModel, config.googleApiKey);
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
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
            .getAsJsonArray("candidates")
            .get(0)
            .getAsJsonObject()
            .getAsJsonObject("content")
            .getAsJsonArray("parts")
            .get(0)
            .getAsJsonObject()
            .get("text")
            .getAsString();
    }

    @Override
    public java.util.List<String> getAvailableModels() throws Exception {
        MineGPTConfig config = MineGPTConfig.getInstance();
        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models?key=" + config.googleApiKey);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            
            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
            JsonArray modelsJson = json.getAsJsonArray("models");
            
            java.util.List<String> models = new java.util.ArrayList<>();
            for (int i = 0; i < modelsJson.size(); i++) {
                String name = modelsJson.get(i).getAsJsonObject().get("name").getAsString();
                // Google api returns "models/gemini-pro", strip prefix if desired or keep
                models.add(name.replace("models/", ""));
            }
            return models;
        }
    }
}
