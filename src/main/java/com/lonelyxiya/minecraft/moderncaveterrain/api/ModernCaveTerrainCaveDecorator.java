package com.lonelyxiya.minecraft.moderncaveterrain.api;

/**
 * Chunk-level decoration hook invoked after Modern Cave Terrain finishes carving.
 */
public interface ModernCaveTerrainCaveDecorator {
    void decorate(ModernCaveTerrainCaveDecorationContext context);
}
