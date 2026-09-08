package com.goldenatom.betterskylight;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Cached per-block ambient sky exposure. This first implementation uses the
 * nearest overhead blocker and sampled verified sky openings around it.
 */
public final class SkyExposureCache {
    private static final int ANGULAR_SAMPLES = 16;
    private static final Map<BlockGetter, ConcurrentMap<Long, Byte>> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private SkyExposureCache() {
    }

    public static int getSkyLight(BlockGetter level, BlockPos pos, int vanillaValue) {
        if (!BetterSkylightConfig.ENABLED.get()) {
            return vanillaValue;
        }

        Level world = findLevel(level);
        if (world == null) {
            return vanillaValue;
        }

        ConcurrentMap<Long, Byte> levelCache;
        synchronized (CACHE) {
            levelCache = CACHE.computeIfAbsent(level, ignored -> new ConcurrentHashMap<>());
        }

        long key = pos.asLong();
        Byte cached = levelCache.get(key);
        if (cached != null) {
            return cached & 0xFF;
        }

        int calculated = calculate(world, pos, vanillaValue);
        Byte raced = levelCache.putIfAbsent(key, (byte) calculated);
        return raced == null ? calculated : raced & 0xFF;
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

    private static Level findLevel(BlockGetter level) {
        if (level instanceof Level world) {
            return world;
        }
        return null;
    }

    private static int calculate(Level level, BlockPos pos, int vanillaValue) {
        LevelChunk originChunk = getLoadedChunk(level, pos.getX(), pos.getZ());
        if (originChunk == null) {
            return vanillaValue;
        }

        int ceilingDistance = findCeilingDistance(level, originChunk, pos);
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

        int ambientMinimum = Math.max(0, Math.min(15, calculated));
        return Math.max(vanillaValue, ambientMinimum);
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

    private static int findCeilingDistance(Level level, LevelChunk chunk, BlockPos pos) {
        int surfaceY = chunk.getHeight(
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
            if (chunk.getBlockState(cursor).getLightBlock(level, cursor) > 0) {
                return y - pos.getY();
            }
        }

        return -1;
    }

    private static OpeningSearch findNearestOpening(Level level, BlockPos pos) {
        int maximumDistance = BetterSkylightConfig.ANALYSIS_DISTANCE.get();
        for (int radius = 1; radius <= maximumDistance; radius = nextRadius(radius, maximumDistance)) {
            for (int sample = 0; sample < ANGULAR_SAMPLES; sample++) {
                double angle = Math.PI * 2.0D * sample / ANGULAR_SAMPLES;
                int x = pos.getX() + (int) Math.round(Math.cos(angle) * radius);
                int z = pos.getZ() + (int) Math.round(Math.sin(angle) * radius);

                LevelChunk chunk = getLoadedChunk(level, x, z);
                if (chunk == null) {
                    continue;
                }

                int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
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

    private static LevelChunk getLoadedChunk(Level level, int blockX, int blockZ) {
        int chunkX = SectionPos.blockToSectionCoord(blockX);
        int chunkZ = SectionPos.blockToSectionCoord(blockZ);
        // getChunkNow is the non-blocking path on both client and server. Never
        // ask Level#getChunk here: this method is also called from C2ME chunk
        // workers, where waiting for another chunk can deadlock generation.
        return level.getChunkSource().getChunkNow(chunkX, chunkZ);
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
