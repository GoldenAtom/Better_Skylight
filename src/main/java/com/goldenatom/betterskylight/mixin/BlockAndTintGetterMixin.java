package com.goldenatom.betterskylight.mixin;

import com.goldenatom.betterskylight.SkyExposureCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockAndTintGetter.class)
public interface BlockAndTintGetterMixin {
    @Inject(method = "getBrightness", at = @At("RETURN"), cancellable = true)
    private void betterSkylight$replaceSkyLight(
            LightLayer lightLayer,
            BlockPos pos,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (lightLayer == LightLayer.SKY) {
            BlockAndTintGetter level = (BlockAndTintGetter) (Object) this;
            cir.setReturnValue(SkyExposureCache.getSkyLight(level, pos, cir.getReturnValueI()));
        }
    }

    @Inject(method = "getRawBrightness", at = @At("RETURN"), cancellable = true)
    private void betterSkylight$replaceCombinedLight(
            BlockPos pos,
            int skyDarken,
            CallbackInfoReturnable<Integer> cir
    ) {
        BlockAndTintGetter level = (BlockAndTintGetter) (Object) this;
        int blockLight = level.getLightEngine()
                .getLayerListener(LightLayer.BLOCK)
                .getLightValue(pos);
        int vanillaSkyLight = level.getLightEngine()
                .getLayerListener(LightLayer.SKY)
                .getLightValue(pos);
        int skyLight = SkyExposureCache.getSkyLight(
                level,
                pos,
                vanillaSkyLight
        );

        cir.setReturnValue(Math.max(blockLight, skyLight - skyDarken));
    }
}
