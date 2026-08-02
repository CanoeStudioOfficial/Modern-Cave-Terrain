package com.lonelyxiya.minecraft.moderncaveterrain.world.carver.cave.mojang;

import com.lonelyxiya.minecraft.moderncaveterrain.ModernCaveTerrain;
import com.lonelyxiya.minecraft.moderncaveterrain.config.util.ConfigHolder;
import com.lonelyxiya.minecraft.moderncaveterrain.world.ChunkCaveContext;
import com.lonelyxiya.minecraft.moderncaveterrain.world.carver.CarverUtils;
import com.lonelyxiya.minecraft.moderncaveterrain.world.carver.ICarver;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSand;
import net.minecraft.block.BlockStone;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraftforge.common.BiomeDictionary;

/**
 * Carves a 1.18-style cave density field into a 1.12.2 ChunkPrimer.
 */
public class Mojang118CaveCarver implements ICarver {
    private static final int SURFACE_WATER_SCAN_RADIUS = 2;
    private static final int SURFACE_WATER_SCAN_HEIGHT = 12;
    private static final IBlockState AIR = Blocks.AIR.getDefaultState();
    private static final IBlockState SAND = Blocks.SAND.getDefaultState();
    private static final IBlockState RED_SAND = Blocks.SAND.getDefaultState()
            .withProperty(BlockSand.VARIANT, BlockSand.EnumType.RED_SAND);
    private static final IBlockState SANDSTONE = Blocks.SANDSTONE.getDefaultState();
    private static final IBlockState RED_SANDSTONE = Blocks.RED_SANDSTONE.getDefaultState();
    private static final IBlockState GRAVEL = Blocks.GRAVEL.getDefaultState();
    private static final IBlockState ANDESITE = Blocks.STONE.getDefaultState()
            .withProperty(BlockStone.VARIANT, BlockStone.EnumType.ANDESITE);

    private final Mojang118CaveDensitySampler densitySampler;
    private final Mojang118AquiferSampler aquiferSampler;
    private final Mojang118PreliminarySurfaceSampler preliminarySurfaceSampler;
    private final World world;
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
        this.world = world;
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
        this.aquiferSampler = new Mojang118AquiferSampler(world.getSeed(), config.liquidAltitude.get(),
                config.enableFloodedUnderground.get());
        this.preliminarySurfaceSampler = new Mojang118PreliminarySurfaceSampler(world);

