package com.goldenatom.betterskylight.mixin;

import com.goldenatom.betterskylight.SkyExposureCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sodium overrides the vanilla interface method, so it needs its own hook. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public abstract class SodiumLevelSliceMixin {
    @Shadow
    @Final
    private ClientLevel level;

    @Inject(method = "getBrightness", at = @At("RETURN"), cancellable = true, remap = false)
    private void betterSkylight$replaceSkyLight(
            LightLayer lightLayer,
            BlockPos pos,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (lightLayer == LightLayer.SKY) {
            cir.setReturnValue(SkyExposureCache.getSkyLight(
                    level,
                    pos,
                    cir.getReturnValueI()
            ));
        }
    }
}
