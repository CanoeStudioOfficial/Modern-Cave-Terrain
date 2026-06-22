package com.yungnickyoung.minecraft.bettercaves.world.carver.modern;

import com.yungnickyoung.minecraft.bettercaves.BetterCaves;
import com.yungnickyoung.minecraft.bettercaves.config.util.ConfigHolder;
import com.yungnickyoung.minecraft.bettercaves.noise.FastNoise;
import com.yungnickyoung.minecraft.bettercaves.util.BetterCavesUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class AquiferSampler {
    private static final IBlockState AIR = Blocks.AIR.getDefaultState();

    private final boolean enabled;
    private final int aquiferBottom;
    private final int aquiferTop;
    private final int liquidAltitude;
    private final int waterLevelMin;
    private final int waterLevelMax;
    private final float waterChance;
    private final IBlockState lavaBlock;
    private final IBlockState waterBlock;
    private final FastNoise waterLevelNoise;
    private final FastNoise waterRegionNoise;
    private final FastNoise barrierNoise;

    public AquiferSampler(World world, ConfigHolder config) {
        this.enabled = config.enableAquifers.get();
        this.aquiferBottom = Math.min(config.aquiferBottom.get(), config.aquiferTop.get());
        this.aquiferTop = Math.max(config.aquiferBottom.get(), config.aquiferTop.get());
        this.liquidAltitude = config.liquidAltitude.get();
        this.waterLevelMin = Math.min(config.aquiferWaterLevelMin.get(), config.aquiferWaterLevelMax.get());
        this.waterLevelMax = Math.max(config.aquiferWaterLevelMin.get(), config.aquiferWaterLevelMax.get());
        this.waterChance = clamp01(config.aquiferWaterChance.get() / 100f);
        this.lavaBlock = getBlockFromString(world, config.lavaBlock.get(), Blocks.LAVA.getDefaultState(), "lava");
        this.waterBlock = getBlockFromString(world, config.waterBlock.get(), Blocks.WATER.getDefaultState(), "water");

        this.waterLevelNoise = new FastNoise();
        this.waterLevelNoise.SetSeed((int)world.getSeed() + 881);
        this.waterLevelNoise.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
        this.waterLevelNoise.SetFractalOctaves(2);
        this.waterLevelNoise.SetFrequency(.018f);

        this.waterRegionNoise = new FastNoise();
        this.waterRegionNoise.SetSeed((int)world.getSeed() + 882);
        this.waterRegionNoise.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
        this.waterRegionNoise.SetFractalOctaves(2);
        this.waterRegionNoise.SetFrequency(.006f);

        this.barrierNoise = new FastNoise();
        this.barrierNoise.SetSeed((int)world.getSeed() + 883);
        this.barrierNoise.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
        this.barrierNoise.SetFractalOctaves(2);
        this.barrierNoise.SetFrequency(.055f);
    }

    public Sample sample(int x, int y, int z, int surfaceAltitude, IBlockState fallbackLiquidBlock) {
        if (y <= liquidAltitude) {
            if (fallbackLiquidBlock == null) {
                return Sample.blocked();
            }
            return Sample.carveAs(fallbackLiquidBlock);
        }

        if (!enabled || y < aquiferBottom || y > aquiferTop || waterChance <= 0) {
            return Sample.air();
        }

        if (y <= liquidAltitude + 6) {
            return Sample.air();
        }

        if (!isWaterRegion(x, z)) {
            return Sample.air();
        }

        int waterLevel = getWaterLevel(x, z, surfaceAltitude);
        if (y > waterLevel) {
            return Sample.air();
        }

        if (shouldPreserveBarrier(x, y, z, surfaceAltitude, waterLevel)) {
            return Sample.blocked();
        }

        return Sample.carveAs(waterBlock);
    }

    private boolean isWaterRegion(int x, int z) {
        return normalize(waterRegionNoise.GetNoise(x, z)) <= waterChance;
    }

    private int getWaterLevel(int x, int z, int surfaceAltitude) {
        int levelRange = Math.max(0, waterLevelMax - waterLevelMin);
        int level = waterLevelMin + Math.round(normalize(waterLevelNoise.GetNoise(x, z)) * levelRange);
        int ceiling = surfaceAltitude > aquiferBottom ? Math.min(aquiferTop, surfaceAltitude - 4) : aquiferTop;
        if (ceiling < aquiferBottom) {
            ceiling = aquiferBottom;
        }
        return clamp(level, aquiferBottom, ceiling);
    }

    private boolean shouldPreserveBarrier(int x, int y, int z, int surfaceAltitude, int waterLevel) {
        float barrier = barrierNoise.GetNoise(x, y, z);
        if (Math.abs(y - waterLevel) <= 1 && barrier > .45f) {
            return true;
        }

        boolean nearbyLevelShift =
            Math.abs(waterLevel - getWaterLevel(x + 16, z, surfaceAltitude)) > 10 ||
            Math.abs(waterLevel - getWaterLevel(x - 16, z, surfaceAltitude)) > 10 ||
            Math.abs(waterLevel - getWaterLevel(x, z + 16, surfaceAltitude)) > 10 ||
            Math.abs(waterLevel - getWaterLevel(x, z - 16, surfaceAltitude)) > 10;

        return nearbyLevelShift && Math.abs(y - waterLevel) <= 6 && barrier > .25f;
    }

    private IBlockState getBlockFromString(World world, String blockName, IBlockState fallback, String role) {
        IBlockState blockState = fallback;
        try {
            Block block = Block.getBlockFromName(blockName);
            if (block != null) {
                blockState = block.getDefaultState();
            }
            else {
                BetterCaves.LOGGER.warn("Unable to use block '" + blockName + "' as aquifer " + role + ": null block returned.");
            }
        }
        catch (Exception e) {
            BetterCaves.LOGGER.warn("Unable to use block '" + blockName + "' as aquifer " + role + ": " + e);
        }

        BetterCaves.LOGGER.info("Using block '" + blockState.getBlock().getRegistryName() + "' as aquifer " + role +
            " in cave generation for dimension " +
            BetterCavesUtils.dimensionAsString(world.provider.getDimension(), world.provider.getDimensionType().toString()) + " ...");
        return blockState;
    }

    private static float normalize(float value) {
        return clamp01((value + 1f) * .5f);
    }

    private static float clamp01(float value) {
        if (value < 0f) {
            return 0f;
        }
        if (value > 1f) {
            return 1f;
        }
        return value;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static class Sample {
        private static final Sample AIR_SAMPLE = new Sample(AIR, false);
        private static final Sample BLOCKED_SAMPLE = new Sample(AIR, true);

        private final IBlockState blockState;
        private final boolean blocked;

        private Sample(IBlockState blockState, boolean blocked) {
            this.blockState = blockState;
            this.blocked = blocked;
        }

        public static Sample air() {
            return AIR_SAMPLE;
        }

        public static Sample blocked() {
            return BLOCKED_SAMPLE;
        }

        public static Sample carveAs(IBlockState blockState) {
            if (blockState == AIR) {
                return AIR_SAMPLE;
            }
            return new Sample(blockState, false);
        }

        public IBlockState getBlockState() {
            return blockState;
        }

        public boolean isBlocked() {
            return blocked;
        }
    }
}