        if (bottomY > topY) {
            ModernCaveTerrain.LOGGER.warn("Warning: Min altitude for 1.18-style caves should not be greater than max altitude.");
        }
    }

    public Mojang118NoiseChunk createNoiseChunk(ChunkCaveContext chunkContext) {
        aquiferSampler.resetForChunk();
        densitySampler.resetForChunk();
        return new Mojang118NoiseChunk(world, chunkContext, preliminarySurfaceSampler);
    }

    public void carveColumn(ChunkPrimer primer, Mojang118NoiseChunk noiseChunk, int localX, int localZ, int blockX,
                            int blockZ, int topY, IBlockState liquidBlock, boolean flooded, Biome biome) {
        if (bottomY < 0 || bottomY > 255 || topY < 0 || topY > 255 || topY < bottomY) {
            return;
        }

        IBlockState airBlockState;
        boolean surfaceWaterRisk = hasSurfaceWaterNearby(primer, localX, localZ, blockX, blockZ, topY);
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

            double aquiferDensity = density - densityThreshold;
            airBlockState = aquiferSampler.computeSubstance(blockX, y, blockZ, aquiferDensity, noiseChunk,
                    seaLevel, flooded);
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
            boolean replaced = digModernBlockLocal(primer, localX, y, localZ, biome, airBlockState);
            if (replaced && aquiferSampler.shouldScheduleFluidUpdate()
                    && (airBlockState.getMaterial() == Material.WATER
                    || airBlockState.getMaterial() == Material.LAVA)) {
                noiseChunk.markFluidForPostProcessing(localX, y, localZ);
            }
        }
    }

    private boolean isNearSurface(int y, int surfaceY) {
        return surfaceY - y <= surfaceCutoff + 12;
    }

    private boolean hasSurfaceWaterNearby(ChunkPrimer primer, int localX, int localZ, int blockX, int blockZ,
                                          int surfaceY) {
        int minY = Math.max(1, surfaceY - SURFACE_WATER_SCAN_HEIGHT);
        int maxY = Math.min(255, surfaceY + SURFACE_WATER_SCAN_HEIGHT);
        BlockPos.MutableBlockPos biomePos = new BlockPos.MutableBlockPos();
        for (int x = localX - SURFACE_WATER_SCAN_RADIUS; x <= localX + SURFACE_WATER_SCAN_RADIUS; x++) {
            for (int z = localZ - SURFACE_WATER_SCAN_RADIUS; z <= localZ + SURFACE_WATER_SCAN_RADIUS; z++) {
                if (x < 0 || x > 15 || z < 0 || z > 15) {
                    biomePos.setPos(blockX + x - localX, 1, blockZ + z - localZ);
                    Biome neighborBiome = world.getBiomeProvider().getBiome(biomePos);
                    if (BiomeDictionary.hasType(neighborBiome, BiomeDictionary.Type.WATER)) {
                        return true;
                    }
                    continue;
                }
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

    private boolean digModernBlockLocal(ChunkPrimer primer, int localX, int y, int localZ, Biome biome,
                                        IBlockState replacement) {
        IBlockState blockState = primer.getBlockState(localX, y, localZ);
        IBlockState blockStateAbove = y < 255 ? primer.getBlockState(localX, y + 1, localZ) : AIR;
        if (!canReplaceModernBlock(blockState, blockStateAbove, biome)) {
            return false;
        }

        if (y > 0 && blockState == biome.topBlock
                && canReplaceModernBlock(primer.getBlockState(localX, y - 1, localZ), AIR, biome)) {
            primer.setBlockState(localX, y - 1, localZ, biome.topBlock);
        }

        if (y < 255 && blockStateAbove == SAND) {
            primer.setBlockState(localX, y + 1, localZ, SANDSTONE);
        }
        else if (y < 255 && blockStateAbove == RED_SAND) {
            primer.setBlockState(localX, y + 1, localZ, RED_SANDSTONE);
        }
        else if (y < 255 && replaceFloatingGravel && blockStateAbove == GRAVEL) {
            primer.setBlockState(localX, y + 1, localZ, ANDESITE);
        }

        primer.setBlockState(localX, y, localZ, replacement);
        return true;
    }

    private boolean canReplaceModernBlock(IBlockState blockState, IBlockState blockStateAbove, Biome biome) {
        Block block = blockState.getBlock();
        if (block == Blocks.STONE || blockState.getMaterial() == Material.ROCK) {
            return block != Blocks.BEDROCK;
        }
        if (block == Blocks.LEAVES || block == Blocks.LEAVES2 || block == Blocks.LOG || block == Blocks.LOG2) {
            return false;
        }
        if (blockStateAbove.getBlock() == Blocks.LOG || blockStateAbove.getBlock() == Blocks.LOG2) {
            return false;
        }
        if (block == Blocks.BEDROCK) {
            return false;
        }
        if (block == biome.topBlock.getBlock() || block == biome.fillerBlock.getBlock()) {
            return true;
        }
        if (block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.HARDENED_CLAY
                || block == Blocks.STAINED_HARDENED_CLAY || block == Blocks.SANDSTONE
                || block == Blocks.RED_SANDSTONE || block == Blocks.MYCELIUM || block == Blocks.SNOW_LAYER) {
            return true;
        }

        return (block == Blocks.SAND || block == Blocks.GRAVEL) && blockStateAbove.getMaterial() != Material.WATER;
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
