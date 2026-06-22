package com.yungnickyoung.minecraft.bettercaves.world.carver.modern;

import com.yungnickyoung.minecraft.bettercaves.api.ModernCaveCarvingType;
import com.yungnickyoung.minecraft.bettercaves.config.util.ConfigHolder;
import com.yungnickyoung.minecraft.bettercaves.noise.OpenSimplex2S;
import net.minecraft.init.Blocks;

class SpaghettiCaveCarver extends AbstractModernNoiseCaveCarver {
    static final int COLUMN_MASK = 1 << 1;

    private final float spaghettiThickness;
    private final float spaghettiRoughnessFrequency;
    private final OpenSimplex2S spaghettiRoughnessNoise;
    private final ProgrammableNoiseSampler spaghettiASampler;
    private final ProgrammableNoiseSampler spaghettiBSampler;

    SpaghettiCaveCarver(long seed, ConfigHolder config) {
        super(seed, COLUMN_MASK, ModernCaveCarvingType.SPAGHETTI, Blocks.QUARTZ_BLOCK.getDefaultState(),
            config.enableSpaghettiCaves.get(), config.spaghettiCaveSpawnChance.get() / 100f, 911, .006);
        this.spaghettiThickness = config.spaghettiCaveThickness.get();
        this.spaghettiRoughnessFrequency = config.spaghettiCaveRoughnessFrequency.get();
        this.spaghettiRoughnessNoise = new OpenSimplex2S(seed + 914);
        this.spaghettiASampler = createNoiseSampler(seed, 951, config.spaghettiCaveNoiseFrequency.get(), 0f,
            config.spaghettiCaveVerticalStretch.get(), config.spaghettiCavePerturbAmp.get(), config.spaghettiCavePerturbFrequency.get());
        this.spaghettiBSampler = createNoiseSampler(seed, 952, config.spaghettiCaveNoiseFrequency.get(), 0f,
            config.spaghettiCaveVerticalStretch.get(), config.spaghettiCavePerturbAmp.get(), config.spaghettiCavePerturbFrequency.get());
    }

    @Override
    public boolean shouldCarve(int x, int y, int z, float fade) {
        double roughness = normalize(noise3(spaghettiRoughnessNoise, x, y, z, spaghettiRoughnessFrequency));
        double thickness = spaghettiThickness * (.75 + roughness * .65) * fade;
        return spaghettiASampler.carveBand(x, y, z, thickness) &&
            spaghettiBSampler.carveBand(x + 103, y, z - 79, thickness);
    }
}
