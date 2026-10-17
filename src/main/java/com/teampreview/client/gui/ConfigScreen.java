package com.teampreview.client.gui;

import com.teampreview.TeamPreviewMod;
import com.teampreview.config.TeamPreviewConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/** Vanilla-styled configuration screen, also reachable from Mod Menu when it is installed. */
public class ConfigScreen extends Screen {
    private final Screen parent;
    private final TeamPreviewConfig config = TeamPreviewConfig.get();

    public ConfigScreen(Screen parent) {
        super(Text.translatable("gui." + TeamPreviewMod.MOD_ID + ".config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 130;
        int y = this.height / 2 - 78;
        int w = 260;
        int h = 20;
        int gap = 22;

        addToggle(x, y, w, h, "enabled", config.enabled, v -> config.enabled = v);
        y += gap;
        addToggle(x, y, w, h, "blockAutoBattleOnLook", config.blockAutoBattleOnLook, v -> config.blockAutoBattleOnLook = v);
        y += gap + 8;
        addToggle(x, y, w, h, "showIvs", config.showIvs, v -> config.showIvs = v);
        y += gap;
        addToggle(x, y, w, h, "showEvs", config.showEvs, v -> config.showEvs = v);
        y += gap;
        addToggle(x, y, w, h, "showHeldItem", config.showHeldItem, v -> config.showHeldItem = v);
        y += gap;
        addToggle(x, y, w, h, "showAbility", config.showAbility, v -> config.showAbility = v);
        y += gap;
        addToggle(x, y, w, h, "showNature", config.showNature, v -> config.showNature = v);
        y += gap + 8;

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> this.close())
                .dimensions(this.width / 2 - 100, y, 200, h).build());
    }

    private void addToggle(int x, int y, int w, int h, String key, boolean initial, Consumer<Boolean> setter) {
        addDrawableChild(CyclingButtonWidget.onOffBuilder(initial)
                .build(x, y, w, h, Text.translatable("config." + TeamPreviewMod.MOD_ID + "." + key),
                        (button, value) -> setter.accept(value)));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 104, 0xFFFFFF);
    }

    @Override
    public void close() {
        TeamPreviewConfig.save();
        this.client.setScreen(this.parent);
    }
}
