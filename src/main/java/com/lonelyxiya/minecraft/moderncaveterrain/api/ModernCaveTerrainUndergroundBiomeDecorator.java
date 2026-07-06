package com.lonelyxiya.minecraft.moderncaveterrain.api;

/**
 * Chunk-level decorator attached to a registered pseudo-3D underground biome.
 */
public interface ModernCaveTerrainUndergroundBiomeDecorator {
    /**
     * Decorates one chunk for the given registered underground biome.
     */
    void decorate(ModernCaveTerrainCaveDecorationContext context,
                  ModernCaveTerrainUndergroundBiomeDefinition definition);
}
