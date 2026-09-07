package com.goldenatom.betterskylight;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(BetterSkylight.MOD_ID)
public final class BetterSkylight {
    public static final String MOD_ID = "better_skylight";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BetterSkylight(IEventBus modEventBus) {
        LOGGER.info("Better Skylight initialized");
    }
}
