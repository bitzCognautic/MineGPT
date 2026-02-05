package com.bitz.minegpt.ai;

public interface AIProvider {
    String sendMessage(String message) throws Exception;
    java.util.List<String> getAvailableModels() throws Exception;
}
