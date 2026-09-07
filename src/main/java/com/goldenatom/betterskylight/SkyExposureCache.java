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
 * Cached per-block ambient sky exposure. This first implementation uses the
 * nearest overhead blocker and sampled verified sky openings around it.
 */
public final class SkyExposureCache {
    private static final int ANGULAR_SAMPLES = 16;
    private static final Map<BlockGetter, Map<Long, Byte>> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private SkyExposureCache() {
    }

    public static int getSkyLight(BlockGetter level, BlockPos pos, int vanillaValue) {
        if (!BetterSkylightConfig.ENABLED.get()) {
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

        OpeningSearch opening = findNearestOpening(level, pos);
        int propagatedOpeningDistance = inferPropagatedOpeningDistance(vanillaValue);
        int openingDistance = nearestPositiveDistance(
                opening.distance,
                propagatedOpeningDistance
        );
        if (openingDistance < 0) {
            return vanillaValue;
        }

        double coneAngle = Math.toRadians(BetterSkylightConfig.AMBIENT_CONE_ANGLE_DEGREES.get());
        double coneRadius = ceilingDistance * Math.tan(coneAngle);
        double openness = Math.min(1.0D, coneRadius / openingDistance);
        int calculated = (int) Math.round(openness * 15.0D);

        return Math.max(0, Math.min(15, calculated));
    }

    private static int inferPropagatedOpeningDistance(int vanillaValue) {
        // Vanilla skylight loses one level per horizontal block after entering
        // through an opening. This gives an exact local distance for arbitrary
        // shapes, including holes which fall between our geometric rays.
        return vanillaValue > 0 ? Math.max(1, 15 - vanillaValue) : -1;
    }

    private static int nearestPositiveDistance(int first, int second) {
        if (first < 0) {
            return second;
        }
        if (second < 0) {
            return first;
        }
        return Math.min(first, second);
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

    private static OpeningSearch findNearestOpening(LevelReader level, BlockPos pos) {
        int maximumDistance = BetterSkylightConfig.ANALYSIS_DISTANCE.get();
        for (int radius = 1; radius <= maximumDistance; radius = nextRadius(radius, maximumDistance)) {
            for (int sample = 0; sample < ANGULAR_SAMPLES; sample++) {
                double angle = Math.PI * 2.0D * sample / ANGULAR_SAMPLES;
                int x = pos.getX() + (int) Math.round(Math.cos(angle) * radius);
                int z = pos.getZ() + (int) Math.round(Math.sin(angle) * radius);

                if (!hasLoadedChunk(level, x, z)) {
                    continue;
                }

                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                if (surfaceY <= pos.getY() + 1) {
                    return new OpeningSearch(radius);
                }
            }

            if (radius == maximumDistance) {
                break;
            }
        }

        return new OpeningSearch(-1);
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

    private record OpeningSearch(int distance) {
    }
}
