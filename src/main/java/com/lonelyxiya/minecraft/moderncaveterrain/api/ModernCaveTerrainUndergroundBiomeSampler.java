package com.lonelyxiya.minecraft.moderncaveterrain.api;

import com.lonelyxiya.minecraft.moderncaveterrain.noise.MojangNormalNoise;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default pseudo-3D underground biome sampler.
 *
 * <p>This is an equivalence layer for 1.12.2: it keeps the modern multi-noise dimensions and nearest parameter
 * selection, while avoiding high-version biome registries and 3D biome containers.</p>
 */
public final class ModernCaveTerrainUndergroundBiomeSampler {
    private static final double MAX_UNDERGROUND_DISTANCE = 0.0325D;
    private static final ParameterRange FULL_RANGE = new ParameterRange(-1.0D, 1.0D);
    private static final ParameterRange UNDERGROUND_DEPTH = new ParameterRange(0.2D, 0.9D);
    private static final ClimateCandidate[] CANDIDATES = new ClimateCandidate[] {
            new ClimateCandidate(ModernCaveTerrainCaveBiomeType.DRIPSTONE,
                    ModernCaveTerrainUndergroundBiomeSample.DRIPSTONE_CAVES,
                    FULL_RANGE, FULL_RANGE, new ParameterRange(0.8D, 1.0D), FULL_RANGE,
                    UNDERGROUND_DEPTH, FULL_RANGE),
            new ClimateCandidate(ModernCaveTerrainCaveBiomeType.LUSH,
                    ModernCaveTerrainUndergroundBiomeSample.LUSH_CAVES,
                    FULL_RANGE, new ParameterRange(0.7D, 1.0D), FULL_RANGE, FULL_RANGE,
                    UNDERGROUND_DEPTH, FULL_RANGE),
            new ClimateCandidate(ModernCaveTerrainCaveBiomeType.DEEP_DARK,
                    ModernCaveTerrainUndergroundBiomeSample.DEEP_DARK,
                    FULL_RANGE, FULL_RANGE, FULL_RANGE, new ParameterRange(-1.0D, -0.375D),
                    new ParameterRange(0.9D, 1.2D), FULL_RANGE)
    };

    private static final ConcurrentMap<Long, NoiseSet> NOISE_CACHE = new ConcurrentHashMap<>();

    private ModernCaveTerrainUndergroundBiomeSampler() {}

    public static ModernCaveTerrainUndergroundBiomeSample sample(World world, ModernCaveTerrainConfig config,
                                                                 int blockX, int blockY, int blockZ,
                                                                 IBlockState fluidState) {
        Biome surfaceBiome = world.getBiome(new BlockPos(blockX, 0, blockZ));
        ClimatePoint target = sampleClimate(world, config, surfaceBiome, blockX, blockY, blockZ);
        FluidKind fluidKind = getFluidKind(fluidState);
        if (fluidKind == FluidKind.LAVA) {
            return createSample(blockX, blockY, blockZ, ModernCaveTerrainCaveBiomeType.LAVA,
                    ModernCaveTerrainUndergroundBiomeSample.LAVA_CAVES, target, 0.0D, Double.POSITIVE_INFINITY,
                    1.0D, surfaceBiome, fluidState);
        }
        if (fluidKind == FluidKind.WATER) {
            return createSample(blockX, blockY, blockZ, ModernCaveTerrainCaveBiomeType.UNDERWATER,
                    ModernCaveTerrainUndergroundBiomeSample.UNDERWATER_CAVES, target, 0.0D,
                    Double.POSITIVE_INFINITY, 1.0D, surfaceBiome, fluidState);
        }

        CandidateChoice choice = chooseCandidate(target);
        return createSample(blockX, blockY, blockZ, choice.type, choice.biomeId, target, choice.distance,
                choice.secondDistance, choice.edgeFactor, surfaceBiome, fluidState);
    }

