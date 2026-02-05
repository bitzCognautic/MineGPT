package com.bitz.minegpt.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class MineGPTConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("minegpt.json");
    
    // API Keys
    public String openaiApiKey = "";
    public String anthropicApiKey = "";
    public String googleApiKey = "";
    public String openrouterApiKey = "";
    
    // Selected provider
    public String selectedProvider = "openai"; // openai, anthropic, google, openrouter
    
    // Model settings
    public String openaiModel = "gpt-4";
    public String anthropicModel = "claude-3-5-sonnet-20241022";
    public String googleModel = "gemini-2.5-flash";
    public String openrouterModel = "anthropic/claude-3.5-sonnet";
    
    // Chat settings
    public int maxTokens = 1000;
    public double temperature = 0.7;
    
    private static MineGPTConfig instance;
    
    public static MineGPTConfig getInstance() {
        if (instance == null) {
            instance = new MineGPTConfig();
        }
        return instance;
    }
    
    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    instance = GSON.fromJson(reader, MineGPTConfig.class);
                    System.out.println("[MineGPT] Configuration loaded successfully");
                }
            } else {
                instance = new MineGPTConfig();
                save();
                System.out.println("[MineGPT] Created new configuration file");
            }
        } catch (Exception e) {
            System.err.println("[MineGPT] Failed to load configuration: " + e.getMessage());
            instance = new MineGPTConfig();
        }
    }
    
    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(instance, writer);
                System.out.println("[MineGPT] Configuration saved successfully");
            }
        } catch (Exception e) {
            System.err.println("[MineGPT] Failed to save configuration: " + e.getMessage());
        }
    }
    
    public String getCurrentApiKey() {
        return switch (selectedProvider) {
            case "openai" -> openaiApiKey;
            case "anthropic" -> anthropicApiKey;
            case "google" -> googleApiKey;
            case "openrouter" -> openrouterApiKey;
            default -> "";
        };
    }
    
    public String getCurrentModel() {
        return switch (selectedProvider) {
            case "openai" -> openaiModel;
            case "anthropic" -> anthropicModel;
            case "google" -> googleModel;
            case "openrouter" -> openrouterModel;
            default -> "";
        };
    }
}
