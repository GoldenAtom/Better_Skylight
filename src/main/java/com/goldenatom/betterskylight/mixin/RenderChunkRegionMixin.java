package com.goldenatom.betterskylight.mixin;

import com.goldenatom.betterskylight.BackingLevelAccess;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderChunkRegion.class)
public interface RenderChunkRegionMixin extends BackingLevelAccess {
    @Override
    @Accessor("level")
    Level betterSkylight$getLevel();
}
