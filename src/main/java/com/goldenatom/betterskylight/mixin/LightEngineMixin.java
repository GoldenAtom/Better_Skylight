package com.goldenatom.betterskylight.mixin;

import com.goldenatom.betterskylight.SkyExposureCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.lighting.SkyLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Changes the raw sky value at Minecraft's authoritative light-engine read. */
@Mixin(LightEngine.class)
public abstract class LightEngineMixin {
    @Shadow
    @Final
    protected LightChunkGetter chunkSource;

    @Inject(method = "checkBlock", at = @At("HEAD"))
    private void betterSkylight$invalidateForBlockUpdate(
            BlockPos pos,
            CallbackInfo ci
    ) {
        if ((Object) this instanceof SkyLightEngine) {
            SkyExposureCache.invalidate(chunkSource.getLevel());
        }
    }

    @Inject(method = "getLightValue", at = @At("RETURN"), cancellable = true)
    private void betterSkylight$replaceRawSkyLight(
            BlockPos pos,
            CallbackInfoReturnable<Integer> cir
    ) {
        if ((Object) this instanceof SkyLightEngine) {
            BlockGetter level = chunkSource.getLevel();
            cir.setReturnValue(SkyExposureCache.getSkyLight(
                    level,
                    pos,
                    cir.getReturnValueI()
            ));
        }
    }
}
