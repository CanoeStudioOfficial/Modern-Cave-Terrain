package com.lonelyxiya.minecraft.moderncaveterrain.api;

import net.minecraft.world.World;

/**
 * Extension point for replacing or refining Modern Cave Terrain's pseudo-3D underground biome choice.
 */
public interface ModernCaveTerrainUndergroundBiomeResolver {
    /**
     * Resolves an underground biome sample.
     *
     * <p>Return {@code null} to keep the incoming sample unchanged. Return the incoming sample, a
     * {@link ModernCaveTerrainUndergroundBiomeSample#withBiome(ModernCaveTerrainCaveBiomeType)} copy, or a newly
     * constructed sample to override it.</p>
     *
     * @param world world being sampled
     * @param sample default or previously resolved sample
     * @return replacement sample, or {@code null} to leave the sample unchanged
     */
    ModernCaveTerrainUndergroundBiomeSample resolve(World world, ModernCaveTerrainUndergroundBiomeSample sample);
}
