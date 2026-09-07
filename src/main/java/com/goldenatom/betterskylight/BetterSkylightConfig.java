package com.goldenatom.betterskylight;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class BetterSkylightConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue AMBIENT_SKY_LIGHT_ENABLED;
    public static final ModConfigSpec.DoubleValue AMBIENT_CONE_ANGLE_DEGREES;
    public static final ModConfigSpec.IntValue ANALYSIS_DISTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Better Skylight settings").push("general");

        ENABLED = builder
                .comment("Master switch. Disable to use vanilla skylight.")
                .define("enabled", true);

        AMBIENT_SKY_LIGHT_ENABLED = builder
                .comment("Enable geometry-aware ambient skylight.")
                .define("ambient_sky_light_enabled", true);

        builder.pop();

        builder.comment("Lighting analysis tuning").push("analysis");

        AMBIENT_CONE_ANGLE_DEGREES = builder
                .comment("Angular radius used for ambient sky visibility.")
                .defineInRange("ambient_cone_angle_degrees", 6.0D, 0.5D, 45.0D);

        ANALYSIS_DISTANCE = builder
                .comment("Maximum distance used when searching for sky openings.")
                .defineInRange("analysis_distance", 512, 16, 2048);

        builder.pop();

        SPEC = builder.build();
    }

    private BetterSkylightConfig() {
    }

    public static void save() {
        SPEC.save();
        SkyExposureCache.clear();
    }
}
