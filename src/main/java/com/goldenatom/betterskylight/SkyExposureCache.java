package com.goldenatom.betterskylight;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Cached per-block ambient sky exposure. The calculation samples verified sky
 * openings around an overhead blocker and blends their directional exposure.
 */
public final class SkyExposureCache {
    private static final int ANGULAR_SAMPLES = 16;
    private static final Map<BlockGetter, Map<Long, Byte>> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private SkyExposureCache() {
    }

    public static int getSkyLight(BlockGetter level, BlockPos pos, int vanillaValue) {
        if (!BetterSkylightConfig.ENABLED.get()
                || !BetterSkylightConfig.AMBIENT_SKY_LIGHT_ENABLED.get()) {
            return vanillaValue;
        }

        LevelReader reader = findLevelReader(level);
        if (reader == null) {
            return vanillaValue;
        }

        Map<Long, Byte> levelCache;
        synchronized (CACHE) {
            levelCache = CACHE.computeIfAbsent(level, ignored -> new HashMap<>());
        }

        long key = pos.asLong();
        synchronized (levelCache) {
            Byte cached = levelCache.get(key);
            if (cached != null) {
                return cached & 0xFF;
            }

            int calculated = calculate(reader, pos, vanillaValue);
            levelCache.put(key, (byte) calculated);
            return calculated;
        }
    }

    public static void invalidate(BlockGetter level) {
        synchronized (CACHE) {
            CACHE.remove(level);
        }
    }

    public static void clear() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    private static LevelReader findLevelReader(BlockGetter level) {
        if (level instanceof LevelReader reader) {
            return reader;
        }
        if (level instanceof BackingLevelAccess access) {
            return access.betterSkylight$getLevel();
        }
        return null;
    }

    private static int calculate(LevelReader level, BlockPos pos, int vanillaValue) {
        if (!hasLoadedChunk(level, pos.getX(), pos.getZ())) {
            return vanillaValue;
        }

        int ceilingDistance = findCeilingDistance(level, pos);
        if (ceilingDistance < 0) {
            return 15;
        }

        double coneAngle = Math.toRadians(BetterSkylightConfig.AMBIENT_CONE_ANGLE_DEGREES.get());
        double coneRadius = ceilingDistance * Math.tan(coneAngle);
        double openness = findAmbientOpenness(level, pos, coneRadius);
        int calculated = (int) Math.round(openness * 15.0D);

        // This milestone only restores missing ambient light. It does not make
        // vanilla-lit positions darker until the full occlusion field exists.
        return Math.max(vanillaValue, Math.max(0, Math.min(15, calculated)));
    }

    private static int findCeilingDistance(LevelReader level, BlockPos pos) {
        int surfaceY = level.getHeight(
                Heightmap.Types.WORLD_SURFACE,
                pos.getX(),
                pos.getZ()
        );
        if (surfaceY <= pos.getY() + 1) {
            return -1;
        }

        int maximumY = level.getMaxBuildHeight();
        int configuredDistance = BetterSkylightConfig.ANALYSIS_DISTANCE.get();
        int endY = Math.min(
                Math.min(maximumY, surfaceY + 1),
                pos.getY() + configuredDistance + 1
        );
        BlockPos.MutableBlockPos cursor = pos.mutable();

        for (int y = pos.getY() + 1; y < endY; y++) {
            cursor.setY(y);
            if (level.getBlockState(cursor).getLightBlock(level, cursor) > 0) {
                return y - pos.getY();
            }
        }

        return -1;
    }

    private static double findAmbientOpenness(
            LevelReader level,
            BlockPos pos,
            double coneRadius
    ) {
        int maximumDistance = BetterSkylightConfig.ANALYSIS_DISTANCE.get();
        int[] openingDistances = new int[ANGULAR_SAMPLES];
        int unresolvedDirections = ANGULAR_SAMPLES;

        for (int radius = 1; radius <= maximumDistance; radius = nextRadius(radius, maximumDistance)) {
            for (int sample = 0; sample < ANGULAR_SAMPLES; sample++) {
                if (openingDistances[sample] != 0) {
                    continue;
                }

                double angle = Math.PI * 2.0D * sample / ANGULAR_SAMPLES;
                int x = pos.getX() + (int) Math.round(Math.cos(angle) * radius);
                int z = pos.getZ() + (int) Math.round(Math.sin(angle) * radius);

                if (!hasLoadedChunk(level, x, z)) {
                    continue;
                }

                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                if (surfaceY <= pos.getY() + 1) {
                    openingDistances[sample] = radius;
                    unresolvedDirections--;
                }
            }

            if (unresolvedDirections == 0 || radius == maximumDistance) {
                break;
            }
        }

        double exposureSum = 0.0D;
        for (int distance : openingDistances) {
            if (distance > 0) {
                exposureSum += Math.min(1.0D, coneRadius / distance);
            }
        }

        // Square-root response keeps a partial view of the sky useful while
        // still blending the edge over all sampled directions.
        return Math.sqrt(exposureSum / ANGULAR_SAMPLES);
    }

    private static boolean hasLoadedChunk(LevelReader level, int blockX, int blockZ) {
        int chunkX = SectionPos.blockToSectionCoord(blockX);
        int chunkZ = SectionPos.blockToSectionCoord(blockZ);
        return level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null;
    }

    private static int nextRadius(int radius, int maximumDistance) {
        if (radius < 8) {
            return radius + 1;
        }
        return Math.min(maximumDistance, radius * 2);
    }
}
