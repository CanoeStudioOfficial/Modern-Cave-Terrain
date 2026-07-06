package com.lonelyxiya.minecraft.moderncaveterrain.api;

/**
 * Chunk-level decorator attached to a registered pseudo-3D underground biome.
 *
 * <p>Decorators run after Modern Cave Terrain carves a chunk, while the chunk is still a {@code ChunkPrimer}.
 * Scan the chunk, sample the underground biome for candidate positions, then place blocks only where the sample id
 * matches the attached definition.</p>
 */
public interface ModernCaveTerrainUndergroundBiomeDecorator {
    /**
     * Decorates one chunk for the given registered underground biome.
     *
     * <pre>{@code
     * .decorator((context, definition) -> {
     *     for (int localX = 0; localX < 16; localX++) {
     *         for (int localZ = 0; localZ < 16; localZ++) {
     *             for (int y = definition.getMinY(); y <= definition.getMaxY(); y++) {
     *                 ModernCaveTerrainUndergroundBiomeSample sample =
     *                     context.sampleUndergroundBiomeLocal(localX, y, localZ);
     *                 if (definition.getId().equals(sample.getBiomeId())) {
     *                     // Place this biome's decoration here.
     *                 }
     *             }
     *         }
     *     }
     * })
     * }</pre>
     */
    void decorate(ModernCaveTerrainCaveDecorationContext context,
                  ModernCaveTerrainUndergroundBiomeDefinition definition);
}
