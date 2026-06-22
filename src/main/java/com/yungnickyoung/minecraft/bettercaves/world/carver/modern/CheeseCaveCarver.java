package com.yungnickyoung.minecraft.bettercaves.world.carver.modern;

import com.yungnickyoung.minecraft.bettercaves.api.ModernCaveCarvingType;
import com.yungnickyoung.minecraft.bettercaves.config.util.ConfigHolder;
import net.minecraft.init.Blocks;

class CheeseCaveCarver extends AbstractModernNoiseCaveCarver {
    static final int COLUMN_MASK = 1;

    private final float cheeseCaveSize;
    private final float cheeseNoiseThreshold;
    private final float cheeseDetailWeight;
    private final ProgrammableNoiseSampler cheeseSampler;
    private final ProgrammableNoiseSampler cheeseDetailSampler;

    CheeseCaveCarver(long seed, ConfigHolder config) {
        super(seed, COLUMN_MASK, ModernCaveCarvingType.CHEESE, Blocks.LAPIS_BLOCK.getDefaultState(),
            config.enableCheeseCaves.get(), config.cheeseCaveSpawnChance.get() / 100f, 901, .0035);
        this.cheeseCaveSize = clamp01(config.cheeseCaveSize.get());
        this.cheeseNoiseThreshold = config.cheeseCaveNoiseThreshold.get();
        this.cheeseDetailWeight = clamp(config.cheeseCaveDetailWeight.get(), 0f, 1f);
        this.cheeseSampler = createNoiseSampler(seed, 941, config.cheeseCaveNoiseFrequency.get(), cheeseNoiseThreshold,
            config.cheeseCaveVerticalStretch.get(), config.cheeseCavePerturbAmp.get(), config.cheeseCavePerturbFrequency.get());
        this.cheeseDetailSampler = createNoiseSampler(seed, 942, config.cheeseCaveDetailFrequency.get(), 0f,
            config.cheeseCaveVerticalStretch.get() * 1.5f, config.cheeseCavePerturbAmp.get() * .3f,
            config.cheeseCavePerturbFrequency.get() * 1.4f);
    }

    @Override
    public boolean shouldCarve(int x, int y, int z, float fade) {
        double shape = cheeseSampler.sample(x, y, z) + cheeseDetailSampler.sample(x, y, z) * cheeseDetailWeight;
        double threshold = cheeseNoiseThreshold + (cheeseCaveSize - .5) * .75 - (1.0 - fade) * .35;

        return shape <= threshold;
    }
}
