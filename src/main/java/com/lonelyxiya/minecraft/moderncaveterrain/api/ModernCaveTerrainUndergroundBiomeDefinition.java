package com.lonelyxiya.minecraft.moderncaveterrain.api;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Registered pseudo-3D underground biome definition.
 *
 * <p>A definition is the new registration unit for underground cave biomes. It replaces the old split between
 * biome resolvers and global cave decorators: one definition describes where the underground biome may appear,
 * how strongly it matches a sample, and optionally how it decorates matching cave space.</p>
 */
public final class ModernCaveTerrainUndergroundBiomeDefinition {
    private final ResourceLocation id;
    private final ModernCaveTerrainCaveBiomeType caveBiomeType;
    private final int minY;
    private final int maxY;
    private final int priority;
    private final Set<ResourceLocation> surfaceBiomes;
    private final Set<ResourceLocation> fallbackNeighborBiomes;
    private final ModernCaveTerrainUndergroundBiomeSelector selector;
    private final ModernCaveTerrainUndergroundBiomeDecorator decorator;

    private ModernCaveTerrainUndergroundBiomeDefinition(Builder builder) {
        this.id = builder.id;
        this.caveBiomeType = builder.caveBiomeType;
        this.minY = builder.minY;
        this.maxY = builder.maxY;
        this.priority = builder.priority;
        this.surfaceBiomes = Collections.unmodifiableSet(new HashSet<>(builder.surfaceBiomes));
        this.fallbackNeighborBiomes = Collections.unmodifiableSet(new HashSet<>(builder.fallbackNeighborBiomes));
        this.selector = builder.selector;
        this.decorator = builder.decorator;
    }

    public ResourceLocation getId() {
        return id;
    }

    public ModernCaveTerrainCaveBiomeType getCaveBiomeType() {
        return caveBiomeType;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getPriority() {
        return priority;
    }

    public Set<ResourceLocation> getSurfaceBiomes() {
        return surfaceBiomes;
    }

    public Set<ResourceLocation> getFallbackNeighborBiomes() {
        return fallbackNeighborBiomes;
    }

    public ModernCaveTerrainUndergroundBiomeSelector getSelector() {
        return selector;
    }

    public ModernCaveTerrainUndergroundBiomeDecorator getDecorator() {
        return decorator;
    }

    public boolean hasSurfaceBiomeRules() {
        return !surfaceBiomes.isEmpty();
    }

    public boolean hasFallbackNeighborRules() {
        return !fallbackNeighborBiomes.isEmpty();
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private ModernCaveTerrainCaveBiomeType caveBiomeType = ModernCaveTerrainCaveBiomeType.GENERIC_3D;
        private int minY = 0;
        private int maxY = 255;
        private int priority = 0;
        private final Set<ResourceLocation> surfaceBiomes = new HashSet<>();
        private final Set<ResourceLocation> fallbackNeighborBiomes = new HashSet<>();
        private ModernCaveTerrainUndergroundBiomeSelector selector;
        private ModernCaveTerrainUndergroundBiomeDecorator decorator;

        private Builder(ResourceLocation id) {
            if (id == null) {
                throw new IllegalArgumentException("Underground biome id cannot be null");
            }

            this.id = id;
        }

        /**
         * Sets the broad cave-biome type returned by samples that match this definition.
         */
        public Builder caveBiomeType(ModernCaveTerrainCaveBiomeType caveBiomeType) {
            if (caveBiomeType == null) {
                throw new IllegalArgumentException("Underground cave biome type cannot be null");
            }

            this.caveBiomeType = caveBiomeType;
            return this;
        }

        /**
         * Restricts this underground biome to an inclusive 1.12.2 block-y range.
         */
        public Builder yRange(int minY, int maxY) {
            this.minY = Math.max(0, Math.min(255, minY));
            this.maxY = Math.max(0, Math.min(255, maxY));
            return this;
        }

        /**
         * Sets selection priority. Higher priority beats lower priority; matching weight breaks ties.
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Adds an exact surface biome id that can host this underground biome.
         */
        public Builder surfaceBiome(String id) {
            return surfaceBiome(new ResourceLocation(id));
        }

        public Builder surfaceBiome(ResourceLocation id) {
            if (id == null) {
                throw new IllegalArgumentException("Surface biome id cannot be null");
            }

            surfaceBiomes.add(id);
            return this;
        }

        /**
         * Adds exact surface biome ids that can host this underground biome.
         */
        public Builder surfaceBiomes(String... ids) {
            if (ids != null) {
                for (String id : ids) {
                    surfaceBiome(id);
                }
            }
            return this;
        }

        public Builder surfaceBiomes(ResourceLocation... ids) {
            if (ids != null) {
                for (ResourceLocation id : ids) {
                    surfaceBiome(id);
                }
            }
            return this;
        }

        /**
         * Adds a nearby biome id used as a fallback rule.
         *
         * <p>This mirrors the cavesrevamp-style river/edge fallback: if the current surface biome is not directly
         * listed, nearby matching biomes can still allow the underground biome with a lower weight.</p>
         */
        public Builder fallbackNeighborBiome(String id) {
            return fallbackNeighborBiome(new ResourceLocation(id));
        }

        public Builder fallbackNeighborBiome(ResourceLocation id) {
            if (id == null) {
                throw new IllegalArgumentException("Fallback neighbor biome id cannot be null");
            }

            fallbackNeighborBiomes.add(id);
            return this;
        }

        /**
         * Adds nearby biome ids used as fallback rules.
         */
        public Builder fallbackNeighborBiomes(String... ids) {
            if (ids != null) {
                for (String id : ids) {
                    fallbackNeighborBiome(id);
                }
            }
            return this;
        }

        public Builder fallbackNeighborBiomes(ResourceLocation... ids) {
            if (ids != null) {
                for (ResourceLocation id : ids) {
                    fallbackNeighborBiome(id);
                }
            }
            return this;
        }

        /**
         * Sets an optional pseudo-3D selector.
         *
         * <p>The selector receives the sampled climate/depth/fluid data. Return {@code <= 0} to reject the sample,
         * or a positive weight to allow it. Example: humid, warm cave bands for lush caves.</p>
         */
        public Builder selector(ModernCaveTerrainUndergroundBiomeSelector selector) {
            this.selector = selector;
            return this;
        }

        /**
         * Sets an optional chunk decorator for this underground biome.
         *
         * <p>The decorator should sample positions with
         * {@link ModernCaveTerrainCaveDecorationContext#sampleUndergroundBiomeLocal(int, int, int)} and only place
         * blocks when the returned sample id matches {@link ModernCaveTerrainUndergroundBiomeDefinition#getId()}.</p>
         */
        public Builder decorator(ModernCaveTerrainUndergroundBiomeDecorator decorator) {
            this.decorator = decorator;
            return this;
        }

        public ModernCaveTerrainUndergroundBiomeDefinition build() {
            if (minY > maxY) {
                int swap = minY;
                minY = maxY;
                maxY = swap;
            }

            return new ModernCaveTerrainUndergroundBiomeDefinition(this);
        }
    }
}
