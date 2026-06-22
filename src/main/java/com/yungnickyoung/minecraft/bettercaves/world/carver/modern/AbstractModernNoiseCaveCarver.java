package com.yungnickyoung.minecraft.bettercaves.world.carver.modern;

import com.yungnickyoung.minecraft.bettercaves.api.ModernCaveCarvingType;
import com.yungnickyoung.minecraft.bettercaves.noise.OpenSimplex2S;
import net.minecraft.block.state.IBlockState;

abstract class AbstractModernNoiseCaveCarver implements ModernNoiseCaveCarver {
    private final int columnMask;
    private final ModernCaveCarvingType type;
    private final IBlockState debugBlock;
    private final boolean enabled;
    private final float spawnChance;
    private final double regionFrequency;
    private final OpenSimplex2S regionNoise;

    protected AbstractModernNoiseCaveCarver(long seed, int columnMask, ModernCaveCarvingType type,
                                            IBlockState debugBlock, boolean enabled, float spawnChance,
                                            long regionSeedOffset, double regionFrequency) {
        this.columnMask = columnMask;
        this.type = type;
        this.debugBlock = debugBlock;
        this.enabled = enabled;
        this.spawnChance = clamp01(spawnChance);
        this.regionFrequency = regionFrequency;
        this.regionNoise = new OpenSimplex2S(seed + regionSeedOffset);
    }

    @Override
    public int getColumnMask() {
        return columnMask;
    }

    @Override
    public ModernCaveCarvingType getType() {
        return type;
    }

    @Override
    public IBlockState getDebugBlock() {
        return debugBlock;
    }

    @Override
    public boolean isColumnActive(int x, int z) {
        if (!enabled || spawnChance <= 0f) {
            return false;
        }
        if (spawnChance >= 1f) {
            return true;
        }
        return normalize(regionNoise.noise2(x * regionFrequency, z * regionFrequency)) <= spawnChance;
    }

    protected static ProgrammableNoiseSampler createNoiseSampler(long seed, int seedOffset, float frequency,
                                                                 float threshold, float stretch,
                                                                 float perturbAmp, float perturbFrequency) {
        return new ProgrammableNoiseSampler(seed + seedOffset, frequency, threshold, stretch,
            1, .5, 2.0, perturbAmp, perturbFrequency, false);
    }

    protected static double noise3(OpenSimplex2S noise, double x, double y, double z, double frequency) {
        return noise.noise3_XZBeforeY(x * frequency, y * frequency, z * frequency);
    }

    protected static double normalize(double value) {
        return clamp01((float)((value + 1.0) * .5));
    }

    protected static float clamp01(float value) {
        if (value < 0f) {
            return 0f;
        }
        if (value > 1f) {
            return 1f;
        }
        return value;
    }

    protected static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
