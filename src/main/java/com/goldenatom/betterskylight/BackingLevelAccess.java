package com.goldenatom.betterskylight;

import net.minecraft.world.level.LevelReader;

/** Implemented by rendering-world wrappers that retain their backing level. */
public interface BackingLevelAccess {
    LevelReader betterSkylight$getLevel();
}
