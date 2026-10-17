package com.teampreview.client;

import com.teampreview.TeamPreviewMod;
import com.teampreview.client.gui.ConfigScreen;
import com.teampreview.client.gui.TeamPreviewScreen;
import com.teampreview.config.TeamPreviewConfig;
import com.teampreview.dto.TeamPreviewData;
import com.teampreview.network.TeamPreviewPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TeamPreviewClient implements ClientModInitializer {
    private static KeyBinding openConfigKey;

    @Override
    public void onInitializeClient() {
        TeamPreviewConfig.load();

        ClientPlayNetworking.registerGlobalReceiver(TeamPreviewPayload.ID, (payload, context) -> {
            TeamPreviewData data = payload.parse();
            if (data == null || data.team.isEmpty()) {
                return;
            }
            context.client().execute(() -> context.client().setScreen(new TeamPreviewScreen(data)));
        });

        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + TeamPreviewMod.MOD_ID + ".open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category." + TeamPreviewMod.MOD_ID));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.wasPressed()) {
                client.setScreen(new ConfigScreen(client.currentScreen));
            }
        });
    }
}
