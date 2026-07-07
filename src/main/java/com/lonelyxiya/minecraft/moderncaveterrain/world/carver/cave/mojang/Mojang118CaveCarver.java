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
    private static final int SURFACE_WATER_SCAN_RADIUS = 2;
    private static final int SURFACE_WATER_SCAN_HEIGHT = 12;

    private final Mojang118CaveDensitySampler densitySampler;
    private final Mojang118AquiferSampler aquiferSampler;
    private final int bottomY;
    private final int topY;
    private final int surfaceCutoff;
    private final int priority;
    private final int seaLevel;
    private final int liquidAltitude;
    private final int localAquiferSurfaceBuffer;
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
        this.liquidAltitude = config.liquidAltitude.get();
        this.localAquiferSurfaceBuffer = Math.max(24, config.mojang118CaveSurfaceCutoffDepth.get() + 16);
        this.densityThreshold = config.mojang118CaveDensityThreshold.get();
        this.replaceFloatingGravel = config.replaceFloatingGravel.get();
        this.debugVisualizer = config.debugVisualizer.get();
        this.debugBlock = Blocks.REDSTONE_BLOCK.getDefaultState();
        this.densitySampler = new Mojang118CaveDensitySampler(
                world.getSeed(),
                config.mojang118CaveHorizontalScale.get(),
                config.mojang118CaveVerticalScale.get()
        );
        this.aquiferSampler = new Mojang118AquiferSampler(world.getSeed(), config.liquidAltitude.get());

        if (bottomY > topY) {
            ModernCaveTerrain.LOGGER.warn("Warning: Min altitude for 1.18-style caves should not be greater than max altitude.");
        }
    }

    public void carveColumn(ChunkPrimer primer, Mojang118NoiseChunk noiseChunk, int localX, int localZ, int blockX,
                            int blockZ, int topY, IBlockState liquidBlock, boolean flooded, Biome biome) {
        if (bottomY < 0 || bottomY > 255 || topY < 0 || topY > 255 || topY < bottomY) {
            return;
        }

        IBlockState airBlockState;
        boolean surfaceWaterRisk = !flooded && hasSurfaceWaterNearby(primer, localX, localZ, topY);
        boolean allowSurfaceEntrance = !flooded && !surfaceWaterRisk;

        for (int y = topY; y >= bottomY; y--) {
            double rawDensity = densitySampler.sampleDensity(blockX, y, blockZ);
            double density = densitySampler.applySurfaceAdjustment(rawDensity, blockX, y, blockZ, topY, bottomY,
                    surfaceCutoff, allowSurfaceEntrance, densityThreshold);

            boolean surfaceEntrance = allowSurfaceEntrance
                    && densitySampler.shouldCarveSurfaceEntrance(rawDensity, density, blockX, y, blockZ, topY,
                    densityThreshold);
            boolean digBlock = density <= densityThreshold || surfaceEntrance;
            if (debugVisualizer) {
                CarverUtils.debugDigBlockLocal(primer, localX, y, localZ, debugBlock, digBlock);
                continue;
            }

            if (!digBlock) {
                continue;
            }

            airBlockState = aquiferSampler.computeSubstance(blockX, y, blockZ, density, noiseChunk, seaLevel, flooded);
            if (airBlockState == null && surfaceEntrance) {
                airBlockState = Blocks.AIR.getDefaultState();
            }
            if (airBlockState == null) {
                continue;
            }
            if (surfaceWaterRisk && isNearSurface(y, topY) && wouldExposeSurfaceWater(primer, localX, y, localZ)) {
                continue;
            }
            if (airBlockState.getMaterial() == Material.WATER
                    && (surfaceEntrance || densitySampler.isSurfaceEntrance(blockX, y, blockZ, topY,
                    densityThreshold))) {
                airBlockState = Blocks.AIR.getDefaultState();
            }
            if (isExposedLocalWater(primer, localX, y, localZ, airBlockState, topY, flooded)) {
                continue;
            }
            if (isLeakingFluidToSurface(primer, localX, y, localZ, airBlockState, topY, flooded, surfaceEntrance)) {
                continue;
            }
            if (wouldExposePredictedCaveAir(primer, noiseChunk, localX, y, localZ, blockX, blockZ, airBlockState,
                    flooded)) {
                continue;
            }
            if (shouldSuppressUnstableFluid(primer, localX, y, localZ, airBlockState)) {
                continue;
            }
            CarverUtils.digBlockLocal(primer, localX, y, localZ, biome, airBlockState, null, -1, replaceFloatingGravel);
        }
    }

    private boolean isNearSurface(int y, int surfaceY) {
        return surfaceY - y <= surfaceCutoff + 12;
    }

    private boolean hasSurfaceWaterNearby(ChunkPrimer primer, int localX, int localZ, int surfaceY) {
        int minY = Math.max(1, surfaceY - SURFACE_WATER_SCAN_HEIGHT);
        int maxY = Math.min(255, surfaceY + SURFACE_WATER_SCAN_HEIGHT);
        for (int x = localX - SURFACE_WATER_SCAN_RADIUS; x <= localX + SURFACE_WATER_SCAN_RADIUS; x++) {
            for (int z = localZ - SURFACE_WATER_SCAN_RADIUS; z <= localZ + SURFACE_WATER_SCAN_RADIUS; z++) {
                for (int y = maxY; y >= minY; y--) {
                    if (isWater(primer, x, y, z)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean wouldExposeSurfaceWater(ChunkPrimer primer, int localX, int y, int localZ) {
        return isWaterOrOutside(primer, localX, y + 1, localZ)
                || isWaterOrOutside(primer, localX, y + 2, localZ)
                || isWaterOrOutside(primer, localX + 1, y, localZ)
                || isWaterOrOutside(primer, localX - 1, y, localZ)
                || isWaterOrOutside(primer, localX, y, localZ + 1)
                || isWaterOrOutside(primer, localX, y, localZ - 1);
    }

    private boolean isWater(ChunkPrimer primer, int localX, int y, int localZ) {
        if (localX < 0 || localX > 15 || y < 0 || y > 255 || localZ < 0 || localZ > 15) {
            return false;
        }

        return primer.getBlockState(localX, y, localZ).getMaterial() == Material.WATER;
    }

    private boolean isWaterOrOutside(ChunkPrimer primer, int localX, int y, int localZ) {
        if (localX < 0 || localX > 15 || y < 0 || y > 255 || localZ < 0 || localZ > 15) {
            return true;
        }

        return primer.getBlockState(localX, y, localZ).getMaterial() == Material.WATER;
    }

    private boolean isLeakingFluidToSurface(ChunkPrimer primer, int localX, int y, int localZ, IBlockState state,
                                            int surfaceY, boolean flooded, boolean surfaceEntrance) {
        if (flooded || state.getMaterial() != Material.WATER) {
            return false;
        }
        int leakDepth = surfaceEntrance ? 44 : 18;
        if (surfaceY - y > leakDepth) {
            return false;
        }

        return isAirOrOutside(primer, localX, y + 1, localZ)
                || isAirOrOutside(primer, localX, y + 2, localZ)
                || isAirOrOutside(primer, localX + 1, y, localZ)
                || isAirOrOutside(primer, localX - 1, y, localZ)
                || isAirOrOutside(primer, localX, y, localZ + 1)
                || isAirOrOutside(primer, localX, y, localZ - 1)
                || isAirOrOutside(primer, localX + 1, y + 1, localZ)
                || isAirOrOutside(primer, localX - 1, y + 1, localZ)
                || isAirOrOutside(primer, localX, y + 1, localZ + 1)
                || isAirOrOutside(primer, localX, y + 1, localZ - 1);
    }

    private boolean isExposedLocalWater(ChunkPrimer primer, int localX, int y, int localZ, IBlockState state,
                                        int surfaceY, boolean flooded) {
        if (flooded || state.getMaterial() != Material.WATER) {
            return false;
        }
        if (surfaceY - y <= localAquiferSurfaceBuffer) {
            return true;
        }
        if (!aquiferSampler.shouldScheduleFluidUpdate()) {
            return false;
        }

        return isAirOrOutside(primer, localX, y + 1, localZ)
                || isAirOrOutside(primer, localX + 1, y, localZ)
                || isAirOrOutside(primer, localX - 1, y, localZ)
                || isAirOrOutside(primer, localX, y, localZ + 1)
                || isAirOrOutside(primer, localX, y, localZ - 1);
    }

    private boolean isAirOrOutside(ChunkPrimer primer, int localX, int y, int localZ) {
        if (localX < 0 || localX > 15 || y < 0 || y > 255 || localZ < 0 || localZ > 15) {
            return true;
        }

        return primer.getBlockState(localX, y, localZ).getBlock() == Blocks.AIR;
    }

    private boolean wouldExposePredictedCaveAir(ChunkPrimer primer, Mojang118NoiseChunk noiseChunk, int localX, int y,
                                                int localZ, int blockX, int blockZ, IBlockState state,
                                                boolean flooded) {
        if (state.getMaterial() != Material.WATER && state.getMaterial() != Material.LAVA) {
            return false;
        }

        if (wouldNeighborBecomeAir(primer, noiseChunk, localX + 1, y, localZ, blockX + 1, y, blockZ,
                flooded)
                || wouldNeighborBecomeAir(primer, noiseChunk, localX - 1, y, localZ, blockX - 1, y, blockZ,
                flooded)
                || wouldNeighborBecomeAir(primer, noiseChunk, localX, y, localZ + 1, blockX, y, blockZ + 1,
                flooded)
                || wouldNeighborBecomeAir(primer, noiseChunk, localX, y, localZ - 1, blockX, y, blockZ - 1,
                flooded)) {
            return true;
        }

        return wouldNeighborBecomeAir(primer, noiseChunk, localX, y - 1, localZ, blockX, y - 1, blockZ,
                flooded);
    }

    private boolean wouldNeighborBecomeAir(ChunkPrimer primer, Mojang118NoiseChunk noiseChunk, int localX, int y,
                                           int localZ, int blockX, int blockY, int blockZ, boolean flooded) {
        if (y < bottomY || y > topY || y < 0 || y > 255) {
            return false;
        }
        if (localX < 0 || localX > 15 || localZ < 0 || localZ > 15) {
            return false;
        }
        if (primer.getBlockState(localX, y, localZ).getBlock() == Blocks.AIR) {
            return true;
        }

        double rawDensity = densitySampler.sampleDensity(blockX, blockY, blockZ);
        boolean allowSurfaceEntrance = !flooded;
        int neighborSurfaceY = Math.min(topY, noiseChunk.surfaceY(blockX, blockZ));
        double density = densitySampler.applySurfaceAdjustment(rawDensity, blockX, blockY, blockZ, neighborSurfaceY,
                bottomY, surfaceCutoff, allowSurfaceEntrance, densityThreshold);
        boolean surfaceEntrance = allowSurfaceEntrance
                && densitySampler.shouldCarveSurfaceEntrance(rawDensity, density, blockX, blockY, blockZ,
                neighborSurfaceY,
                densityThreshold);
        if (density > densityThreshold && !surfaceEntrance) {
            return false;
        }

        IBlockState predictedState = aquiferSampler.previewSubstance(blockX, blockY, blockZ, density, noiseChunk,
                seaLevel, flooded);
        return predictedState == null || predictedState.getBlock() == Blocks.AIR;
    }

    private boolean shouldSuppressUnstableFluid(ChunkPrimer primer, int localX, int y, int localZ, IBlockState state) {
        if (state.getMaterial() != Material.LAVA && state.getMaterial() != Material.WATER) {
            return false;
        }

        if (state.getMaterial() == Material.LAVA && y > liquidAltitude) {
            return true;
        }

        if (!aquiferSampler.shouldScheduleFluidUpdate()) {
            return false;
        }

        if (state.getMaterial() == Material.LAVA) {
            return isOppositeFluidAdjacent(primer, localX, y, localZ, state)
                    || isAirOrOutside(primer, localX, y + 1, localZ)
                    || isAirOrOutside(primer, localX + 1, y, localZ)
                    || isAirOrOutside(primer, localX - 1, y, localZ)
                    || isAirOrOutside(primer, localX, y, localZ + 1)
                    || isAirOrOutside(primer, localX, y, localZ - 1);
        }

        return isOppositeFluidAdjacent(primer, localX, y, localZ, state);
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