    private static ClimatePoint sampleClimate(World world, ModernCaveTerrainConfig config, Biome surfaceBiome,
                                              int blockX, int blockY, int blockZ) {
        NoiseSet noises = getNoiseSet(world.getSeed());
        int surfaceY = Math.min(world.getActualHeight() - 1, world.getHeight(new BlockPos(blockX, 0, blockZ)).getY());
        int topY = surfaceY;
        int bottomY = 0;
        if (config != null && config.hasOption(ModernCaveTerrainConfigOptions.MOJANG_118_CAVE_TOP)) {
            topY = Math.min(topY, Math.min(world.getActualHeight() - 1, config.getMojang118StyleCaveTop()));
            bottomY = Math.max(0, config.getMojang118StyleCaveBottom());
        }

        double temperature = normalizeTemperature(surfaceBiome.getTemperature(new BlockPos(blockX, Math.min(64, surfaceY), blockZ)))
                + noises.temperature.getValue(blockX * 0.006D, blockY * 0.010D, blockZ * 0.006D) * 0.32D;
        double humidity = normalizeHumidity(surfaceBiome.getRainfall(), surfaceBiome)
                + noises.humidity.getValue(blockX * 0.008D, blockY * 0.018D, blockZ * 0.008D) * 0.38D;
        double continentalness = noises.continentalness.getValue(blockX * 0.0035D, 0.0D, blockZ * 0.0035D) * 1.08D
                + surfaceContinentalnessBias(surfaceBiome);
        double erosion = noises.erosion.getValue(blockX * 0.010D, blockY * 0.0025D, blockZ * 0.010D) * 1.05D
                + surfaceErosionBias(surfaceBiome);
        double depth = clampedMap(blockY, Math.max(bottomY + 1, topY), bottomY, 0.0D, 1.1D)
                + noises.depth.getValue(blockX * 0.006D, blockY * 0.012D, blockZ * 0.006D) * 0.055D;
        double weirdness = noises.weirdness.getValue(blockX * 0.014D, blockY * 0.014D, blockZ * 0.014D);

        return new ClimatePoint(clamp(temperature, -1.0D, 1.0D), clamp(humidity, -1.0D, 1.0D),
                clamp(continentalness, -1.0D, 1.0D), clamp(erosion, -1.0D, 1.0D),
                clamp(depth, -0.1D, 1.2D), clamp(weirdness, -1.0D, 1.0D));
    }

    private static CandidateChoice chooseCandidate(ClimatePoint target) {
        ClimateCandidate best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        double secondDistance = Double.POSITIVE_INFINITY;

        for (ClimateCandidate candidate : CANDIDATES) {
            double distance = candidate.fitness(target);
            if (distance < bestDistance) {
                secondDistance = bestDistance;
                bestDistance = distance;
                best = candidate;
            }
            else if (distance < secondDistance) {
                secondDistance = distance;
            }
        }

        if (best == null || bestDistance > MAX_UNDERGROUND_DISTANCE) {
            double edgeFactor = clamp((bestDistance - MAX_UNDERGROUND_DISTANCE) / MAX_UNDERGROUND_DISTANCE, 0.0D, 1.0D);
            return new CandidateChoice(ModernCaveTerrainCaveBiomeType.NORMAL,
                    ModernCaveTerrainUndergroundBiomeSample.NORMAL_CAVES, bestDistance, secondDistance, edgeFactor);
        }

        double thresholdFactor = clamp((MAX_UNDERGROUND_DISTANCE - bestDistance) / MAX_UNDERGROUND_DISTANCE, 0.0D, 1.0D);
        double neighborFactor = secondDistance == Double.POSITIVE_INFINITY
                ? 1.0D
                : clamp((secondDistance - bestDistance) / 0.08D, 0.0D, 1.0D);
        return new CandidateChoice(best.type, best.biomeId, bestDistance, secondDistance,
                Math.min(thresholdFactor, neighborFactor));
    }

