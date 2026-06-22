package com.yungnickyoung.minecraft.bettercaves.world.carver.modern;

import com.yungnickyoung.minecraft.bettercaves.BetterCaves;
import com.yungnickyoung.minecraft.bettercaves.config.util.ConfigHolder;
import com.yungnickyoung.minecraft.bettercaves.noise.OpenSimplex2S;
import com.yungnickyoung.minecraft.bettercaves.world.carver.CarverUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Random;

public class ModernCaveCarverController {
    private static final int CANYON_CELL_CHUNKS = 8;
    private static final int CANYON_SEARCH_RADIUS = 2;
    private static final IBlockState CHEESE_DEBUG_BLOCK = Blocks.LAPIS_BLOCK.getDefaultState();
    private static final IBlockState SPAGHETTI_DEBUG_BLOCK = Blocks.QUARTZ_BLOCK.getDefaultState();
    private static final IBlockState NOODLE_DEBUG_BLOCK = Blocks.IRON_BLOCK.getDefaultState();
    private static final IBlockState CANYON_DEBUG_BLOCK = Blocks.DIAMOND_BLOCK.getDefaultState();

    private final World world;
    private final AquiferSampler aquiferSampler;

    private final boolean enabled;
    private final boolean debugVisualizerEnabled;
    private final boolean overrideSurfaceDetection;
    private final boolean replaceFloatingGravel;
    private final boolean cheeseCavesEnabled;
    private final boolean noodleCavesEnabled;
    private final boolean spaghettiCavesEnabled;
    private final boolean canyonsEnabled;

    private final int bottomY;
    private final int topY;
    private final int liquidAltitude;
    private final float cheeseSpawnChance;
    private final float cheeseCaveSize;
    private final float cheeseNoiseThreshold;
    private final float cheeseDetailWeight;
    private final float noodleSpawnChance;
    private final float noodleThickness;
    private final float noodleToggleThreshold;
    private final float spaghettiSpawnChance;
    private final float spaghettiThickness;
    private final float spaghettiRoughnessFrequency;
    private final float canyonSpawnChance;
    private final float canyonWidth;
    private final IBlockState canyonLavaBlock;

    private final OpenSimplex2S cheeseRegionNoise;
    private final OpenSimplex2S spaghettiRegionNoise;
    private final OpenSimplex2S spaghettiRoughnessNoise;
    private final OpenSimplex2S noodleRegionNoise;
    private final OpenSimplex2S canyonWallNoise;
    private final ProgrammableNoiseSampler cheeseSampler;
    private final ProgrammableNoiseSampler cheeseDetailSampler;
    private final ProgrammableNoiseSampler spaghettiASampler;
    private final ProgrammableNoiseSampler spaghettiBSampler;
    private final ProgrammableNoiseSampler noodleASampler;
    private final ProgrammableNoiseSampler noodleBSampler;
    private final ProgrammableNoiseSampler noodleToggleSampler;

