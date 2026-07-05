package com.lonelyxiya.minecraft.moderncaveterrain.world.carver.cave.mojang;

import com.lonelyxiya.minecraft.moderncaveterrain.ModernCaveTerrain;
import com.lonelyxiya.minecraft.moderncaveterrain.config.util.ConfigHolder;
import com.lonelyxiya.minecraft.moderncaveterrain.world.carver.CarverUtils;
import com.lonelyxiya.minecraft.moderncaveterrain.world.carver.ICarver;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

/**
 * Carves a 1.18-style cave density field into a 1.12.2 ChunkPrimer.
 */
public class Mojang118CaveCarver implements ICarver {
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
            ModernCaveTerrain.LOGGER.warn("Warning: Min altitude for 1.18-style caves should not be greater than max altitude.");
        }
    }

    public void carveColumn(ChunkPrimer primer, int localX, int localZ, int blockX, int blockZ, int topY,
                            IBlockState liquidBlock, boolean flooded, Biome biome) {
        if (bottomY < 0 || bottomY > 255 || topY < 0 || topY > 255 || topY < bottomY) {
            return;
        }

        IBlockState airBlockState;
        boolean allowSurfaceEntrance = !flooded;

        for (int y = topY; y >= bottomY; y--) {
            double density = densitySampler.sampleDensity(blockX, y, blockZ);
            density = densitySampler.applySurfaceAdjustment(density, blockX, y, blockZ, topY, bottomY,
                    surfaceCutoff, allowSurfaceEntrance);

            boolean digBlock = density <= densityThreshold;
            if (debugVisualizer) {
                CarverUtils.debugDigBlockLocal(primer, localX, y, localZ, debugBlock, digBlock);
                continue;
            }

            if (!digBlock) {
                continue;
            }

            airBlockState = aquiferSampler.computeSubstance(blockX, y, blockZ, density, topY, seaLevel, flooded);
            if (airBlockState == null) {
                continue;
            }
            CarverUtils.digBlockLocal(primer, localX, y, localZ, biome, airBlockState, null, -1, replaceFloatingGravel);
        }
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
