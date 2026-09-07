package com.goldenatom.betterskylight;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;

@Mod(BetterSkylight.MOD_ID)
public final class BetterSkylight {
    public static final String MOD_ID = "better_skylight";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BetterSkylight(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(
                ModConfig.Type.COMMON,
                BetterSkylightConfig.SPEC,
                "better_skylight-common.toml"
        );

        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) ->
                SkyExposureCache.invalidate(event.getLevel()));
        NeoForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent event) ->
                SkyExposureCache.invalidate(event.getLevel()));
        NeoForge.EVENT_BUS.addListener((BlockEvent.FluidPlaceBlockEvent event) ->
                SkyExposureCache.invalidate(event.getLevel()));
        NeoForge.EVENT_BUS.addListener((BlockEvent.BlockToolModificationEvent event) ->
                SkyExposureCache.invalidate(event.getLevel()));

        LOGGER.info("Better Skylight initialized");
    }
}
