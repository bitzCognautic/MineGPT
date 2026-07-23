package com.bitz.minegpt.gui;

import com.bitz.minegpt.config.MineGPTConfig;
import com.bitz.minegpt.ai.AIProvider;
import com.bitz.minegpt.ai.OpenAIProvider;
import com.bitz.minegpt.ai.AnthropicProvider;
import com.bitz.minegpt.ai.GoogleProvider;
import com.bitz.minegpt.ai.OpenRouterProvider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AIChatScreen extends Screen {
    private TextFieldWidget chatInput;
    private ButtonWidget sendButton;
    private ButtonWidget closeButton;
    private ButtonWidget settingsButton;
    private ButtonWidget clearButton;
    private ButtonWidget modelDropdownButton;
    
    // Settings widgets
    private TextFieldWidget openaiKeyField;
    private TextFieldWidget anthropicKeyField;
    private TextFieldWidget googleKeyField;
    private TextFieldWidget openrouterKeyField;
    private ButtonWidget providerButton;
    private ButtonWidget saveSettingsButton;
    private ButtonWidget cancelSettingsButton; // To close settings without saving or just back
    
    private List<ChatMessage> messages = new ArrayList<>();
    private boolean showSettings = false;
    private boolean showModelDropdown = false;
    
    private AIProvider currentProvider;
    private boolean isWaitingForResponse = false;
    
    private List<String> availableModels = new ArrayList<>();
    private int modelScrollOffset = 0;
    private int chatScrollOffset = 0;
    
    public AIChatScreen() {
        super(Text.literal("MineGPT - AI Chat"));
        initializeProvider();
    }
    
    private void initializeProvider() {
        MineGPTConfig config = MineGPTConfig.getInstance();
        currentProvider = switch (config.selectedProvider) {
            case "anthropic" -> new AnthropicProvider();
            case "google" -> new GoogleProvider();
            case "openrouter" -> new OpenRouterProvider();
            default -> new OpenAIProvider();
        };
        // Reset models when provider changes as they are provider-specific
        availableModels.clear();
        availableModels.add(config.getCurrentModel()); // Ensure current is in list
    }
    
    @Override
    protected void init() {
        super.init();
        
        int bottomY = this.height - 40;
        int centerX = this.width / 2;
        
        // Chat Input: [ Input ............ ] [Model] [Clear] [Send]
        // Left align input
        int inputWidth = this.width - 350;
        if (inputWidth < 100) inputWidth = 100;
        
        chatInput = new TextFieldWidget(this.textRenderer, 20, bottomY, inputWidth, 20, Text.literal("Message"));
        chatInput.setMaxLength(2000);
        chatInput.setPlaceholder(Text.literal("Ask AI..."));
        this.addDrawableChild(chatInput);
        this.setInitialFocus(chatInput);
        
        int currentX = 20 + inputWidth + 5;
        
        // Model Dropdown Button
        modelDropdownButton = ButtonWidget.builder(
            Text.literal(abbreviate(MineGPTConfig.getInstance().getCurrentModel(), 15)),
            button -> toggleModelDropdown()
        ).dimensions(currentX, bottomY, 100, 20).build();
        this.addDrawableChild(modelDropdownButton);
        currentX += 105;
        
        // Clear Button
        clearButton = ButtonWidget.builder(
            Text.literal("Clear"),
            button -> {
                chatInput.setText("");
                messages.clear();
            }
        ).dimensions(currentX, bottomY, 50, 20).build();
        this.addDrawableChild(clearButton);
        currentX += 55;
        
        // Send Button
        sendButton = ButtonWidget.builder(
            Text.literal("Send"),
            button -> sendMessage()
        ).dimensions(currentX, bottomY, 50, 20).build();
        this.addDrawableChild(sendButton);
        currentX += 55;
        
        // Settings Button (Top Right)
        settingsButton = ButtonWidget.builder(
            Text.literal("⚙"),
            button -> toggleSettings()
        ).dimensions(this.width - 30, 10, 20, 20).build();
        this.addDrawableChild(settingsButton);
        
        // Close Button (Top Left)
        closeButton = ButtonWidget.builder(
            Text.literal("X"),
            button -> this.close()
        ).dimensions(10, 10, 20, 20).build();
        this.addDrawableChild(closeButton);
        
        // Settings Widgets (initially hidden)
        initSettingsWidgets();
        
        updateWidgetVisibility();
    }
    
    private String abbreviate(String str, int len) {
        if (str == null) return "";
        if (str.length() <= len) return str;
        return str.substring(0, len - 3) + "...";
    }
    
    private void initSettingsWidgets() {
        MineGPTConfig config = MineGPTConfig.getInstance();
        int settingsX = this.width / 2 - 150;
        int startY = 50;
        
        providerButton = ButtonWidget.builder(
            Text.literal("Provider: " + config.selectedProvider.toUpperCase()),
            button -> cycleProvider()
        ).dimensions(settingsX, startY, 300, 20).build();
        this.addDrawableChild(providerButton);
        
        int gap = 35;
        
        openaiKeyField = new TextFieldWidget(this.textRenderer, settingsX, startY + gap, 300, 20, Text.literal("OpenAI Key"));
        openaiKeyField.setMaxLength(200);
        openaiKeyField.setText(config.openaiApiKey);
        openaiKeyField.setPlaceholder(Text.literal("OpenAI API Key (sk-...)"));
        this.addDrawableChild(openaiKeyField);
        
        anthropicKeyField = new TextFieldWidget(this.textRenderer, settingsX, startY + gap * 2, 300, 20, Text.literal("Anthropic Key"));
        anthropicKeyField.setMaxLength(200);
        anthropicKeyField.setText(config.anthropicApiKey);
        anthropicKeyField.setPlaceholder(Text.literal("Anthropic API Key (sk-ant-...)"));
        this.addDrawableChild(anthropicKeyField);
        
        googleKeyField = new TextFieldWidget(this.textRenderer, settingsX, startY + gap * 3, 300, 20, Text.literal("Google Key"));
        googleKeyField.setMaxLength(200);
        googleKeyField.setText(config.googleApiKey);
        googleKeyField.setPlaceholder(Text.literal("Google API Key (AIza...)"));
        this.addDrawableChild(googleKeyField);
        
        openrouterKeyField = new TextFieldWidget(this.textRenderer, settingsX, startY + gap * 4, 300, 20, Text.literal("OpenRouter Key"));
        openrouterKeyField.setMaxLength(200);
        openrouterKeyField.setText(config.openrouterApiKey);
        openrouterKeyField.setPlaceholder(Text.literal("OpenRouter API Key (sk-or-...)"));
        this.addDrawableChild(openrouterKeyField);
        
        saveSettingsButton = ButtonWidget.builder(
            Text.literal("Save & Close Settings"),
            button -> {
                saveSettings();
                toggleSettings();
            }
        ).dimensions(settingsX, startY + gap * 5 + 10, 300, 20).build();
        this.addDrawableChild(saveSettingsButton);
    }
    
    private void updateWidgetVisibility() {
        boolean chatVisible = !showSettings && !showModelDropdown; // Hide chat inputs when dropdown is open? No, overlay.
        chatVisible = !showSettings;
        
        chatInput.visible = chatVisible;
        sendButton.visible = chatVisible;
        clearButton.visible = chatVisible;
        modelDropdownButton.visible = chatVisible;
        closeButton.visible = true; // Always visible
        settingsButton.visible = !showSettings; // Hide gear when in settings
        
        // Settings visibility
        providerButton.visible = showSettings;
        openaiKeyField.visible = showSettings;
        anthropicKeyField.visible = showSettings;
        googleKeyField.visible = showSettings;
        openrouterKeyField.visible = showSettings;
        saveSettingsButton.visible = showSettings;
    }
    
    private void cycleProvider() {
        MineGPTConfig config = MineGPTConfig.getInstance();
        config.selectedProvider = switch (config.selectedProvider) {
            case "openai" -> "anthropic";
            case "anthropic" -> "google";
            case "google" -> "openrouter";
            case "openrouter" -> "openai";
            default -> "openai";
        };
        providerButton.setMessage(Text.literal("Provider: " + config.selectedProvider.toUpperCase()));
        initializeProvider();
    }
    
    private void toggleSettings() {
        showSettings = !showSettings;
        if (showModelDropdown) showModelDropdown = false;
        updateWidgetVisibility();
        if (showSettings) {
            // Refocus first field
            this.setFocused(openaiKeyField);
        } else {
            this.setFocused(chatInput);
        }
    }
    
    private void saveSettings() {
        MineGPTConfig config = MineGPTConfig.getInstance();
        config.openaiApiKey = openaiKeyField.getText();
        config.anthropicApiKey = anthropicKeyField.getText();
        config.googleApiKey = googleKeyField.getText();
        config.openrouterApiKey = openrouterKeyField.getText();
        MineGPTConfig.save();
    }
    
    // --- Model Dropdown Logic ---
    
    private void toggleModelDropdown() {
        if (showSettings) return;
        showModelDropdown = !showModelDropdown;
        
        if (showModelDropdown) {
            fetchModels();
        }
    }
    
    private void fetchModels() {
        if (isWaitingForResponse) return; // Don't fetch if busy? Actually fetching models is separate.
        // We can run in background
        new Thread(() -> {
            try {
                // If list is small (just default), fetch
                if (availableModels.size() <= 1) {
                   List<String> models = currentProvider.getAvailableModels();
                   if (models != null && !models.isEmpty()) {
                       availableModels = models;
                       // Update config if current model is not in list? 
                       // Keep current.
                   }
                }
            } catch (Exception e) {
                e.printStackTrace(); // Log
            }
        }).start();
    }
    
    private void selectModel(String model) {
        MineGPTConfig config = MineGPTConfig.getInstance();
        switch (config.selectedProvider) {
            case "openai" -> config.openaiModel = model;
            case "anthropic" -> config.anthropicModel = model;
            case "google" -> config.googleModel = model;
            case "openrouter" -> config.openrouterModel = model;
        }
        MineGPTConfig.save();
        modelDropdownButton.setMessage(Text.literal(abbreviate(model, 15)));
        showModelDropdown = false;
    }

    // --- Chat Logic ---
    
    private void sendMessage() {
        String message = chatInput.getText().trim();
        if (message.isEmpty() || isWaitingForResponse) {
            return;
        }
        
        messages.add(new ChatMessage("You", message, true));
        chatInput.setText("");
        
        if (MineGPTConfig.getInstance().getCurrentApiKey().isEmpty()) {
            messages.add(new ChatMessage("System", "Please configure your API key in settings!", false));
            return;
        }
        
        isWaitingForResponse = true;
        
        new Thread(() -> {
            try {
                String response = currentProvider.sendMessage(message);
                messages.add(new ChatMessage("AI", response, false));
            } catch (Exception e) {
                messages.add(new ChatMessage("Error", "Failed: " + e.getMessage(), false));
            } finally {
                isWaitingForResponse = false;
            }
        }).start();
    }
    
    // --- Rendering ---
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw background manually to prevent blur crash
        context.fill(0, 0, this.width, this.height, 0xCC000000);
        
        if (showSettings) {
            context.drawCenteredTextWithShadow(this.textRenderer, "MineGPT Settings", this.width / 2, 20, 0xFFFFFF);
            int startY = 85; 
            int x = this.width / 2 - 150;
            context.drawTextWithShadow(this.textRenderer, "OpenAI API Key:", x, 85 - 12, 0xAAAAAA);
            context.drawTextWithShadow(this.textRenderer, "Anthropic API Key:", x, 85 + 35 - 12, 0xAAAAAA);
            context.drawTextWithShadow(this.textRenderer, "Google API Key:", x, 85 + 70 - 12, 0xAAAAAA);
            context.drawTextWithShadow(this.textRenderer, "OpenRouter API Key:", x, 85 + 105 - 12, 0xAAAAAA);
        } else {
            // Chat Title
            context.drawCenteredTextWithShadow(this.textRenderer, "MineGPT - " + MineGPTConfig.getInstance().getCurrentModel(), this.width / 2, 20, 0x00FF00);
            
            // Draw messages
            renderMessages(context);
            
            if (isWaitingForResponse) {
                context.drawCenteredTextWithShadow(this.textRenderer, "AI is thinking...", this.width / 2, this.height - 60, 0xFFFF00);
            }
        }
        
        // Draw widgets (buttons, inputs)
        super.render(context, mouseX, mouseY, delta);
        
        // Draw Model Dropdown Overlay
        if (showModelDropdown && !showSettings) {
            renderModelDropdown(context, mouseX, mouseY);
        }
    }
    
    private void renderMessages(DrawContext context) {
        int startY = 50;
        int maxY = this.height - 50; // Above input bar
        int wrapWidth = this.width - 60;
        
        List<RenderableLine> allLines = new ArrayList<>();
        for (ChatMessage msg : messages) {
            int color = msg.isUser ? 0xFF55FF55 : 0xFF5555FF;
            if (msg.sender.equals("Error")) color = 0xFFFF5555;
            if (msg.sender.equals("System")) color = 0xFFFFFF55;
            
            String prefix = msg.sender + ": ";
            List<String> wrapped = wrapText(msg.message, wrapWidth - this.textRenderer.getWidth(prefix));
            
            for (int i = 0; i < wrapped.size(); i++) {
                String line = (i == 0) ? (prefix + wrapped.get(i)) : ("   " + wrapped.get(i));
                allLines.add(new RenderableLine(line, (i == 0) ? color : 0xFFFFFFFF, (i == 0) ? prefix.length() : 0));
            }
        }

        int totalLines = allLines.size();
        int maxVisibleLines = (maxY - startY) / 12;
        
        // Clamp scroll offset
        int maxScroll = Math.max(0, totalLines - maxVisibleLines);
        if (chatScrollOffset > maxScroll) chatScrollOffset = maxScroll;
        
        int y = startY;
        for (int i = chatScrollOffset; i < Math.min(chatScrollOffset + maxVisibleLines, totalLines); i++) {
            RenderableLine rl = allLines.get(i);
            if (rl.prefixLen > 0) {
                String prefix = rl.text.substring(0, rl.prefixLen);
                String rest = rl.text.substring(rl.prefixLen);
                context.drawTextWithShadow(this.textRenderer, prefix, 20, y, rl.color);
                context.drawTextWithShadow(this.textRenderer, rest, 20 + this.textRenderer.getWidth(prefix), y, 0xFFFFFFFF);
            } else {
                context.drawTextWithShadow(this.textRenderer, rl.text, 20, y, 0xFFFFFFFF);
            }
            y += 12;
        }
        
        // Chat Scrollbar
        if (totalLines > maxVisibleLines) {
            int sbX = this.width - 10;
            int sbH = maxY - startY;
            context.fill(sbX, startY, sbX + 4, maxY, 0xFF222222);
            float pct = (float) chatScrollOffset / maxScroll;
            int thumbH = Math.max(10, (int)((float)maxVisibleLines / totalLines * sbH));
            int thumbY = startY + (int)(pct * (sbH - thumbH));
            context.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xFFFFFFFF);
        }
    }

    private static class RenderableLine {
        String text;
        int color;
        int prefixLen;
        RenderableLine(String text, int color, int prefixLen) {
            this.text = text;
            this.color = color;
            this.prefixLen = prefixLen;
        }
    }
    
    private void renderModelDropdown(DrawContext context, int mouseX, int mouseY) {
        int x = modelDropdownButton.getX();
        int y = modelDropdownButton.getY() - 150; // Appear above
        int w = 200; // Wider
        int h = 145;
        
        context.fill(x, y, x + w, y + h, 0xFF000000);
        // Draw border manually
        context.fill(x, y, x + w, y + 1, 0xFFFFFFFF); // Top
        context.fill(x, y + h - 1, x + w, y + h, 0xFFFFFFFF); // Bottom
        context.fill(x, y, x + 1, y + h, 0xFFFFFFFF); // Left
        context.fill(x + w - 1, y, x + w, y + h, 0xFFFFFFFF); // Right
        
        List<String> displayModels = availableModels; // Add filtering here later
        
        int itemHeight = 12;
        int maxItems = h / itemHeight;
        
        for (int i = 0; i < maxItems; i++) {
            int idx = i + modelScrollOffset;
            if (idx >= displayModels.size()) break;
            
            String model = displayModels.get(idx);
            boolean hovered = (mouseX >= x && mouseX <= x + w && mouseY >= y + i * itemHeight && mouseY < y + (i + 1) * itemHeight);
            
            int bgColor = hovered ? 0xFF444444 : 0xFF222222;
            context.fill(x + 1, y + i * itemHeight + 1, x + w - 1, y + (i + 1) * itemHeight - 1, bgColor);
            context.drawTextWithShadow(this.textRenderer, abbreviate(model, 30), x + 4, y + i * itemHeight + 2, 0xFFFFFFFF);
        }
        
        // Scrollbar indicator?
        if (displayModels.size() > maxItems) {
            // Simple indicator
             context.fill(x + w - 4, y, x + w, y + h, 0xFF555555);
             float pct = (float) modelScrollOffset / (displayModels.size() - maxItems);
             int sy = y + (int)(pct * (h - 20));
             context.fill(x + w - 4, sy, x + w, sy + 20, 0xFFFFFFFF);
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (showModelDropdown) {
             modelScrollOffset -= (int)verticalAmount;
             if (modelScrollOffset < 0) modelScrollOffset = 0;
             int maxOffset = Math.max(0, availableModels.size() - 10); // Approx
             if (modelScrollOffset > maxOffset) modelScrollOffset = maxOffset;
             return true;
        } else if (!showSettings) {
             chatScrollOffset -= (int)verticalAmount;
             if (chatScrollOffset < 0) chatScrollOffset = 0;
             // Max scroll is calculated in renderMessages for convenience, but we could do it here too
             return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean isRelease) {
        double mouseX = click.x();
        double mouseY = click.y();
        
        // 1. Handle Model Dropdown Overlay first (top layer)
        if (showModelDropdown && !showSettings) {
            int x = modelDropdownButton.getX();
            int y = modelDropdownButton.getY() - 150;
            int w = 200;
            int h = 145;
            
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                int itemHeight = 12;
                int clickedIndex = (int)((mouseY - y) / itemHeight) + modelScrollOffset;
                if (clickedIndex >= 0 && clickedIndex < availableModels.size()) {
                    selectModel(availableModels.get(clickedIndex));
                    return true;
                }
            } else if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h + 20) {
                // Click outside closes dropdown
                showModelDropdown = false;
            }
        }
        
        // 2. Delegate to super (button widgets, text fields)
        if (super.mouseClicked(click, isRelease)) {
            return true;
        }

        return false;
    }
    
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (!showSettings && this.getFocused() == chatInput) {
            if (input.key() == 257 || input.key() == 335) { // Enter
                sendMessage();
                return true;
            }
        }
        return super.keyPressed(input);
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (this.textRenderer.getWidth(testLine) > maxWidth) {
                if (!currentLine.isEmpty()) lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }
        if (!currentLine.isEmpty()) lines.add(currentLine.toString());
        return lines;
    }

    private static class ChatMessage {
        String sender;
        String message;
        boolean isUser;
        ChatMessage(String sender, String message, boolean isUser) {
            this.sender = sender;
            this.message = message;
            this.isUser = isUser;
        }
    }
}
