package com.lonelyxiya.minecraft.moderncaveterrain.api;

import net.minecraft.world.World;

/**
 * Optional custom selector for registered pseudo-3D underground biomes.
 */
public interface ModernCaveTerrainUndergroundBiomeSelector {
    /**
     * Returns a match weight for the registered underground biome.
     *
     * <p>Values less than or equal to zero mean no match. Larger values make the biome more likely to win against
     * other matching registered biomes with the same priority.</p>
     */
    double getWeight(World world, ModernCaveTerrainUndergroundBiomeSample sample);
}
