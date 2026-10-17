package com.teampreview.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TeamPreviewConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("cobblemon_teampreview.json");

    private static TeamPreviewConfig instance;

    /** Master switch. When false the mod does nothing and right-click battles normally. */
    public boolean enabled = true;
    /** When true, trainers that would force a battle by looking at the player no longer do so. */
    public boolean blockAutoBattleOnLook = false;

    public boolean showIvs = true;
    public boolean showEvs = true;
    public boolean showHeldItem = true;
    public boolean showAbility = true;
    public boolean showNature = true;

    public static TeamPreviewConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                TeamPreviewConfig loaded = GSON.fromJson(reader, TeamPreviewConfig.class);
                if (loaded != null) {
                    instance = loaded;
                    return;
                }
            } catch (IOException | RuntimeException ignored) {
            }
        }
        instance = new TeamPreviewConfig();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(get(), writer);
            }
        } catch (IOException ignored) {
        }
    }
}
