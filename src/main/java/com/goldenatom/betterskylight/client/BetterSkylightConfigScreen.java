package com.goldenatom.betterskylight.client;

import com.goldenatom.betterskylight.BetterSkylightConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class BetterSkylightConfigScreen extends Screen {
    private final Screen parent;

    private boolean enabled;
    private boolean ambientSkyLightEnabled;

    public BetterSkylightConfigScreen(Screen parent) {
        super(Component.translatable("screen.better_skylight.config"));
        this.parent = parent;

        this.enabled = BetterSkylightConfig.ENABLED.get();
        this.ambientSkyLightEnabled =
                BetterSkylightConfig.AMBIENT_SKY_LIGHT_ENABLED.get();
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 100;

        addRenderableWidget(Button.builder(
                label("enabled", enabled),
                button -> {
                    enabled = !enabled;
                    button.setMessage(label("enabled", enabled));
                }
        ).bounds(left, 60, 200, 20).build());

        addRenderableWidget(Button.builder(
                label("ambient", ambientSkyLightEnabled),
                button -> {
                    ambientSkyLightEnabled = !ambientSkyLightEnabled;
                    button.setMessage(label("ambient", ambientSkyLightEnabled));
                }
        ).bounds(left, 86, 200, 20).build());

        addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE,
                button -> saveAndClose()
        ).bounds(left, this.height - 40, 200, 20).build());
    }

    private Component label(String key, boolean value) {
        return Component.translatable(
                "config.better_skylight." + key,
                value
                        ? Component.translatable("config.better_skylight.on")
                        : Component.translatable("config.better_skylight.off")
        );
    }

    private void saveAndClose() {
        boolean lightingChanged = enabled != BetterSkylightConfig.ENABLED.get()
                || ambientSkyLightEnabled
                != BetterSkylightConfig.AMBIENT_SKY_LIGHT_ENABLED.get();

        BetterSkylightConfig.ENABLED.set(enabled);
        BetterSkylightConfig.AMBIENT_SKY_LIGHT_ENABLED.set(
                ambientSkyLightEnabled
        );
        BetterSkylightConfig.save();
        if (lightingChanged && minecraft.level != null) {
            minecraft.levelRenderer.allChanged();
        }
        minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(
                font,
                title,
                width / 2,
                25,
                0xFFFFFF
        );

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