    public ModernCaveCarverController(World worldIn, ConfigHolder config) {
        this.world = worldIn;
        this.enabled = config.enableModernCaves.get();
        this.debugVisualizerEnabled = config.debugVisualizer.get();
        this.overrideSurfaceDetection = config.overrideSurfaceDetection.get();
        this.replaceFloatingGravel = config.replaceFloatingGravel.get();
        this.cheeseCavesEnabled = config.enableCheeseCaves.get();
        this.noodleCavesEnabled = config.enableNoodleCaves.get();
        this.spaghettiCavesEnabled = config.enableSpaghettiCaves.get();
        this.canyonsEnabled = config.enableModernCanyons.get();
        this.bottomY = clamp(Math.min(config.modernCaveBottom.get(), config.modernCaveTop.get()), 1, 254);
        this.topY = clamp(Math.max(config.modernCaveBottom.get(), config.modernCaveTop.get()), 1, 254);
        this.liquidAltitude = config.liquidAltitude.get();
        this.cheeseSpawnChance = clamp01(config.cheeseCaveSpawnChance.get() / 100f);
        this.cheeseCaveSize = clamp01(config.cheeseCaveSize.get());
        this.cheeseNoiseThreshold = config.cheeseCaveNoiseThreshold.get();
        this.cheeseDetailWeight = clamp(config.cheeseCaveDetailWeight.get(), 0f, 1f);
        this.noodleSpawnChance = clamp01(config.noodleCaveSpawnChance.get() / 100f);
        this.noodleThickness = config.noodleCaveThickness.get();
        this.noodleToggleThreshold = config.noodleCaveToggleThreshold.get();
        this.spaghettiSpawnChance = clamp01(config.spaghettiCaveSpawnChance.get() / 100f);
        this.spaghettiThickness = config.spaghettiCaveThickness.get();
        this.spaghettiRoughnessFrequency = config.spaghettiCaveRoughnessFrequency.get();
        this.canyonSpawnChance = clamp01(config.canyonSpawnChance.get() / 100f);
        this.canyonWidth = config.canyonWidth.get();
        this.canyonLavaBlock = getBlockFromString(config.lavaBlock.get(), Blocks.LAVA.getDefaultState());
        this.aquiferSampler = new AquiferSampler(worldIn, config);

        long seed = worldIn.getSeed();
        this.cheeseRegionNoise = new OpenSimplex2S(seed + 901);
        this.spaghettiRegionNoise = new OpenSimplex2S(seed + 911);
        this.spaghettiRoughnessNoise = new OpenSimplex2S(seed + 914);
        this.noodleRegionNoise = new OpenSimplex2S(seed + 921);
        this.canyonWallNoise = new OpenSimplex2S(seed + 934);
        this.cheeseSampler = createNoiseSampler(seed, 941, config.cheeseCaveNoiseFrequency.get(), cheeseNoiseThreshold,
            config.cheeseCaveVerticalStretch.get(), config.cheeseCavePerturbAmp.get(), config.cheeseCavePerturbFrequency.get());
        this.cheeseDetailSampler = createNoiseSampler(seed, 942, config.cheeseCaveDetailFrequency.get(), 0f,
            config.cheeseCaveVerticalStretch.get() * 1.5f, config.cheeseCavePerturbAmp.get() * .3f,
            config.cheeseCavePerturbFrequency.get() * 1.4f);
        this.spaghettiASampler = createNoiseSampler(seed, 951, config.spaghettiCaveNoiseFrequency.get(), 0f,
            config.spaghettiCaveVerticalStretch.get(), config.spaghettiCavePerturbAmp.get(), config.spaghettiCavePerturbFrequency.get());
        this.spaghettiBSampler = createNoiseSampler(seed, 952, config.spaghettiCaveNoiseFrequency.get(), 0f,
            config.spaghettiCaveVerticalStretch.get(), config.spaghettiCavePerturbAmp.get(), config.spaghettiCavePerturbFrequency.get());
        this.noodleASampler = createNoiseSampler(seed, 961, config.noodleCaveNoiseFrequency.get(), 0f,
            config.noodleCaveVerticalStretch.get(), config.noodleCavePerturbAmp.get(), config.noodleCavePerturbFrequency.get());
        this.noodleBSampler = createNoiseSampler(seed, 962, config.noodleCaveNoiseFrequency.get(), 0f,
            config.noodleCaveVerticalStretch.get(), config.noodleCavePerturbAmp.get(), config.noodleCavePerturbFrequency.get());
        this.noodleToggleSampler = createNoiseSampler(seed, 963, config.noodleCaveToggleFrequency.get(), noodleToggleThreshold,
            1f, 0f, 0f);

        if (config.modernCaveBottom.get() > config.modernCaveTop.get()) {
            BetterCaves.LOGGER.warn("Warning: Min altitude for modern caves should not be greater than max altitude. Values were swapped.");
        }
    }

