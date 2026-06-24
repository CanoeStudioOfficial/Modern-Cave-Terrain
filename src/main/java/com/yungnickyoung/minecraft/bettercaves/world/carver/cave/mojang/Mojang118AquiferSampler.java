package com.yungnickyoung.minecraft.bettercaves.world.carver.cave.mojang;

import com.yungnickyoung.minecraft.bettercaves.noise.MojangNormalNoise;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

/**
 * Lightweight aquifer approximation based on the modern overworld fluid-level noises.
 *
 * <p>This keeps Better Caves' 1.12 carving pipeline, but replaces the old single liquid altitude with
 * per-position water pockets and deep lava decisions derived from Mojang's aquifer noise parameters.</p>
 */
public class Mojang118AquiferSampler {
    private static final double MOJANG_MIN_Y = -64.0D;
    private static final double MOJANG_HEIGHT = 384.0D;
    private static final double OLD_WORLD_MAX_Y = 255.0D;
    private static final int MODERN_SEA_LEVEL = 63;
    private static final int MODERN_LAVA_LEVEL = -54;
    private static final int NO_FLUID_LEVEL = Integer.MIN_VALUE / 4;

    private final MojangNormalNoise floodednessNoise;
    private final MojangNormalNoise fluidLevelSpreadNoise;
    private final MojangNormalNoise lavaNoise;

    public Mojang118AquiferSampler(long seed) {
        this.floodednessNoise = MojangNormalNoise.create(seed, "aquifer_fluid_level_floodedness", -7, 1.0D);
        this.fluidLevelSpreadNoise = MojangNormalNoise.create(seed, "aquifer_fluid_level_spread", -5, 1.0D);
        this.lavaNoise = MojangNormalNoise.create(seed, "aquifer_lava", -1, 1.0D);
    }

    public IBlockState sampleFluidState(int blockX, int blockY, int blockZ, int surfaceY, int seaLevel,
                                        IBlockState columnLiquidBlock, boolean flooded) {
        double mojangY = toMojangY(blockY);
        if (mojangY <= MODERN_LAVA_LEVEL) {
            return lavaState(columnLiquidBlock);
        }

        int surfaceMojangY = (int) Math.floor(toMojangY(surfaceY));
        boolean surfaceUnderWater = flooded || surfaceY <= seaLevel;
        int fluidLevel = computeSurfaceLevel(blockX, mojangY, blockZ, surfaceMojangY, surfaceUnderWater);
        if (fluidLevel == NO_FLUID_LEVEL || mojangY > fluidLevel) {
            return null;
        }

        return computeFluidType(blockX, mojangY, blockZ, columnLiquidBlock, fluidLevel);
    }

    private int computeSurfaceLevel(int blockX, double mojangY, int blockZ, int surfaceMojangY,
                                    boolean surfaceUnderWater) {
        double fluidDepth = surfaceMojangY + 8.0D - mojangY;
        double floodednessFactor = surfaceUnderWater ? clampedMap(fluidDepth, 0.0D, 64.0D, 1.0D, 0.0D) : 0.0D;
        double floodedness = clamp(sample(floodednessNoise, blockX, mojangY, blockZ, 1.0D, 0.67D), -1.0D, 1.0D);
        double fullyFloodedThreshold = lerp(1.0D - floodednessFactor, -0.3D, 0.8D);
        double partiallyFloodedThreshold = lerp(1.0D - floodednessFactor, -0.8D, 0.4D);
        double partiallyFloodedness = floodedness - partiallyFloodedThreshold;
        double fullyFloodedness = floodedness - fullyFloodedThreshold;

        if (fullyFloodedness > 0.0D) {
            return MODERN_SEA_LEVEL;
        }
        if (partiallyFloodedness > 0.0D) {
            return computeRandomizedFluidSurfaceLevel(blockX, mojangY, blockZ, surfaceMojangY);
        }

        return NO_FLUID_LEVEL;
    }

    private int computeRandomizedFluidSurfaceLevel(int blockX, double mojangY, int blockZ, int surfaceMojangY) {
        int cellX = floorDiv(blockX, 16);
        int cellY = floorDiv((int) Math.floor(mojangY), 40);
        int cellZ = floorDiv(blockZ, 16);
        int cellMiddleY = cellY * 40 + 20;
        double spread = fluidLevelSpreadNoise.getValue(cellX, cellY, cellZ) * 10.0D;
        int quantizedSpread = quantize(spread, 3);
        int targetFluidSurfaceLevel = cellMiddleY + quantizedSpread;

        return Math.min(surfaceMojangY, targetFluidSurfaceLevel);
    }

    private IBlockState computeFluidType(int blockX, double mojangY, int blockZ, IBlockState columnLiquidBlock,
                                         int fluidSurfaceLevel) {
        if (fluidSurfaceLevel <= -10) {
            int cellX = floorDiv(blockX, 64);
            int cellY = floorDiv((int) Math.floor(mojangY), 40);
            int cellZ = floorDiv(blockZ, 64);
            double lavaNoiseValue = lavaNoise.getValue(cellX, cellY, cellZ);
            if (Math.abs(lavaNoiseValue) > 0.3D) {
                return lavaState(columnLiquidBlock);
            }
        }

        return Blocks.WATER.getDefaultState();
    }

    private IBlockState lavaState(IBlockState columnLiquidBlock) {
        if (columnLiquidBlock != null && columnLiquidBlock.getMaterial().isLiquid()) {
            return columnLiquidBlock;
        }

        return Blocks.LAVA.getDefaultState();
    }

    private double sample(MojangNormalNoise noise, int blockX, double mojangY, int blockZ, double xzScale, double yScale) {
        return noise.getValue(blockX * xzScale, mojangY * yScale, blockZ * xzScale);
    }

    private static double toMojangY(int blockY) {
        return MOJANG_MIN_Y + blockY / OLD_WORLD_MAX_Y * MOJANG_HEIGHT;
    }

    private static int floorDiv(int x, int y) {
        int result = x / y;
        if ((x ^ y) < 0 && result * y != x) {
            result--;
        }
        return result;
    }

    private static int quantize(double value, int resolution) {
        return (int) Math.floor(value / (double) resolution) * resolution;
    }

    private static double clampedMap(double value, double oldMin, double oldMax, double newMin, double newMax) {
        return lerp(clamp((value - oldMin) / (oldMax - oldMin), 0.0D, 1.0D), newMin, newMax);
    }

    private static double lerp(double factor, double from, double to) {
        return from + factor * (to - from);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