    private static ModernCaveTerrainUndergroundBiomeSample createSample(int blockX, int blockY, int blockZ,
                                                                        ModernCaveTerrainCaveBiomeType type,
                                                                        ResourceLocation biomeId,
                                                                        ClimatePoint target, double distance,
                                                                        double secondDistance, double edgeFactor,
                                                                        Biome surfaceBiome,
                                                                        IBlockState fluidState) {
        return new ModernCaveTerrainUndergroundBiomeSample(blockX, blockY, blockZ, type, biomeId,
                target.temperature, target.humidity, target.continentalness, target.erosion, target.depth,
                target.weirdness, distance, secondDistance, edgeFactor, surfaceBiome, fluidState);
    }

    private static NoiseSet getNoiseSet(long seed) {
        NoiseSet existing = NOISE_CACHE.get(seed);
        if (existing != null) {
            return existing;
        }

        NoiseSet created = new NoiseSet(seed);
        NoiseSet raced = NOISE_CACHE.putIfAbsent(seed, created);
        return raced == null ? created : raced;
    }

    private static FluidKind getFluidKind(IBlockState state) {
        if (state == null) {
            return FluidKind.NONE;
        }
        if (state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.FLOWING_LAVA
                || state.getMaterial() == Material.LAVA) {
            return FluidKind.LAVA;
        }
        if (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER
                || state.getMaterial() == Material.WATER) {
            return FluidKind.WATER;
        }
        return FluidKind.NONE;
    }

    private static double normalizeTemperature(float temperature) {
        return (temperature - 0.5D) * 1.25D;
    }

    private static double normalizeHumidity(float rainfall, Biome biome) {
        double humidity = clamp(rainfall, 0.0D, 1.0D) * 2.0D - 1.0D;
        if (hasType(biome, BiomeDictionary.Type.WET)
                || hasType(biome, BiomeDictionary.Type.JUNGLE)
                || hasType(biome, BiomeDictionary.Type.SWAMP)) {
            humidity += 0.25D;
        }
        if (hasType(biome, BiomeDictionary.Type.DRY)) {
            humidity -= 0.22D;
        }
        return humidity * 0.88D;
    }

    private static double surfaceContinentalnessBias(Biome biome) {
        double bias = 0.12D;
        if (hasType(biome, BiomeDictionary.Type.OCEAN)) {
            bias -= 0.48D;
        }
        if (hasType(biome, BiomeDictionary.Type.RIVER) || hasType(biome, BiomeDictionary.Type.BEACH)) {
            bias -= 0.28D;
        }
        if (hasType(biome, BiomeDictionary.Type.MOUNTAIN) || hasType(biome, BiomeDictionary.Type.HILLS)) {
            bias += 0.18D;
        }
        if (hasType(biome, BiomeDictionary.Type.MESA) || hasType(biome, BiomeDictionary.Type.DRY)) {
            bias += 0.14D;
        }
        return bias;
    }

    private static double surfaceErosionBias(Biome biome) {
        double bias = 0.0D;
        if (hasType(biome, BiomeDictionary.Type.MOUNTAIN) || hasType(biome, BiomeDictionary.Type.HILLS)) {
            bias -= 0.16D;
        }
        if (hasType(biome, BiomeDictionary.Type.OCEAN) || hasType(biome, BiomeDictionary.Type.RIVER)) {
            bias += 0.10D;
        }
        return bias;
    }

    private static boolean hasType(Biome biome, BiomeDictionary.Type type) {
        return BiomeDictionary.hasType(biome, type);
    }

    private static double clampedMap(double value, double fromMin, double fromMax, double toMin, double toMax) {
        if (fromMin < fromMax) {
            if (value <= fromMin) {
                return toMin;
            }
            if (value >= fromMax) {
                return toMax;
            }
        }
        else {
            if (value >= fromMin) {
                return toMin;
            }
            if (value <= fromMax) {
                return toMax;
            }
        }

        return lerp((value - fromMin) / (fromMax - fromMin), toMin, toMax);
    }

