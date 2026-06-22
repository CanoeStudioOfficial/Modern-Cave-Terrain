package com.yungnickyoung.minecraft.bettercaves.world.carver.modern;

import com.yungnickyoung.minecraft.bettercaves.api.ModernCaveCarvingType;
import com.yungnickyoung.minecraft.bettercaves.config.util.ConfigHolder;
import net.minecraft.init.Blocks;

class NoodleCaveCarver extends AbstractModernNoiseCaveCarver {
    static final int COLUMN_MASK = 1 << 2;

    private final float noodleThickness;
    private final float noodleToggleThreshold;
    private final ProgrammableNoiseSampler noodleASampler;
    private final ProgrammableNoiseSampler noodleBSampler;
    private final ProgrammableNoiseSampler noodleToggleSampler;

    NoodleCaveCarver(long seed, ConfigHolder config) {
        super(seed, COLUMN_MASK, ModernCaveCarvingType.NOODLE, Blocks.IRON_BLOCK.getDefaultState(),
            config.enableNoodleCaves.get(), config.noodleCaveSpawnChance.get() / 100f, 921, .009);
        this.noodleThickness = config.noodleCaveThickness.get();
        this.noodleToggleThreshold = config.noodleCaveToggleThreshold.get();
        this.noodleASampler = createNoiseSampler(seed, 961, config.noodleCaveNoiseFrequency.get(), 0f,
            config.noodleCaveVerticalStretch.get(), config.noodleCavePerturbAmp.get(), config.noodleCavePerturbFrequency.get());
        this.noodleBSampler = createNoiseSampler(seed, 962, config.noodleCaveNoiseFrequency.get(), 0f,
            config.noodleCaveVerticalStretch.get(), config.noodleCavePerturbAmp.get(), config.noodleCavePerturbFrequency.get());
        this.noodleToggleSampler = createNoiseSampler(seed, 963, config.noodleCaveToggleFrequency.get(), noodleToggleThreshold,
            1f, 0f, 0f);
    }

    @Override
    public boolean shouldCarve(int x, int y, int z, float fade) {
        double toggle = noodleToggleSampler.sample(x, y, z);
        if (toggle < noodleToggleThreshold) {
            return false;
        }

        double thickness = noodleThickness * (.75 + normalize(toggle) * .5) * fade;
        return noodleASampler.carveBand(x, y, z, thickness) &&
            noodleBSampler.carveBand(x - 47, y + 23, z + 61, thickness);
    }
}
