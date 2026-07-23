package com.bitz.minegpt.gui;

import com.bitz.minegpt.config.MineGPTConfig;
import com.bitz.minegpt.ai.AIProvider;
import com.bitz.minegpt.ai.OpenAIProvider;
import com.bitz.minegpt.ai.AnthropicProvider;
import com.bitz.minegpt.ai.GoogleProvider;
import com.bitz.minegpt.ai.OpenRouterProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.math.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AIChatScreen extends Screen {
    private EditBox chatInput;
    private Button sendButton;
    private Button closeButton;
    private Button settingsButton;
    private Button clearButton;
    private Button modelDropdownButton;

    private EditBox openaiKeyField;
    private EditBox anthropicKeyField;
    private EditBox googleKeyField;
    private EditBox openrouterKeyField;
    private Button providerButton;
    private Button saveSettingsButton;
    private Button cancelSettingsButton;

    private List<ChatMessage> messages = new ArrayList<>();
    private boolean showSettings = false;
    private boolean showModelDropdown = false;

    private AIProvider currentProvider;
    private boolean isWaitingForResponse = false;

    private List<String> availableModels = new ArrayList<>();
    private int modelScrollOffset = 0;
    private int chatScrollOffset = 0;

    public AIChatScreen() {
        super(Component.literal("MineGPT - AI Chat"));
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
        availableModels.clear();
        availableModels.add(config.getCurrentModel());
    }

    @Override
    protected void init() {
        super.init();

        int bottomY = this.height - 40;
        int centerX = this.width / 2;

        int inputWidth = this.width - 350;
        if (inputWidth < 100) inputWidth = 100;

        chatInput = new EditBox(this.font, 20, bottomY, inputWidth, 20, Component.literal("Message"));
        chatInput.setMaxLength(2000);
        chatInput.setHint(Component.literal("Ask AI..."));
        this.addRenderableWidget(chatInput);
        this.setInitialFocus(chatInput);

        int currentX = 20 + inputWidth + 5;

        modelDropdownButton = Button.builder(
            Component.literal(abbreviate(MineGPTConfig.getInstance().getCurrentModel(), 15)),
            button -> toggleModelDropdown()
        ).bounds(currentX, bottomY, 100, 20).build();
        this.addRenderableWidget(modelDropdownButton);
        currentX += 105;

        clearButton = Button.builder(
            Component.literal("Clear"),
            button -> {
                chatInput.setValue("");
                messages.clear();
            }
        ).bounds(currentX, bottomY, 50, 20).build();
        this.addRenderableWidget(clearButton);
        currentX += 55;

        sendButton = Button.builder(
            Component.literal("Send"),
            button -> sendMessage()
        ).bounds(currentX, bottomY, 50, 20).build();
        this.addRenderableWidget(sendButton);
        currentX += 55;

        settingsButton = Button.builder(
            Component.literal("\u2699"),
            button -> toggleSettings()
        ).bounds(this.width - 30, 10, 20, 20).build();
        this.addRenderableWidget(settingsButton);

        closeButton = Button.builder(
            Component.literal("X"),
            button -> this.close()
        ).bounds(10, 10, 20, 20).build();
        this.addRenderableWidget(closeButton);

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

        providerButton = Button.builder(
            Component.literal("Provider: " + config.selectedProvider.toUpperCase()),
            button -> cycleProvider()
        ).bounds(settingsX, startY, 300, 20).build();
        this.addRenderableWidget(providerButton);

        int gap = 35;

        openaiKeyField = new EditBox(this.font, settingsX, startY + gap, 300, 20, Component.literal("OpenAI Key"));
        openaiKeyField.setMaxLength(200);
        openaiKeyField.setValue(config.openaiApiKey);
        openaiKeyField.setHint(Component.literal("OpenAI API Key (sk-...)"));
        this.addRenderableWidget(openaiKeyField);

        anthropicKeyField = new EditBox(this.font, settingsX, startY + gap * 2, 300, 20, Component.literal("Anthropic Key"));
        anthropicKeyField.setMaxLength(200);
        anthropicKeyField.setValue(config.anthropicApiKey);
        anthropicKeyField.setHint(Component.literal("Anthropic API Key (sk-ant-...)"));
        this.addRenderableWidget(anthropicKeyField);

        googleKeyField = new EditBox(this.font, settingsX, startY + gap * 3, 300, 20, Component.literal("Google Key"));
        googleKeyField.setMaxLength(200);
        googleKeyField.setValue(config.googleApiKey);
        googleKeyField.setHint(Component.literal("Google API Key (AIza...)"));
        this.addRenderableWidget(googleKeyField);

        openrouterKeyField = new EditBox(this.font, settingsX, startY + gap * 4, 300, 20, Component.literal("OpenRouter Key"));
        openrouterKeyField.setMaxLength(200);
        openrouterKeyField.setValue(config.openrouterApiKey);
        openrouterKeyField.setHint(Component.literal("OpenRouter API Key (sk-or-...)"));
        this.addRenderableWidget(openrouterKeyField);

        saveSettingsButton = Button.builder(
            Component.literal("Save & Close Settings"),
            button -> {
                saveSettings();
                toggleSettings();
            }
        ).bounds(settingsX, startY + gap * 5 + 10, 300, 20).build();
        this.addRenderableWidget(saveSettingsButton);
    }

    private void updateWidgetVisibility() {
        boolean chatVisible = !showSettings;

        chatInput.visible = chatVisible;
        sendButton.visible = chatVisible;
        clearButton.visible = chatVisible;
        modelDropdownButton.visible = chatVisible;
        closeButton.visible = true;
        settingsButton.visible = !showSettings;

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
        providerButton.setMessage(Component.literal("Provider: " + config.selectedProvider.toUpperCase()));
        initializeProvider();
    }

    private void toggleSettings() {
        showSettings = !showSettings;
        if (showModelDropdown) showModelDropdown = false;
        updateWidgetVisibility();
        if (showSettings) {
            this.setFocused(openaiKeyField);
        } else {
            this.setFocused(chatInput);
        }
    }

    private void saveSettings() {
        MineGPTConfig config = MineGPTConfig.getInstance();
        config.openaiApiKey = openaiKeyField.getValue();
        config.anthropicApiKey = anthropicKeyField.getValue();
        config.googleApiKey = googleKeyField.getValue();
        config.openrouterApiKey = openrouterKeyField.getValue();
        MineGPTConfig.save();
    }

    private void toggleModelDropdown() {
        if (showSettings) return;
        showModelDropdown = !showModelDropdown;

        if (showModelDropdown) {
            fetchModels();
        }
    }

    private void fetchModels() {
        if (isWaitingForResponse) return;
        new Thread(() -> {
            try {
                if (availableModels.size() <= 1) {
                   List<String> models = currentProvider.getAvailableModels();
                   if (models != null && !models.isEmpty()) {
                       availableModels = models;
                   }
                }
            } catch (Exception e) {
                e.printStackTrace();
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
        modelDropdownButton.setMessage(Component.literal(abbreviate(model, 15)));
        showModelDropdown = false;
    }

    private void sendMessage() {
        String message = chatInput.getValue().trim();
        if (message.isEmpty() || isWaitingForResponse) {
            return;
        }

        messages.add(new ChatMessage("You", message, true));
        chatInput.setValue("");

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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xCC000000);

        if (showSettings) {
            graphics.drawCenteredString(this.font, "MineGPT Settings", this.width / 2, 20, 0xFFFFFF);
            int startY = 85;
            int x = this.width / 2 - 150;
            graphics.drawString(this.font, "OpenAI API Key:", x, 85 - 12, 0xAAAAAA, false);
            graphics.drawString(this.font, "Anthropic API Key:", x, 85 + 35 - 12, 0xAAAAAA, false);
            graphics.drawString(this.font, "Google API Key:", x, 85 + 70 - 12, 0xAAAAAA, false);
            graphics.drawString(this.font, "OpenRouter API Key:", x, 85 + 105 - 12, 0xAAAAAA, false);
        } else {
            graphics.drawCenteredString(this.font, "MineGPT - " + MineGPTConfig.getInstance().getCurrentModel(), this.width / 2, 20, 0x00FF00);

            renderMessages(graphics);

            if (isWaitingForResponse) {
                graphics.drawCenteredString(this.font, "AI is thinking...", this.width / 2, this.height - 60, 0xFFFF00);
            }
        }

        super.render(graphics, mouseX, mouseY, delta);

        if (showModelDropdown && !showSettings) {
            renderModelDropdown(graphics, mouseX, mouseY);
        }
    }

    private void renderMessages(GuiGraphics graphics) {
        int startY = 50;
        int maxY = this.height - 50;
        int wrapWidth = this.width - 60;

        List<RenderableLine> allLines = new ArrayList<>();
        for (ChatMessage msg : messages) {
            int color = msg.isUser ? 0xFF55FF55 : 0xFF5555FF;
            if (msg.sender.equals("Error")) color = 0xFFFF5555;
            if (msg.sender.equals("System")) color = 0xFFFFFF55;

            String prefix = msg.sender + ": ";
            List<String> wrapped = wrapText(msg.message, wrapWidth - this.font.width(prefix));

            for (int i = 0; i < wrapped.size(); i++) {
                String line = (i == 0) ? (prefix + wrapped.get(i)) : ("   " + wrapped.get(i));
                allLines.add(new RenderableLine(line, (i == 0) ? color : 0xFFFFFFFF, (i == 0) ? prefix.length() : 0));
            }
        }

        int totalLines = allLines.size();
        int maxVisibleLines = (maxY - startY) / 12;

        int maxScroll = Math.max(0, totalLines - maxVisibleLines);
        if (chatScrollOffset > maxScroll) chatScrollOffset = maxScroll;

        int y = startY;
        for (int i = chatScrollOffset; i < Math.min(chatScrollOffset + maxVisibleLines, totalLines); i++) {
            RenderableLine rl = allLines.get(i);
            if (rl.prefixLen > 0) {
                String prefix = rl.text.substring(0, rl.prefixLen);
                String rest = rl.text.substring(rl.prefixLen);
                graphics.drawString(this.font, prefix, 20, y, rl.color, false);
                graphics.drawString(this.font, rest, 20 + this.font.width(prefix), y, 0xFFFFFFFF, false);
            } else {
                graphics.drawString(this.font, rl.text, 20, y, 0xFFFFFFFF, false);
            }
            y += 12;
        }

        if (totalLines > maxVisibleLines) {
            int sbX = this.width - 10;
            int sbH = maxY - startY;
            graphics.fill(sbX, startY, sbX + 4, maxY, 0xFF222222);
            float pct = (float) chatScrollOffset / maxScroll;
            int thumbH = Math.max(10, (int)((float)maxVisibleLines / totalLines * sbH));
            int thumbY = startY + (int)(pct * (sbH - thumbH));
            graphics.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xFFFFFFFF);
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

    private void renderModelDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = modelDropdownButton.getX();
        int y = modelDropdownButton.getY() - 150;
        int w = 200;
        int h = 145;

        graphics.fill(x, y, x + w, y + h, 0xFF000000);
        graphics.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFFFFFFFF);
        graphics.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
        graphics.fill(x + w - 1, y, x + w, y + h, 0xFFFFFFFF);

        List<String> displayModels = availableModels;

        int itemHeight = 12;
        int maxItems = h / itemHeight;

        for (int i = 0; i < maxItems; i++) {
            int idx = i + modelScrollOffset;
            if (idx >= displayModels.size()) break;

            String model = displayModels.get(idx);
            boolean hovered = (mouseX >= x && mouseX <= x + w && mouseY >= y + i * itemHeight && mouseY < y + (i + 1) * itemHeight);

            int bgColor = hovered ? 0xFF444444 : 0xFF222222;
            graphics.fill(x + 1, y + i * itemHeight + 1, x + w - 1, y + (i + 1) * itemHeight - 1, bgColor);
            graphics.drawString(this.font, abbreviate(model, 30), x + 4, y + i * itemHeight + 2, 0xFFFFFFFF, false);
        }

        if (displayModels.size() > maxItems) {
             graphics.fill(x + w - 4, y, x + w, y + h, 0xFF555555);
             float pct = (float) modelScrollOffset / (displayModels.size() - maxItems);
             int sy = y + (int)(pct * (h - 20));
             graphics.fill(x + w - 4, sy, x + w, sy + 20, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (showModelDropdown) {
             modelScrollOffset -= (int)verticalAmount;
             if (modelScrollOffset < 0) modelScrollOffset = 0;
             int maxOffset = Math.max(0, availableModels.size() - 10);
             if (modelScrollOffset > maxOffset) modelScrollOffset = maxOffset;
             return true;
        } else if (!showSettings) {
             chatScrollOffset -= (int)verticalAmount;
             if (chatScrollOffset < 0) chatScrollOffset = 0;
             return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean isRelease) {
        double mouseX = click.x();
        double mouseY = click.y();

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
                showModelDropdown = false;
            }
        }

        if (super.mouseClicked(click, isRelease)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (!showSettings && this.getFocused() == chatInput) {
            if (input.key() == 257 || input.key() == 335) {
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
            if (this.font.width(testLine) > maxWidth) {
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