package com.lonelyxiya.minecraft.moderncaveterrain.api;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lightweight registry for pseudo-3D underground biomes.
 */
public final class ModernCaveTerrainUndergroundBiomeRegistry {
    private static final int FALLBACK_SCAN_RADIUS = 32;
    private static final int FALLBACK_SCAN_STEP = 8;
    private static final List<ModernCaveTerrainUndergroundBiomeDefinition> DEFINITIONS =
            new CopyOnWriteArrayList<>();

    private ModernCaveTerrainUndergroundBiomeRegistry() {}

    public static void register(ModernCaveTerrainUndergroundBiomeDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Underground biome definition cannot be null");
        }

        unregister(definition.getId());
        DEFINITIONS.add(definition);
    }

    public static boolean unregister(ResourceLocation id) {
        if (id == null) {
            return false;
        }

        boolean removed = false;
        for (ModernCaveTerrainUndergroundBiomeDefinition definition : DEFINITIONS) {
            if (id.equals(definition.getId())) {
                removed |= DEFINITIONS.remove(definition);
            }
        }
        return removed;
    }

    public static ModernCaveTerrainUndergroundBiomeDefinition get(ResourceLocation id) {
        if (id == null) {
            return null;
        }

        for (ModernCaveTerrainUndergroundBiomeDefinition definition : DEFINITIONS) {
            if (id.equals(definition.getId())) {
                return definition;
            }
        }
        return null;
    }

    public static List<ModernCaveTerrainUndergroundBiomeDefinition> getDefinitions() {
        return Collections.unmodifiableList(new ArrayList<>(DEFINITIONS));
    }

    static ModernCaveTerrainUndergroundBiomeSample resolve(World world,
                                                           ModernCaveTerrainUndergroundBiomeSample sample) {
        ModernCaveTerrainUndergroundBiomeDefinition best = null;
        double bestWeight = 0.0D;

        for (ModernCaveTerrainUndergroundBiomeDefinition definition : DEFINITIONS) {
            double weight = getMatchWeight(world, sample, definition);
            if (weight <= 0.0D) {
                continue;
            }
            if (best == null
                    || definition.getPriority() > best.getPriority()
                    || definition.getPriority() == best.getPriority() && weight > bestWeight) {
                best = definition;
                bestWeight = weight;
            }
        }

        return best == null ? sample : sample.withBiome(best.getCaveBiomeType(), best.getId());
    }

    private static double getMatchWeight(World world, ModernCaveTerrainUndergroundBiomeSample sample,
                                         ModernCaveTerrainUndergroundBiomeDefinition definition) {
        if (sample.getBlockY() < definition.getMinY() || sample.getBlockY() > definition.getMaxY()) {
            return 0.0D;
        }

        double biomeWeight = getSurfaceBiomeWeight(world, sample, definition);
        if (biomeWeight <= 0.0D) {
            return 0.0D;
        }

        double selectorWeight = 1.0D;
        ModernCaveTerrainUndergroundBiomeSelector selector = definition.getSelector();
        if (selector != null) {
            selectorWeight = selector.getWeight(world, sample);
        }

        return Math.max(0.0D, biomeWeight * selectorWeight);
    }

    private static double getSurfaceBiomeWeight(World world, ModernCaveTerrainUndergroundBiomeSample sample,
                                                ModernCaveTerrainUndergroundBiomeDefinition definition) {
        ResourceLocation surfaceBiomeId = getBiomeId(sample.getSurfaceBiome());
        if (!definition.hasSurfaceBiomeRules() && !definition.hasFallbackNeighborRules()) {
            return 1.0D;
        }
        if (surfaceBiomeId != null && definition.getSurfaceBiomes().contains(surfaceBiomeId)) {
            return 1.0D;
        }
        if (definition.hasFallbackNeighborRules()
                && hasFallbackNeighborBiome(world, sample, definition)) {
            return 0.72D;
        }

        return 0.0D;
    }

    private static boolean hasFallbackNeighborBiome(World world, ModernCaveTerrainUndergroundBiomeSample sample,
                                                   ModernCaveTerrainUndergroundBiomeDefinition definition) {
        BlockPos center = new BlockPos(sample.getBlockX(), 64, sample.getBlockZ());
        for (int dx = -FALLBACK_SCAN_RADIUS; dx <= FALLBACK_SCAN_RADIUS; dx += FALLBACK_SCAN_STEP) {
            for (int dz = -FALLBACK_SCAN_RADIUS; dz <= FALLBACK_SCAN_RADIUS; dz += FALLBACK_SCAN_STEP) {
                ResourceLocation neighborId = getBiomeId(world.getBiome(center.add(dx, 0, dz)));
                if (neighborId != null && definition.getFallbackNeighborBiomes().contains(neighborId)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static ResourceLocation getBiomeId(Biome biome) {
        return biome == null ? null : biome.getRegistryName();
    }
}
