package com.yungnickyoung.minecraft.bettercaves.world.carver.cave.mojang;

import com.yungnickyoung.minecraft.bettercaves.BetterCaves;
import com.yungnickyoung.minecraft.bettercaves.config.util.ConfigHolder;
import com.yungnickyoung.minecraft.bettercaves.world.carver.CarverUtils;
import com.yungnickyoung.minecraft.bettercaves.world.carver.ICarver;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Arrays;

/**
 * Carves a 1.18-style cave density field into a 1.12.2 ChunkPrimer.
 */
public class Mojang118CaveCarver implements ICarver {
    private static final int CHUNK_SIZE = 16;
    private static final int WORLD_HEIGHT = 256;
    private static final int FLUID_FILL_VOLUME = CHUNK_SIZE * WORLD_HEIGHT * CHUNK_SIZE;

    private final Mojang118CaveDensitySampler densitySampler;
    private final Mojang118AquiferSampler aquiferSampler;
    private final int bottomY;
    private final int topY;
    private final int surfaceCutoff;
    private final int priority;
    private final int seaLevel;
    private final double densityThreshold;
    private final boolean replaceFloatingGravel;
    private final boolean debugVisualizer;
    private final IBlockState debugBlock;

    public Mojang118CaveCarver(World world, ConfigHolder config) {
        this.bottomY = config.mojang118CaveBottom.get();
        this.topY = config.mojang118CaveTop.get();
        this.surfaceCutoff = config.mojang118CaveSurfaceCutoffDepth.get();
        this.priority = config.mojang118CavePriority.get();
        this.seaLevel = world.getSeaLevel();
        this.densityThreshold = config.mojang118CaveDensityThreshold.get();
        this.replaceFloatingGravel = config.replaceFloatingGravel.get();
        this.debugVisualizer = config.debugVisualizer.get();
        this.debugBlock = Blocks.REDSTONE_BLOCK.getDefaultState();
        this.densitySampler = new Mojang118CaveDensitySampler(
                world.getSeed(),
                config.mojang118CaveHorizontalScale.get(),
                config.mojang118CaveVerticalScale.get()
        );
        this.aquiferSampler = new Mojang118AquiferSampler(world.getSeed());

        if (bottomY > topY) {
            BetterCaves.LOGGER.warn("Warning: Min altitude for 1.18-style caves should not be greater than max altitude.");
        }
    }

    public void carveColumn(ChunkPrimer primer, int localX, int localZ, int blockX, int blockZ, int topY,
                            IBlockState liquidBlock, boolean flooded, Biome biome) {
        if (bottomY < 0 || bottomY > 255 || topY < 0 || topY > 255 || topY < bottomY) {
            return;
        }

        IBlockState airState = Blocks.AIR.getDefaultState();
        IBlockState waterState = Blocks.WATER.getDefaultState();
        IBlockState airBlockState;
        IBlockState fluidState;
        int transitionBoundary = Math.max(bottomY, topY - surfaceCutoff);
        int transitionHeight = Math.max(1, topY - transitionBoundary);

        for (int y = topY; y >= bottomY; y--) {
            double density = densitySampler.sampleDensity(blockX, y, blockZ);
            if (y >= transitionBoundary) {
                double surfaceFactor = (double) (y - transitionBoundary) / transitionHeight;
                density += surfaceFactor * 0.45D;
            }

            boolean digBlock = density <= densityThreshold;
            if (debugVisualizer) {
                CarverUtils.debugDigBlockLocal(primer, localX, y, localZ, debugBlock, digBlock);
                continue;
            }

            if (!digBlock) {
                continue;
            }

            fluidState = flooded && y < seaLevel
                    ? waterState
                    : aquiferSampler.sampleFluidState(blockX, y, blockZ, topY, seaLevel, flooded);
            if (fluidState != null && isOppositeFluidAdjacent(primer, localX, y, localZ, fluidState)) {
                continue;
            }
            airBlockState = fluidState == null ? airState : fluidState;
            CarverUtils.digBlockLocal(primer, localX, y, localZ, biome, airBlockState, null, -1, replaceFloatingGravel);
        }
    }

    public void completeAquiferFluidBodies(ChunkPrimer primer, int baseBlockX, int baseBlockZ, int[] topYs,
                                           boolean[] floodedColumns) {
        if (debugVisualizer) {
            return;
        }

        boolean[] fillMask = new boolean[FLUID_FILL_VOLUME];
        completeSampledFluidGaps(primer, baseBlockX, baseBlockZ, topYs, floodedColumns, Blocks.WATER.getDefaultState(), Material.WATER, fillMask);
        Arrays.fill(fillMask, false);
        completeSampledFluidGaps(primer, baseBlockX, baseBlockZ, topYs, floodedColumns, Blocks.LAVA.getDefaultState(), Material.LAVA, fillMask);
    }