    public void carveChunk(ChunkPrimer primer, int chunkX, int chunkZ, int[][] surfaceAltitudes, IBlockState[][] liquidBlocks) {
        if (!enabled || (!cheeseCavesEnabled && !noodleCavesEnabled && !spaghettiCavesEnabled && !canyonsEnabled)) {
            return;
        }

        if (canyonsEnabled) {
            carveCanyons(primer, chunkX, chunkZ, surfaceAltitudes);
        }

        for (int localX = 0; localX < 16; localX++) {
            int blockX = chunkX * 16 + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int blockZ = chunkZ * 16 + localZ;
                int surfaceAltitude = surfaceAltitudes[localX][localZ];
                int columnTopY = overrideSurfaceDetection || debugVisualizerEnabled
                    ? topY
                    : Math.min(surfaceAltitude, topY);

                if (columnTopY < bottomY) {
                    continue;
                }

                IBlockState fallbackLiquidBlock = liquidBlocks[localX][localZ];

                for (int y = columnTopY; y >= bottomY; y--) {
                    IBlockState debugBlock = getDebugBlockForCarve(blockX, y, blockZ, surfaceAltitude);
                    if (debugBlock == null) {
                        continue;
                    }

                    Material currentMaterial = primer.getBlockState(localX, y, localZ).getMaterial();
                    if (currentMaterial == Material.AIR || currentMaterial == Material.WATER || currentMaterial == Material.LAVA) {
                        continue;
                    }

                    AquiferSampler.Sample aquiferSample = aquiferSampler.sample(blockX, y, blockZ, surfaceAltitude, fallbackLiquidBlock);
                    if (aquiferSample.isBlocked()) {
                        continue;
                    }
                    if (aquiferSample.getBlockState().getMaterial() == Material.WATER &&
                        isUnsafeAquiferWaterPlacement(primer, localX, y, localZ)) {
                        continue;
                    }

                    BlockPos blockPos = new BlockPos(blockX, y, blockZ);
                    if (debugVisualizerEnabled) {
                        CarverUtils.debugDigBlock(primer, blockPos, debugBlock, true);
                    }
                    else {
                        CarverUtils.digBlock(world, primer, blockPos, aquiferSample.getBlockState(), null, -1, replaceFloatingGravel);
                    }
                }
            }
        }
    }

    private IBlockState getDebugBlockForCarve(int x, int y, int z, int surfaceAltitude) {
        if (cheeseCavesEnabled && shouldCarveCheese(x, y, z, surfaceAltitude)) {
            return CHEESE_DEBUG_BLOCK;
        }
        if (spaghettiCavesEnabled && shouldCarveSpaghetti(x, y, z, surfaceAltitude)) {
            return SPAGHETTI_DEBUG_BLOCK;
        }
        if (noodleCavesEnabled && shouldCarveNoodle(x, y, z, surfaceAltitude)) {
            return NOODLE_DEBUG_BLOCK;
        }
        return null;
    }

    private boolean shouldCarveCheese(int x, int y, int z, int surfaceAltitude) {
        if (!passesRegionChance(cheeseRegionNoise, x, z, cheeseSpawnChance, .0035)) {
            return false;
        }

        float fade = caveFade(y, surfaceAltitude, false);
        if (fade <= 0f) {
            return false;
        }

        double shape = cheeseSampler.sample(x, y, z) + cheeseDetailSampler.sample(x, y, z) * cheeseDetailWeight;
        double threshold = cheeseNoiseThreshold + (cheeseCaveSize - .5) * .75 - (1.0 - fade) * .35;

        return shape <= threshold;
    }

    private boolean shouldCarveSpaghetti(int x, int y, int z, int surfaceAltitude) {
        if (!passesRegionChance(spaghettiRegionNoise, x, z, spaghettiSpawnChance, .006)) {
            return false;
        }

        float fade = caveFade(y, surfaceAltitude, false);
        if (fade <= 0f) {
            return false;
        }

        double roughness = normalize(noise3(spaghettiRoughnessNoise, x, y, z, spaghettiRoughnessFrequency));
        double thickness = spaghettiThickness * (.75 + roughness * .65) * fade;
        return spaghettiASampler.carveBand(x, y, z, thickness) &&
            spaghettiBSampler.carveBand(x + 103, y, z - 79, thickness);
    }

    private boolean shouldCarveNoodle(int x, int y, int z, int surfaceAltitude) {
        if (!passesRegionChance(noodleRegionNoise, x, z, noodleSpawnChance, .009)) {
            return false;
        }

        float fade = caveFade(y, surfaceAltitude, false);
        if (fade <= 0f) {
            return false;
        }

        double toggle = noodleToggleSampler.sample(x, y, z);
        if (toggle < noodleToggleThreshold) {
            return false;
        }

        double thickness = noodleThickness * (.75 + normalize(toggle) * .5) * fade;
        return noodleASampler.carveBand(x, y, z, thickness) &&
            noodleBSampler.carveBand(x - 47, y + 23, z + 61, thickness);
    }

    private void carveCanyons(ChunkPrimer primer, int chunkX, int chunkZ, int[][] surfaceAltitudes) {
        int cellX = Math.floorDiv(chunkX, CANYON_CELL_CHUNKS);
        int cellZ = Math.floorDiv(chunkZ, CANYON_CELL_CHUNKS);

        for (int searchX = cellX - CANYON_SEARCH_RADIUS; searchX <= cellX + CANYON_SEARCH_RADIUS; searchX++) {
            for (int searchZ = cellZ - CANYON_SEARCH_RADIUS; searchZ <= cellZ + CANYON_SEARCH_RADIUS; searchZ++) {
                Canyon canyon = createCanyon(searchX, searchZ);
                if (canyon != null) {
                    carveCanyon(primer, chunkX, chunkZ, surfaceAltitudes, canyon);
                }
            }
        }
    }

    private Canyon createCanyon(int cellX, int cellZ) {
        Random random = new Random(mixSeed(cellX, cellZ));
        if (random.nextFloat() > canyonSpawnChance) {
            return null;
        }

        int cellSizeBlocks = CANYON_CELL_CHUNKS * 16;
        float widthScale = clamp(canyonWidth / .052f, .35f, 2.5f);
        boolean surface = random.nextFloat() < .45f;
        double originX = cellX * cellSizeBlocks + random.nextInt(cellSizeBlocks);
        double originZ = cellZ * cellSizeBlocks + random.nextInt(cellSizeBlocks);
        double yaw = random.nextDouble() * Math.PI * 2.0;
        int length = surface ? 96 + random.nextInt(96) : 80 + random.nextInt(112);
        double width = (surface ? 4.5 + random.nextDouble() * 4.5 : 3.5 + random.nextDouble() * 3.5) * widthScale;
        double curveAmplitude = (10.0 + random.nextDouble() * 22.0) * widthScale;
        int ledgeOffset = random.nextInt(11);

        if (surface) {
            int canyonBottom = 4 + random.nextInt(18);
            return new Canyon(surface, originX, originZ, yaw, length, width, curveAmplitude, 0,
                0, canyonBottom, canyonBottom <= liquidAltitude + 8, ledgeOffset, random.nextDouble() * Math.PI * 2.0);
        }

        int centerY = 28 + random.nextInt(42);
        int height = 34 + random.nextInt(46);
        int canyonBottom = Math.max(3, centerY - height / 2);
        return new Canyon(surface, originX, originZ, yaw, length, width, curveAmplitude, centerY,
            height, canyonBottom, canyonBottom <= liquidAltitude + 8, ledgeOffset, random.nextDouble() * Math.PI * 2.0);
    }

    private void carveCanyon(ChunkPrimer primer, int chunkX, int chunkZ, int[][] surfaceAltitudes, Canyon canyon) {
        double yawX = Math.cos(canyon.yaw);
        double yawZ = Math.sin(canyon.yaw);
        double normalX = -yawZ;
        double normalZ = yawX;
        int chunkBlockX = chunkX * 16;
        int chunkBlockZ = chunkZ * 16;

        for (int step = 0; step < canyon.length; step += 2) {
            double progress = canyon.length <= 1 ? 0.0 : (double)step / (double)(canyon.length - 1);
            double centeredStep = step - canyon.length * .5;
            double curve = Math.sin(progress * Math.PI * 2.0 + canyon.phase) * canyon.curveAmplitude;
            double centerX = canyon.originX + yawX * centeredStep + normalX * curve;
            double centerZ = canyon.originZ + yawZ * centeredStep + normalZ * curve;
            double stepWidth = canyon.width * (.78 + Math.sin(progress * Math.PI) * .35);
            if (centerX + stepWidth + 2.0 < chunkBlockX || centerX - stepWidth - 2.0 > chunkBlockX + 15 ||
                centerZ + stepWidth + 2.0 < chunkBlockZ || centerZ - stepWidth - 2.0 > chunkBlockZ + 15) {
                continue;
            }

            int minLocalX = clamp((int)Math.floor(centerX - stepWidth - 2.0) - chunkBlockX, 0, 15);
            int maxLocalX = clamp((int)Math.floor(centerX + stepWidth + 2.0) - chunkBlockX, 0, 15);
            int minLocalZ = clamp((int)Math.floor(centerZ - stepWidth - 2.0) - chunkBlockZ, 0, 15);
            int maxLocalZ = clamp((int)Math.floor(centerZ + stepWidth + 2.0) - chunkBlockZ, 0, 15);

            for (int localX = minLocalX; localX <= maxLocalX; localX++) {
                int blockX = chunkBlockX + localX;
                for (int localZ = minLocalZ; localZ <= maxLocalZ; localZ++) {
                    int blockZ = chunkBlockZ + localZ;
                    double dx = blockX + .5 - centerX;
                    double dz = blockZ + .5 - centerZ;
                    double radial = Math.sqrt(dx * dx + dz * dz) / stepWidth;
                    if (radial > 1.15) {
                        continue;
                    }

                    int surfaceAltitude = surfaceAltitudes[localX][localZ];
                    int yTop = getCanyonTopY(canyon, surfaceAltitude);
                    int yBottom = getCanyonBottomY(canyon);
                    if (yTop <= yBottom) {
                        continue;
                    }

                    for (int y = yTop; y >= yBottom; y--) {
                        if (!shouldCarveCanyonBlock(canyon, radial, y, yTop, yBottom, blockX, blockZ, progress)) {
                            continue;
                        }
                        carveCanyonBlock(primer, blockX, y, blockZ, canyon);
                    }
                }
            }
        }
    }

    private int getCanyonTopY(Canyon canyon, int surfaceAltitude) {
        if (canyon.surface) {
            return overrideSurfaceDetection || debugVisualizerEnabled
                ? topY
                : clamp(surfaceAltitude + 2, 1, 254);
        }

        int top = clamp(canyon.centerY + canyon.height / 2, bottomY, topY);
        if (!overrideSurfaceDetection && !debugVisualizerEnabled) {
            top = Math.min(top, surfaceAltitude - 8);
        }
        return clamp(top, 1, 254);
    }

    private int getCanyonBottomY(Canyon canyon) {
        if (canyon.surface) {
            return clamp(canyon.bottomY, bottomY, topY);
        }
        return clamp(canyon.centerY - canyon.height / 2, bottomY, topY);
    }

    private boolean shouldCarveCanyonBlock(Canyon canyon, double radial, int y, int top, int bottom, int x, int z, double progress) {
        int span = Math.max(1, top - bottom);
        double depth = (double)(top - y) / (double)span;
        double widthAtY = canyon.surface
            ? .72 + Math.sin(depth * Math.PI) * .43
            : Math.sin(depth * Math.PI);

        if (widthAtY <= .08 || radial > widthAtY) {
            return false;
        }
        return !isCanyonLedge(canyon, radial, y, x, z, progress);
    }

    private boolean isCanyonLedge(Canyon canyon, double radial, int y, int x, int z, double progress) {
        if (radial < .46 || radial > .98) {
            return false;
        }

        int band = positiveMod(y + canyon.ledgeOffset, 11);
        if (band > 1 && band < 10) {
            return false;
        }

        double ledgeNoise = normalize(noise3(canyonWallNoise, x + progress * 23.0, y, z - progress * 23.0, .075));
        return ledgeNoise > .38;
    }

    private void carveCanyonBlock(ChunkPrimer primer, int x, int y, int z, Canyon canyon) {
        BlockPos blockPos = new BlockPos(x, y, z);
        if (debugVisualizerEnabled) {
            CarverUtils.debugDigBlock(primer, blockPos, CANYON_DEBUG_BLOCK, true);
            return;
        }

        int lavaLevel = canyon.hasLavaLake ? canyon.bottomY + 2 : liquidAltitude;
        CarverUtils.digBlock(world, primer, blockPos, Blocks.AIR.getDefaultState(), canyonLavaBlock, lavaLevel, replaceFloatingGravel);
    }

    private boolean isUnsafeAquiferWaterPlacement(ChunkPrimer primer, int localX, int y, int localZ) {
        if (y <= liquidAltitude + 6) {
            return true;
        }

        if (isAirOrLava(primer, localX, y - 1, localZ)) {
            return true;
        }
        if (localX > 0 && isAirOrLava(primer, localX - 1, y, localZ)) {
            return true;
        }
        if (localX < 15 && isAirOrLava(primer, localX + 1, y, localZ)) {
            return true;
        }
        if (localZ > 0 && isAirOrLava(primer, localX, y, localZ - 1)) {
            return true;
        }
        return localZ < 15 && isAirOrLava(primer, localX, y, localZ + 1);
    }

    private boolean isAirOrLava(ChunkPrimer primer, int localX, int y, int localZ) {
        Material material = primer.getBlockState(localX, y, localZ).getMaterial();
        return material == Material.AIR || material == Material.LAVA;
    }

    private float caveFade(int y, int surfaceAltitude, boolean allowSurfaceOpenings) {
        float lowerFade = smoothStep((float)(y - bottomY) / 8f);
        float upperFade = smoothStep((float)(topY - y) / 8f);
        float fade = Math.min(lowerFade, upperFade);

        if (allowSurfaceOpenings || overrideSurfaceDetection || debugVisualizerEnabled) {
            return fade;
        }

        int depth = surfaceAltitude - y;
        if (depth <= 3) {
            return 0f;
        }
        if (depth >= 18) {
            return fade;
        }
        return fade * ((float)(depth - 3) / 15f);
    }

    private boolean passesRegionChance(OpenSimplex2S noise, int x, int z, float chance, double frequency) {
        if (chance <= 0f) {
            return false;
        }
        if (chance >= 1f) {
            return true;
        }
        return normalize(noise.noise2(x * frequency, z * frequency)) <= chance;
    }

    private static ProgrammableNoiseSampler createNoiseSampler(long seed, int seedOffset, float frequency, float threshold,
                                                               float stretch, float perturbAmp, float perturbFrequency) {
        return new ProgrammableNoiseSampler(seed + seedOffset, frequency, threshold, stretch,
            1, .5, 2.0, perturbAmp, perturbFrequency, false);
    }

    private double noise3(OpenSimplex2S noise, double x, double y, double z, double frequency) {
        return noise.noise3_XZBeforeY(x * frequency, y * frequency, z * frequency);
    }

    private IBlockState getBlockFromString(String blockName, IBlockState fallback) {
        try {
            Block block = Block.getBlockFromName(blockName);
            if (block != null) {
                return block.getDefaultState();
            }
        }
        catch (Exception ignored) {
        }
        return fallback;
    }

    private long mixSeed(int cellX, int cellZ) {
        long mixed = world.getSeed();
        mixed ^= (long)cellX * 341873128712L;
        mixed ^= (long)cellZ * 132897987541L;
        mixed = mixed * 6364136223846793005L + 1442695040888963407L;
        mixed ^= mixed >>> 33;
        return mixed;
    }

    private static int positiveMod(int value, int divisor) {
        int result = value % divisor;
        return result < 0 ? result + divisor : result;
    }

    private static double normalize(double value) {
        return clamp01((float)((value + 1.0) * .5));
    }

    private static float smoothStep(float value) {
        value = clamp01(value);
        return value * value * (3f - 2f * value);
    }

    private static float clamp01(float value) {
        if (value < 0f) {
            return 0f;
        }
        if (value > 1f) {
            return 1f;
        }
        return value;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static class Canyon {
        private final boolean surface;
        private final double originX;
        private final double originZ;
        private final double yaw;
        private final int length;
        private final double width;
        private final double curveAmplitude;
        private final int centerY;
        private final int height;
        private final int bottomY;
        private final boolean hasLavaLake;
        private final int ledgeOffset;
        private final double phase;

        private Canyon(boolean surface, double originX, double originZ, double yaw, int length, double width,
                       double curveAmplitude, int centerY, int height, int bottomY, boolean hasLavaLake,
                       int ledgeOffset, double phase) {
            this.surface = surface;
            this.originX = originX;
            this.originZ = originZ;
            this.yaw = yaw;
            this.length = length;
            this.width = width;
            this.curveAmplitude = curveAmplitude;
            this.centerY = centerY;
            this.height = height;
            this.bottomY = bottomY;
            this.hasLavaLake = hasLavaLake;
            this.ledgeOffset = ledgeOffset;
            this.phase = phase;
        }
    }
}