    private static double lerp(double factor, double from, double to) {
        return from + factor * (to - from);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum FluidKind {
        NONE,
        WATER,
        LAVA
    }

    private static final class NoiseSet {
        private final MojangNormalNoise temperature;
        private final MojangNormalNoise humidity;
        private final MojangNormalNoise continentalness;
        private final MojangNormalNoise erosion;
        private final MojangNormalNoise depth;
        private final MojangNormalNoise weirdness;

        private NoiseSet(long seed) {
            this.temperature = MojangNormalNoise.create(seed, "moderncaveterrain:underground_temperature", -7, 1.0D, 1.0D);
            this.humidity = MojangNormalNoise.create(seed, "moderncaveterrain:underground_humidity", -7, 1.0D, 1.0D);
            this.continentalness = MojangNormalNoise.create(seed, "moderncaveterrain:underground_continentalness", -9, 1.0D, 1.0D);
            this.erosion = MojangNormalNoise.create(seed, "moderncaveterrain:underground_erosion", -7, 1.0D, 1.0D);
            this.depth = MojangNormalNoise.create(seed, "moderncaveterrain:underground_depth", -6, 1.0D);
            this.weirdness = MojangNormalNoise.create(seed, "moderncaveterrain:underground_weirdness", -7, 1.0D, 1.0D);
        }
    }

    private static final class ClimatePoint {
        private final double temperature;
        private final double humidity;
        private final double continentalness;
        private final double erosion;
        private final double depth;
        private final double weirdness;

        private ClimatePoint(double temperature, double humidity, double continentalness, double erosion,
                             double depth, double weirdness) {
            this.temperature = temperature;
            this.humidity = humidity;
            this.continentalness = continentalness;
            this.erosion = erosion;
            this.depth = depth;
            this.weirdness = weirdness;
        }
    }

    private static final class ParameterRange {
        private final double min;
        private final double max;

        private ParameterRange(double min, double max) {
            this.min = min;
            this.max = max;
        }

        private double distance(double value) {
            if (value < min) {
                return min - value;
            }
            if (value > max) {
                return value - max;
            }
            return 0.0D;
        }
    }

    private static final class ClimateCandidate {
        private final ModernCaveTerrainCaveBiomeType type;
        private final ResourceLocation biomeId;
        private final ParameterRange temperature;
        private final ParameterRange humidity;
        private final ParameterRange continentalness;
        private final ParameterRange erosion;
        private final ParameterRange depth;
        private final ParameterRange weirdness;

        private ClimateCandidate(ModernCaveTerrainCaveBiomeType type, ResourceLocation biomeId,
                                 ParameterRange temperature, ParameterRange humidity,
                                 ParameterRange continentalness, ParameterRange erosion,
                                 ParameterRange depth, ParameterRange weirdness) {
            this.type = type;
            this.biomeId = biomeId;
            this.temperature = temperature;
            this.humidity = humidity;
            this.continentalness = continentalness;
            this.erosion = erosion;
            this.depth = depth;
            this.weirdness = weirdness;
        }

        private double fitness(ClimatePoint target) {
            return square(temperature.distance(target.temperature)) * 0.75D
                    + square(humidity.distance(target.humidity)) * 1.15D
                    + square(continentalness.distance(target.continentalness)) * 1.15D
                    + square(erosion.distance(target.erosion)) * 1.05D
                    + square(depth.distance(target.depth)) * 2.25D
                    + square(weirdness.distance(target.weirdness)) * 0.75D;
        }

        private static double square(double value) {
            return value * value;
        }
    }

    private static final class CandidateChoice {
        private final ModernCaveTerrainCaveBiomeType type;
        private final ResourceLocation biomeId;
        private final double distance;
        private final double secondDistance;
        private final double edgeFactor;

        private CandidateChoice(ModernCaveTerrainCaveBiomeType type, ResourceLocation biomeId, double distance,
                                double secondDistance, double edgeFactor) {
            this.type = type;
            this.biomeId = biomeId;
            this.distance = distance;
            this.secondDistance = secondDistance;
            this.edgeFactor = edgeFactor;
        }
    }
}
