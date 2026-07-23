package com.bitz.minegpt;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import com.bitz.minegpt.gui.AIChatScreen;
import com.bitz.minegpt.config.MineGPTConfig;

public class MineGPT implements ClientModInitializer {
    public static final String MOD_ID = "minegpt";
    public static final String MOD_NAME = "MineGPT";

    private static KeyMapping openChatKey;

    @Override
    public void onInitializeClient() {
        MineGPTConfig.load();

        try {
            Class<?> kbClass = KeyMapping.class;
            Object category = null;

            for (java.lang.reflect.Field f : kbClass.getFields()) {
                if (f.getName().contains("MISC") || f.getName().contains("GAMEPLAY")) {
                    category = f.get(null);
                    break;
                }
            }
            if (category == null) category = "key.categories.misc";

            java.lang.reflect.Constructor<?>[] constructors = kbClass.getConstructors();
            for (java.lang.reflect.Constructor<?> c : constructors) {
                Class<?>[] types = c.getParameterTypes();
                if (types.length == 3 && types[0] == String.class && types[1] == int.class) {
                    openChatKey = (KeyMapping) c.newInstance("key.minegpt.open_chat", GLFW.GLFW_KEY_PERIOD, category);
                    break;
                }
                if (types.length == 4 && types[0] == String.class && types[1] == InputConstants.Type.class && types[2] == int.class) {
                    openChatKey = (KeyMapping) c.newInstance("key.minegpt.open_chat", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_PERIOD, category);
                    break;
                }
            }

            if (openChatKey != null) {
                KeyMappingHelper.registerKeyMapping(openChatKey);
                System.out.println("[MineGPT] KeyMapping registered successfully");
            }
        } catch (Exception e) {
            System.err.println("[MineGPT] Failed to register keybinding: " + e.getMessage());
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean pressed = false;
            if (openChatKey != null) {
                while (openChatKey.consumeClick()) pressed = true;
            } else {
                long window = client.getWindow().handle();
                if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_PERIOD) == GLFW.GLFW_PRESS) {
                    if (client.gui.screen() == null) pressed = true;
                }
            }

            if (pressed && !(client.gui.screen() instanceof AIChatScreen)) {
                client.gui.setScreen(new AIChatScreen());
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("minegpt")
                .executes(context -> {
                    Minecraft client = Minecraft.getInstance();
                    client.execute(() -> {
                        if (!(client.gui.screen() instanceof AIChatScreen)) {
                            client.gui.setScreen(new AIChatScreen());
                        }
                    });
                    return 1;
                }));
        });

        System.out.println("[MineGPT] Initialized successfully!");
    }
}