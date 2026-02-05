package com.bitz.minegpt;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import com.bitz.minegpt.gui.AIChatScreen;
import com.bitz.minegpt.config.MineGPTConfig;

public class MineGPT implements ClientModInitializer {
    public static final String MOD_ID = "minegpt";
    public static final String MOD_NAME = "MineGPT";
    
    private static KeyBinding openChatKey;
    
    @Override
    public void onInitializeClient() {
        // Load configuration
        MineGPTConfig.load();
        
        // Register keybinding using reflection to handle API differences
        try {
            Class<?> kbClass = KeyBinding.class;
            Object category = null;
            
            // Look for standard category fields first
            for (java.lang.reflect.Field f : kbClass.getFields()) {
                if (f.getName().contains("MISC") || f.getName().contains("GAMEPLAY")) {
                    category = f.get(null);
                    break;
                }
            }
            if (category == null) category = "key.categories.misc"; // Fallback to string

            java.lang.reflect.Constructor<?>[] constructors = kbClass.getConstructors();
            for (java.lang.reflect.Constructor<?> c : constructors) {
                Class<?>[] types = c.getParameterTypes();
                // Match (String, int, String/Category)
                if (types.length == 3 && types[0] == String.class && types[1] == int.class) {
                    openChatKey = (KeyBinding) c.newInstance("key.minegpt.open_chat", GLFW.GLFW_KEY_PERIOD, category);
                    break;
                }
                // Match (String, Type, int, String/Category)
                if (types.length == 4 && types[0] == String.class && types[1] == InputUtil.Type.class && types[2] == int.class) {
                    openChatKey = (KeyBinding) c.newInstance("key.minegpt.open_chat", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_PERIOD, category);
                    break;
                }
            }
            
            if (openChatKey != null) {
                KeyBindingHelper.registerKeyBinding(openChatKey);
                System.out.println("[MineGPT] KeyBinding registered successfully");
            }
        } catch (Exception e) {
            System.err.println("[MineGPT] Failed to register keybinding: " + e.getMessage());
        }
        
        // Register tick event
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean pressed = false;
            if (openChatKey != null) {
                while (openChatKey.wasPressed()) pressed = true;
            } else {
                // Fallback period key check (using GLFW)
                long window = client.getWindow().getHandle();
                if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_PERIOD) == GLFW.GLFW_PRESS) {
                    // This is "isDown", we need "wasPressed" logic.
                    // For now, only if no screen is open to avoid repeating.
                    if (client.currentScreen == null) pressed = true;
                }
            }

            if (pressed && !(client.currentScreen instanceof AIChatScreen)) {
                client.setScreen(new AIChatScreen());
            }
        });
        
        // Register command /minegpt as backup - Fixed to avoid adding new listeners every time
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("minegpt")
                .executes(context -> {
                    net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                    client.execute(() -> {
                        if (!(client.currentScreen instanceof AIChatScreen)) {
                            client.setScreen(new AIChatScreen());
                        }
                    });
                    return 1;
                }));
        });
        
        System.out.println("[MineGPT] Initialized successfully!");
    }
}