    private void completeSampledFluidGaps(ChunkPrimer primer, int baseBlockX, int baseBlockZ, int[] topYs,
                                          boolean[] floodedColumns, IBlockState fluidState, Material fluidMaterial,
                                          boolean[] fillMask) {
        int minY = Math.max(0, bottomY);

        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {
                int columnIndex = columnIndex(localX, localZ);
                int columnTopY = topYs[columnIndex];
                if (columnTopY < minY) {
                    continue;
                }

                int maxY = Math.min(Math.min(WORLD_HEIGHT - 1, topY), columnTopY);
                int blockX = baseBlockX + localX;
                int blockZ = baseBlockZ + localZ;

                for (int y = maxY; y >= minY; y--) {
                    if (primer.getBlockState(localX, y, localZ).getMaterial() != Material.AIR) {
                        continue;
                    }

                    IBlockState sampledState = aquiferSampler.sampleFluidState(blockX, y, blockZ, columnTopY, seaLevel, floodedColumns[columnIndex]);
                    if (sampledState == null || sampledState.getMaterial() != fluidMaterial) {
                        continue;
                    }
                    if (!isSameFluidAdjacent(primer, localX, y, localZ, fluidMaterial)) {
                        continue;
                    }
                    if (isOppositeFluidAdjacent(primer, localX, y, localZ, fluidState)) {
                        continue;
                    }

                    fillMask[index(localX, y, localZ)] = true;
                }
            }
        }

        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {
                for (int y = minY; y < WORLD_HEIGHT; y++) {
                    int index = index(localX, y, localZ);
                    if (fillMask[index]) {
                        primer.setBlockState(localX, y, localZ, fluidState);
                    }
                }
            }
        }
    }

    private boolean isSameFluidAdjacent(ChunkPrimer primer, int localX, int y, int localZ, Material fluidMaterial) {
        return isSameFluid(primer, localX, y + 1, localZ, fluidMaterial)
                || isSameFluid(primer, localX, y - 1, localZ, fluidMaterial)
                || isSameFluid(primer, localX + 1, y, localZ, fluidMaterial)
                || isSameFluid(primer, localX - 1, y, localZ, fluidMaterial)
                || isSameFluid(primer, localX, y, localZ + 1, fluidMaterial)
                || isSameFluid(primer, localX, y, localZ - 1, fluidMaterial);
    }

    private boolean isSameFluid(ChunkPrimer primer, int localX, int y, int localZ, Material fluidMaterial) {
        if (localX < 0 || localX >= CHUNK_SIZE || y < 0 || y >= WORLD_HEIGHT || localZ < 0 || localZ >= CHUNK_SIZE) {
            return false;
        }

        return primer.getBlockState(localX, y, localZ).getMaterial() == fluidMaterial;
    }

    private int columnIndex(int localX, int localZ) {
        return localX * CHUNK_SIZE + localZ;
    }

    private int index(int localX, int y, int localZ) {
        return (localX * WORLD_HEIGHT + y) * CHUNK_SIZE + localZ;
    }

    private boolean isOppositeFluidAdjacent(ChunkPrimer primer, int localX, int y, int localZ, IBlockState fluidState) {
        return isOppositeFluid(primer, localX, y + 1, localZ, fluidState)
                || isOppositeFluid(primer, localX, y - 1, localZ, fluidState)
                || isOppositeFluid(primer, localX + 1, y, localZ, fluidState)
                || isOppositeFluid(primer, localX - 1, y, localZ, fluidState)
                || isOppositeFluid(primer, localX, y, localZ + 1, fluidState)
                || isOppositeFluid(primer, localX, y, localZ - 1, fluidState);
    }

    private boolean isOppositeFluid(ChunkPrimer primer, int localX, int y, int localZ, IBlockState fluidState) {
        if (localX < 0 || localX > 15 || y < 0 || y > 255 || localZ < 0 || localZ > 15) {
            return false;
        }

        Material material = primer.getBlockState(localX, y, localZ).getMaterial();
        Material targetMaterial = fluidState.getMaterial();

        return targetMaterial == Material.WATER && material == Material.LAVA
                || targetMaterial == Material.LAVA && material == Material.WATER;
    }

    public int getPriority() {
        return priority;
    }

    public int getBottomY() {
        return bottomY;
    }

    public int getTopY() {
        return topY;
    }
}
