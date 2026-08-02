package com.lonelyxiya.minecraft.moderncaveterrain.world.carver.cave.mojang;

import com.lonelyxiya.minecraft.moderncaveterrain.ModernCaveTerrain;
import com.lonelyxiya.minecraft.moderncaveterrain.noise.MojangNormalNoise;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.FlatGeneratorInfo;
import net.minecraft.world.gen.FlatLayerInfo;
import net.minecraft.world.gen.NoiseGeneratorOctaves;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Supplies the world-coordinate preliminary surface used by modern aquifers.
 *
 * <p>Modern Minecraft evaluates a terrain density function at any requested x/z coordinate. A 1.12.2
 * {@code ChunkPrimer}, on the other hand, only exposes the current 16x16 chunk. Clamping out-of-chunk samples to
 * that primer's edge makes the same aquifer cell receive a different fluid status from adjacent chunks. For the
 * vanilla overworld generator this class evaluates the original 1.12.2 coarse terrain density column directly;
 * other generators receive a deterministic biome/noise fallback. Neither path loads or generates neighboring
 * chunks.</p>
 */
final class Mojang118PreliminarySurfaceSampler {
    private static final int QUARTS_PER_CHUNK = 4;
    private static final int BIOME_SAMPLE_BORDER = 2;
    private static final int BIOME_SAMPLE_WIDTH = QUARTS_PER_CHUNK + BIOME_SAMPLE_BORDER * 2;
    private static final int DENSITY_COLUMN_SIZE = 33;
    private static final int VERTICAL_CELL_HEIGHT = 8;
    private static final int MAX_CACHED_CHUNKS = 512;

