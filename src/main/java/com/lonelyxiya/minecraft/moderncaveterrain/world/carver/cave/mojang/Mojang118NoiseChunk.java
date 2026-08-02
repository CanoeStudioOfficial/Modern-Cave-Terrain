package com.lonelyxiya.minecraft.moderncaveterrain.world.carver.cave.mojang;

import com.lonelyxiya.minecraft.moderncaveterrain.world.ChunkCaveContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Small 1.12.2 equivalent of the modern NoiseChunk surface data used by aquifers.
 *
 * <p>This intentionally implements only the pieces needed by the aquifer port: cached preliminary surface
 * sampling, adjusted surface level, and modern coordinate conversion.</p>
 */
public final class Mojang118NoiseChunk {
    static final int[][] SURFACE_SAMPLING_OFFSETS_IN_CHUNKS = new int[][] {
            {0, 0}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0},
            {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}
    };

    private static final double MOJANG_MIN_Y = -64.0D;
    private static final double MOJANG_HEIGHT = 384.0D;
    private static final double OLD_WORLD_MAX_Y = 255.0D;
    private static final int CHUNK_SIZE = 16;

    private final World world;
    private final ChunkCaveContext chunkContext;
    private final int chunkMinBlockX;
    private final int chunkMinBlockZ;
    private final int singleColumnSurfaceY;
    private final Mojang118PreliminarySurfaceSampler preliminarySurfaceSampler;
    private final Map<Long, Integer> preliminarySurfaceCache = new HashMap<>();
    private final BitSet fluidPostProcessingPositions = new BitSet(16 * 256 * 16);

    Mojang118NoiseChunk(World world, ChunkCaveContext chunkContext,
                        Mojang118PreliminarySurfaceSampler preliminarySurfaceSampler) {
        this.world = world;
        this.chunkContext = chunkContext;
        this.chunkMinBlockX = chunkContext.getChunkX() * CHUNK_SIZE;
        this.chunkMinBlockZ = chunkContext.getChunkZ() * CHUNK_SIZE;
        this.singleColumnSurfaceY = -1;
        this.preliminarySurfaceSampler = preliminarySurfaceSampler;
    }

    public Mojang118NoiseChunk(World world, int blockX, int blockZ, int surfaceY) {
        this.world = world;
        this.chunkContext = null;
        this.chunkMinBlockX = blockX & ~15;
        this.chunkMinBlockZ = blockZ & ~15;
        this.singleColumnSurfaceY = surfaceY;
        this.preliminarySurfaceSampler = null;
    }

    public int preliminarySurfaceLevel(int sampleX, int sampleZ) {
        int quantizedX = quantizeToQuartBlock(sampleX);
        int quantizedZ = quantizeToQuartBlock(sampleZ);
        long key = columnKey(quantizedX, quantizedZ);
        Integer cached = preliminarySurfaceCache.get(key);
        if (cached != null) {
            return cached;
        }

        int computed = computePreliminarySurfaceLevel(quantizedX, quantizedZ);
        preliminarySurfaceCache.put(key, computed);
        return computed;
    }

    public int adjustedSurfaceLevel(int preliminarySurfaceLevel) {
        return preliminarySurfaceLevel + 8;
    }

    public int maxPreliminarySurfaceLevel(int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        int maxY = Integer.MIN_VALUE;
        for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ += 4) {
            for (int blockX = minBlockX; blockX <= maxBlockX; blockX += 4) {
                maxY = Math.max(maxY, preliminarySurfaceLevel(blockX, blockZ));
            }
        }
        return maxY;
    }

    public int surfaceY(int blockX, int blockZ) {
        if (chunkContext != null) {
            int localX = clamp(blockX - chunkMinBlockX, 0, CHUNK_SIZE - 1);
            int localZ = clamp(blockZ - chunkMinBlockZ, 0, CHUNK_SIZE - 1);
            return chunkContext.getSurfaceAltitude(localX, localZ);
        }

        if (world != null) {
            return world.getHeight(new BlockPos(blockX, 0, blockZ)).getY();
        }

        return singleColumnSurfaceY;
    }

    public static double toMojangY(int blockY) {
        return MOJANG_MIN_Y + blockY / OLD_WORLD_MAX_Y * MOJANG_HEIGHT;
    }

    void markFluidForPostProcessing(int localX, int blockY, int localZ) {
        fluidPostProcessingPositions.set(Mojang118FluidPostProcessor.positionIndex(localX, blockY, localZ));
    }

    public void enqueueFluidPostProcessing() {
        if (chunkContext == null || fluidPostProcessingPositions.isEmpty()) {
            return;
        }

        Mojang118FluidPostProcessor.enqueue(world, chunkContext.getChunkX(), chunkContext.getChunkZ(),
                fluidPostProcessingPositions);
        fluidPostProcessingPositions.clear();
    }

    private int computePreliminarySurfaceLevel(int blockX, int blockZ) {
        int surfaceY = preliminarySurfaceSampler != null
                ? preliminarySurfaceSampler.sampleSurfaceY(blockX, blockZ)
                : clamp(surfaceY(blockX, blockZ), 0, 255);
        return (int) Math.floor(toMojangY(surfaceY));
    }

    private static int blockToQuart(int blockCoord) {
        return floorDiv(blockCoord, 4);
    }

    private static int quartToBlock(int quartCoord) {
        return quartCoord * 4;
    }

    static int quantizeToQuartBlock(int blockCoord) {
        return quartToBlock(blockToQuart(blockCoord));
    }

    private static long columnKey(int blockX, int blockZ) {
        return (((long) blockX) << 32) ^ (((long) blockZ) & 0xffffffffL);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int floorDiv(int x, int y) {
        int result = x / y;
        if ((x ^ y) < 0 && result * y != x) {
            result--;
        }
        return result;
    }
}
