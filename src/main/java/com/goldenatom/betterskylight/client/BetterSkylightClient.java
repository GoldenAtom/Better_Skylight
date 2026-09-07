package com.goldenatom.betterskylight.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = "better_skylight", dist = Dist.CLIENT)
public final class BetterSkylightClient {
    public BetterSkylightClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, parent) -> new BetterSkylightConfigScreen(parent)
        );
    }
}