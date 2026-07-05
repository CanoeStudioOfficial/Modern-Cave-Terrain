package com.lonelyxiya.minecraft.moderncaveterrain.api;

import com.lonelyxiya.minecraft.moderncaveterrain.config.ModernCaveTerrainSettings;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;

/**
 * Immutable pseudo-3D underground biome sample.
 *
 * <p>The six climate fields mirror the high-version multi-noise biome source dimensions in a Java 8 and
 * 1.12.2-friendly form: temperature, humidity, continentalness, erosion, depth and weirdness.</p>
 */
public final class ModernCaveTerrainUndergroundBiomeSample {
    public static final ResourceLocation NONE = new ResourceLocation(ModernCaveTerrainSettings.MOD_ID, "none");
    public static final ResourceLocation NORMAL_CAVES = new ResourceLocation(ModernCaveTerrainSettings.MOD_ID, "normal_caves");
    public static final ResourceLocation GENERIC_3D_CAVES = new ResourceLocation(ModernCaveTerrainSettings.MOD_ID, "generic_3d_caves");
    public static final ResourceLocation UNDERWATER_CAVES = new ResourceLocation(ModernCaveTerrainSettings.MOD_ID, "underwater_caves");
    public static final ResourceLocation LAVA_CAVES = new ResourceLocation(ModernCaveTerrainSettings.MOD_ID, "lava_caves");

    private final int blockX;
    private final int blockY;
    private final int blockZ;
    private final ModernCaveTerrainCaveBiomeType caveBiomeType;
    private final ResourceLocation biomeId;
    private final double temperature;
    private final double humidity;
    private final double continentalness;
    private final double erosion;
    private final double depth;
    private final double weirdness;
    private final double distance;
    private final double secondDistance;
    private final double edgeFactor;
    private final Biome surfaceBiome;
    private final IBlockState fluidState;

    public ModernCaveTerrainUndergroundBiomeSample(int blockX, int blockY, int blockZ,
                                                   ModernCaveTerrainCaveBiomeType caveBiomeType,
                                                   ResourceLocation biomeId,
                                                   double temperature, double humidity,
                                                   double continentalness, double erosion,
                                                   double depth, double weirdness,
                                                   double distance, double secondDistance,
                                                   double edgeFactor, Biome surfaceBiome,
                                                   IBlockState fluidState) {
        if (caveBiomeType == null) {
            throw new IllegalArgumentException("Underground cave biome type cannot be null");
        }
        if (biomeId == null) {
            throw new IllegalArgumentException("Underground cave biome id cannot be null");
        }

        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.caveBiomeType = caveBiomeType;
        this.biomeId = biomeId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.continentalness = continentalness;
        this.erosion = erosion;
        this.depth = depth;
        this.weirdness = weirdness;
        this.distance = distance;
        this.secondDistance = secondDistance;
        this.edgeFactor = clamp(edgeFactor, 0.0D, 1.0D);
        this.surfaceBiome = surfaceBiome;
        this.fluidState = fluidState;
    }

    public int getBlockX() {
        return blockX;
    }

    public int getBlockY() {
        return blockY;
    }

    public int getBlockZ() {
        return blockZ;
    }

    public ModernCaveTerrainCaveBiomeType getCaveBiomeType() {
        return caveBiomeType;
    }

    public ResourceLocation getBiomeId() {
        return biomeId;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getContinentalness() {
        return continentalness;
    }

    public double getErosion() {
        return erosion;
    }

    public double getDepth() {
        return depth;
    }

    public double getWeirdness() {
        return weirdness;
    }

    /**
     * Gets the nearest underground-biome parameter distance. Lower means a stronger match.
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Gets the second-nearest underground-biome parameter distance.
     */
    public double getSecondDistance() {
        return secondDistance;
    }

    /**
     * Gets a 0..1 confidence-like factor. Values near 0 are close to a biome edge or fallback boundary.
     */
    public double getEdgeFactor() {
        return edgeFactor;
    }

    public Biome getSurfaceBiome() {
        return surfaceBiome;
    }

    public IBlockState getFluidState() {
        return fluidState;
    }

    public ModernCaveTerrainUndergroundBiomeSample withBiome(ModernCaveTerrainCaveBiomeType caveBiomeType) {
        return withBiome(caveBiomeType, getDefaultBiomeId(caveBiomeType));
    }

    public ModernCaveTerrainUndergroundBiomeSample withBiome(ModernCaveTerrainCaveBiomeType caveBiomeType,
                                                             ResourceLocation biomeId) {
        return new ModernCaveTerrainUndergroundBiomeSample(blockX, blockY, blockZ, caveBiomeType, biomeId,
                temperature, humidity, continentalness, erosion, depth, weirdness, distance, secondDistance,
                edgeFactor, surfaceBiome, fluidState);
    }

    public static ResourceLocation getDefaultBiomeId(ModernCaveTerrainCaveBiomeType caveBiomeType) {
        if (caveBiomeType == null) {
            return NONE;
        }

        switch (caveBiomeType) {
            case NORMAL:
                return NORMAL_CAVES;
            case GENERIC_3D:
                return GENERIC_3D_CAVES;
            case UNDERWATER:
                return UNDERWATER_CAVES;
            case LAVA:
                return LAVA_CAVES;
            case NONE:
            default:
                return NONE;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