    private final World world;
    private final WorldType terrainType;
    private final float[] biomeWeights = new float[25];
    private final MojangNormalNoise fallbackSurfaceNoise;
    private final int flatSurfaceY;
    private final NoiseGeneratorOctaves minLimitNoise;
    private final NoiseGeneratorOctaves maxLimitNoise;
    private final NoiseGeneratorOctaves mainNoise;
    private final NoiseGeneratorOctaves depthNoise;
    private final ChunkGeneratorSettings settings;
    private final Map<Long, int[]> surfaceCache = new LinkedHashMap<Long, int[]>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, int[]> eldest) {
            return size() > MAX_CACHED_CHUNKS;
        }
    };

    Mojang118PreliminarySurfaceSampler(World world) {
        this.world = world;
        this.terrainType = world.getWorldInfo().getTerrainType();
        this.fallbackSurfaceNoise = MojangNormalNoise.create(world.getSeed(), "preliminary_surface", -6, 1.0D);
        this.flatSurfaceY = terrainType == WorldType.FLAT ? findFlatSurfaceY(world) : -1;
        buildBiomeWeights();

        NoiseGeneratorOctaves minLimit = null;
        NoiseGeneratorOctaves maxLimit = null;
        NoiseGeneratorOctaves main = null;
        NoiseGeneratorOctaves depth = null;
        ChunkGeneratorSettings generatorSettings = null;

        try {
            IChunkProvider provider = world.getChunkProvider();
            IChunkGenerator generator = provider instanceof ChunkProviderServer
                    ? ((ChunkProviderServer) provider).chunkGenerator
                    : null;
            if (generator instanceof ChunkGeneratorOverworld) {
                ChunkGeneratorOverworld overworld = (ChunkGeneratorOverworld) generator;
                minLimit = ObfuscationReflectionHelper.getPrivateValue(
                        ChunkGeneratorOverworld.class, overworld, "field_185991_j");
                maxLimit = ObfuscationReflectionHelper.getPrivateValue(
                        ChunkGeneratorOverworld.class, overworld, "field_185992_k");
                main = ObfuscationReflectionHelper.getPrivateValue(
                        ChunkGeneratorOverworld.class, overworld, "field_185993_l");
                generatorSettings = ObfuscationReflectionHelper.getPrivateValue(
                        ChunkGeneratorOverworld.class, overworld, "field_186000_s");
                depth = overworld.depthNoise;
            }
        }
        catch (RuntimeException exception) {
            ModernCaveTerrain.LOGGER.warn("Unable to access the 1.12.2 terrain density sampler; "
                    + "using a deterministic preliminary-surface fallback.", exception);
        }

        this.minLimitNoise = minLimit;
        this.maxLimitNoise = maxLimit;
        this.mainNoise = main;
        this.depthNoise = depth;
        this.settings = generatorSettings;
    }

    synchronized int sampleSurfaceY(int blockX, int blockZ) {
        if (flatSurfaceY >= 0) {
            return flatSurfaceY;
        }
        int quartX = floorDiv(blockX, 4);
        int quartZ = floorDiv(blockZ, 4);
        int chunkX = floorDiv(quartX, QUARTS_PER_CHUNK);
        int chunkZ = floorDiv(quartZ, QUARTS_PER_CHUNK);
        long key = chunkKey(chunkX, chunkZ);
        int[] surfaces = surfaceCache.get(key);
        if (surfaces == null) {
            surfaces = canSampleOverworldDensity()
                    ? computeOverworldSurfaces(chunkX, chunkZ)
                    : computeFallbackSurfaces(chunkX, chunkZ);
            surfaceCache.put(key, surfaces);
        }

        int localQuartX = floorMod(quartX, QUARTS_PER_CHUNK);
        int localQuartZ = floorMod(quartZ, QUARTS_PER_CHUNK);
        return surfaces[columnIndex(localQuartX, localQuartZ)];
    }

    private boolean canSampleOverworldDensity() {
        return minLimitNoise != null && maxLimitNoise != null && mainNoise != null
                && depthNoise != null && settings != null;
    }

    private int[] computeOverworldSurfaces(int chunkX, int chunkZ) {
        int quartStartX = chunkX * QUARTS_PER_CHUNK;
        int quartStartZ = chunkZ * QUARTS_PER_CHUNK;
        Biome[] biomes = world.getBiomeProvider().getBiomesForGeneration(null,
                quartStartX - BIOME_SAMPLE_BORDER, quartStartZ - BIOME_SAMPLE_BORDER,
                BIOME_SAMPLE_WIDTH, BIOME_SAMPLE_WIDTH);
        double[] depthRegion = depthNoise.generateNoiseOctaves(null, quartStartX, quartStartZ,
                QUARTS_PER_CHUNK, QUARTS_PER_CHUNK, settings.depthNoiseScaleX,
                settings.depthNoiseScaleZ, settings.depthNoiseScaleExponent);
        double[] mainRegion = mainNoise.generateNoiseOctaves(null, quartStartX, 0, quartStartZ,
                QUARTS_PER_CHUNK, DENSITY_COLUMN_SIZE, QUARTS_PER_CHUNK,
                settings.coordinateScale / settings.mainNoiseScaleX,
                settings.heightScale / settings.mainNoiseScaleY,
                settings.coordinateScale / settings.mainNoiseScaleZ);
        double[] minLimitRegion = minLimitNoise.generateNoiseOctaves(null, quartStartX, 0, quartStartZ,
                QUARTS_PER_CHUNK, DENSITY_COLUMN_SIZE, QUARTS_PER_CHUNK,
                settings.coordinateScale, settings.heightScale, settings.coordinateScale);
        double[] maxLimitRegion = maxLimitNoise.generateNoiseOctaves(null, quartStartX, 0, quartStartZ,
                QUARTS_PER_CHUNK, DENSITY_COLUMN_SIZE, QUARTS_PER_CHUNK,
                settings.coordinateScale, settings.heightScale, settings.coordinateScale);

        int[] surfaces = new int[QUARTS_PER_CHUNK * QUARTS_PER_CHUNK];
        double[] densityColumn = new double[DENSITY_COLUMN_SIZE];
        int horizontalIndex = 0;
        int noiseIndex = 0;
        for (int localQuartX = 0; localQuartX < QUARTS_PER_CHUNK; localQuartX++) {
            for (int localQuartZ = 0; localQuartZ < QUARTS_PER_CHUNK; localQuartZ++) {
                Biome centerBiome = biomes[biomeIndex(localQuartX + BIOME_SAMPLE_BORDER,
                        localQuartZ + BIOME_SAMPLE_BORDER)];
                float averagedScale = 0.0F;
                float averagedDepth = 0.0F;
                float totalWeight = 0.0F;

                for (int dx = -BIOME_SAMPLE_BORDER; dx <= BIOME_SAMPLE_BORDER; dx++) {
                    for (int dz = -BIOME_SAMPLE_BORDER; dz <= BIOME_SAMPLE_BORDER; dz++) {
                        Biome biome = biomes[biomeIndex(localQuartX + dx + BIOME_SAMPLE_BORDER,
                                localQuartZ + dz + BIOME_SAMPLE_BORDER)];
                        float biomeDepth = settings.biomeDepthOffSet
                                + biome.getBaseHeight() * settings.biomeDepthWeight;
                        float biomeScale = settings.biomeScaleOffset
                                + biome.getHeightVariation() * settings.biomeScaleWeight;
                        if (terrainType == WorldType.AMPLIFIED && biomeDepth > 0.0F) {
                            biomeDepth = 1.0F + biomeDepth * 2.0F;
                            biomeScale = 1.0F + biomeScale * 4.0F;
                        }

                        float weight = biomeWeights[(dx + BIOME_SAMPLE_BORDER)
                                + (dz + BIOME_SAMPLE_BORDER) * 5] / (biomeDepth + 2.0F);
                        if (biome.getBaseHeight() > centerBiome.getBaseHeight()) {
                            weight *= 0.5F;
                        }
                        averagedScale += biomeScale * weight;
                        averagedDepth += biomeDepth * weight;
                        totalWeight += weight;
                    }
                }

                averagedScale = averagedScale / totalWeight * 0.9F + 0.1F;
                averagedDepth = (averagedDepth / totalWeight * 4.0F - 1.0F) / 8.0F;
                double depthOffset = shapeDepthNoise(depthRegion[horizontalIndex++]);
                double terrainDepth = averagedDepth + depthOffset * 0.2D;
                terrainDepth = terrainDepth * settings.baseSize / 8.0D;
                double densityCenterY = settings.baseSize + terrainDepth * 4.0D;

                for (int cellY = 0; cellY < DENSITY_COLUMN_SIZE; cellY++) {
                    double verticalGradient = (cellY - densityCenterY) * settings.stretchY
                            * 128.0D / 256.0D / averagedScale;
                    if (verticalGradient < 0.0D) {
                        verticalGradient *= 4.0D;
                    }

                    double lower = minLimitRegion[noiseIndex] / settings.lowerLimitScale;
                    double upper = maxLimitRegion[noiseIndex] / settings.upperLimitScale;
                    double blend = (mainRegion[noiseIndex] / 10.0D + 1.0D) / 2.0D;
                    double density = MathHelper.clampedLerp(lower, upper, blend) - verticalGradient;
                    if (cellY > 29) {
                        double topSlide = (cellY - 29) / 3.0D;
                        density = density * (1.0D - topSlide) - 10.0D * topSlide;
                    }
                    densityColumn[cellY] = density;
                    noiseIndex++;
                }

                surfaces[columnIndex(localQuartX, localQuartZ)] = surfaceLevelFromDensityColumn(densityColumn);
            }
        }
        return surfaces;
    }

    private int[] computeFallbackSurfaces(int chunkX, int chunkZ) {
        int[] surfaces = new int[QUARTS_PER_CHUNK * QUARTS_PER_CHUNK];
        int blockStartX = chunkX * 16;
        int blockStartZ = chunkZ * 16;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int localQuartX = 0; localQuartX < QUARTS_PER_CHUNK; localQuartX++) {
            for (int localQuartZ = 0; localQuartZ < QUARTS_PER_CHUNK; localQuartZ++) {
                int blockX = blockStartX + localQuartX * 4;
                int blockZ = blockStartZ + localQuartZ * 4;
                pos.setPos(blockX, 0, blockZ);
                Biome biome = world.getBiomeProvider().getBiome(pos);
                double variation = 4.0D + Math.max(0.0D, biome.getHeightVariation()) * 8.0D;
                double noise = fallbackSurfaceNoise.getValue(blockX * 0.01D, 0.0D, blockZ * 0.01D);
                int surfaceY = (int) Math.round(world.getSeaLevel() + biome.getBaseHeight() * 17.0D
                        + noise * variation);
                surfaces[columnIndex(localQuartX, localQuartZ)] = clamp(surfaceY, 1, 255);
            }
        }
        return surfaces;
    }

    static int surfaceLevelFromDensityColumn(double[] densityColumn) {
        int highestSolidY = -1;
        for (int cellY = 0; cellY < DENSITY_COLUMN_SIZE - 1; cellY++) {
            double lowerDensity = densityColumn[cellY];
            double step = (densityColumn[cellY + 1] - lowerDensity) / VERTICAL_CELL_HEIGHT;
            for (int offsetY = 0; offsetY < VERTICAL_CELL_HEIGHT; offsetY++) {
                if (lowerDensity + step * offsetY > 0.0D) {
                    highestSolidY = cellY * VERTICAL_CELL_HEIGHT + offsetY;
                }
            }
        }
        return clamp(highestSolidY + 1, 1, 255);
    }

    private void buildBiomeWeights() {
        for (int dx = -BIOME_SAMPLE_BORDER; dx <= BIOME_SAMPLE_BORDER; dx++) {
            for (int dz = -BIOME_SAMPLE_BORDER; dz <= BIOME_SAMPLE_BORDER; dz++) {
                biomeWeights[(dx + BIOME_SAMPLE_BORDER) + (dz + BIOME_SAMPLE_BORDER) * 5]
                        = 10.0F / MathHelper.sqrt(dx * dx + dz * dz + 0.2F);
            }
        }
    }

    private double shapeDepthNoise(double value) {
        double shaped = value / 8000.0D;
        if (shaped < 0.0D) {
            shaped = -shaped * 0.3D;
        }
        shaped = shaped * 3.0D - 2.0D;
        if (shaped < 0.0D) {
            shaped /= 2.0D;
            shaped = Math.max(shaped, -1.0D);
            shaped /= 1.4D;
            return shaped / 2.0D;
        }
        return Math.min(shaped, 1.0D) / 8.0D;
    }

    private static int biomeIndex(int x, int z) {
        return x + z * BIOME_SAMPLE_WIDTH;
    }

    private static int columnIndex(int x, int z) {
        return x * QUARTS_PER_CHUNK + z;
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ ((long) chunkZ & 0xffffffffL);
    }

    private static int floorDiv(int value, int divisor) {
        int result = value / divisor;
        if ((value ^ divisor) < 0 && result * divisor != value) {
            result--;
        }
        return result;
    }

    private static int floorMod(int value, int divisor) {
        return value - floorDiv(value, divisor) * divisor;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int findFlatSurfaceY(World world) {
        FlatGeneratorInfo info = FlatGeneratorInfo.createFlatGeneratorFromString(
                world.getWorldInfo().getGeneratorOptions());
        int surfaceY = 1;
        for (FlatLayerInfo layer : info.getFlatLayers()) {
            Material material = layer.getLayerMaterial().getMaterial();
            if (material != Material.AIR && material != Material.WATER && material != Material.LAVA) {
                surfaceY = Math.max(surfaceY, layer.getMinY() + layer.getLayerCount());
            }
        }
        return clamp(surfaceY, 1, 255);
    }
}
