package com.lonelyxiya.minecraft.moderncaveterrain.api;

import net.minecraft.world.World;

/**
 * Optional custom selector for registered pseudo-3D underground biomes.
 *
 * <p>Use this to refine broad surface-biome and y-range rules with 3D climate values such as humidity,
 * temperature, depth, weirdness or fluid state.</p>
 */
public interface ModernCaveTerrainUndergroundBiomeSelector {
    /**
     * Returns a match weight for the registered underground biome.
     *
     * <p>Values less than or equal to zero mean no match. Larger values make the biome more likely to win against
     * other matching registered biomes with the same priority.</p>
     *
     * <pre>{@code
     * .selector((world, sample) ->
     *     sample.getHumidity() > 0.35D && sample.getTemperature() > -0.15D ? 1.0D : 0.0D)
     * }</pre>
     */
    double getWeight(World world, ModernCaveTerrainUndergroundBiomeSample sample);
}
