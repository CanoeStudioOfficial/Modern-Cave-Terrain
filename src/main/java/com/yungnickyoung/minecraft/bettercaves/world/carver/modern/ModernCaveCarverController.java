package com.yungnickyoung.minecraft.bettercaves.world.carver.modern;

import com.yungnickyoung.minecraft.bettercaves.BetterCaves;
import com.yungnickyoung.minecraft.bettercaves.config.util.ConfigHolder;
import com.yungnickyoung.minecraft.bettercaves.noise.OpenSimplex2S;
import com.yungnickyoung.minecraft.bettercaves.world.carver.CarverUtils;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

public class ModernCaveCarverController {
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
    private final float noodleSpawnChance;
    private final float noodleThickness;
    private final float spaghettiSpawnChance;
    private final float spaghettiThickness;
    private final float canyonSpawnChance;
    private final float canyonWidth;

    private final OpenSimplex2S cheeseRegionNoise;
    private final OpenSimplex2S cheeseShapeNoise;
    private final OpenSimplex2S cheeseDetailNoise;
    private final OpenSimplex2S cheeseWarpNoise;
    private final OpenSimplex2S spaghettiRegionNoise;
    private final OpenSimplex2S spaghettiANoise;
    private final OpenSimplex2S spaghettiBNoise;
    private final OpenSimplex2S spaghettiRoughnessNoise;
    private final OpenSimplex2S noodleRegionNoise;
    private final OpenSimplex2S noodleANoise;
    private final OpenSimplex2S noodleBNoise;
    private final OpenSimplex2S noodleToggleNoise;
    private final OpenSimplex2S canyonRegionNoise;
    private final OpenSimplex2S canyonAxisNoise;
    private final OpenSimplex2S canyonWarpNoise;
    private final OpenSimplex2S canyonWallNoise;

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
        this.noodleSpawnChance = clamp01(config.noodleCaveSpawnChance.get() / 100f);
        this.noodleThickness = config.noodleCaveThickness.get();
        this.spaghettiSpawnChance = clamp01(config.spaghettiCaveSpawnChance.get() / 100f);
        this.spaghettiThickness = config.spaghettiCaveThickness.get();
        this.canyonSpawnChance = clamp01(config.canyonSpawnChance.get() / 100f);
        this.canyonWidth = config.canyonWidth.get();
        this.aquiferSampler = new AquiferSampler(worldIn, config);

        long seed = worldIn.getSeed();
        this.cheeseRegionNoise = new OpenSimplex2S(seed + 901);
        this.cheeseShapeNoise = new OpenSimplex2S(seed + 902);
        this.cheeseDetailNoise = new OpenSimplex2S(seed + 903);
        this.cheeseWarpNoise = new OpenSimplex2S(seed + 904);
        this.spaghettiRegionNoise = new OpenSimplex2S(seed + 911);
        this.spaghettiANoise = new OpenSimplex2S(seed + 912);
        this.spaghettiBNoise = new OpenSimplex2S(seed + 913);
        this.spaghettiRoughnessNoise = new OpenSimplex2S(seed + 914);
        this.noodleRegionNoise = new OpenSimplex2S(seed + 921);
        this.noodleANoise = new OpenSimplex2S(seed + 922);
        this.noodleBNoise = new OpenSimplex2S(seed + 923);
        this.noodleToggleNoise = new OpenSimplex2S(seed + 924);
        this.canyonRegionNoise = new OpenSimplex2S(seed + 931);
        this.canyonAxisNoise = new OpenSimplex2S(seed + 932);
        this.canyonWarpNoise = new OpenSimplex2S(seed + 933);
        this.canyonWallNoise = new OpenSimplex2S(seed + 934);

        if (config.modernCaveBottom.get() > config.modernCaveTop.get()) {
            BetterCaves.LOGGER.warn("Warning: Min altitude for modern caves should not be greater than max altitude. Values were swapped.");
        }
    }

    public void carveChunk(ChunkPrimer primer, int chunkX, int chunkZ, int[][] surfaceAltitudes, IBlockState[][] liquidBlocks) {
        if (!enabled || (!cheeseCavesEnabled && !noodleCavesEnabled && !spaghettiCavesEnabled && !canyonsEnabled)) {
            return;
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

                boolean canyonColumn = canyonsEnabled && isCanyonColumn(blockX, blockZ);
                IBlockState fallbackLiquidBlock = liquidBlocks[localX][localZ];

                for (int y = columnTopY; y >= bottomY; y--) {
                    IBlockState debugBlock = getDebugBlockForCarve(blockX, y, blockZ, surfaceAltitude, canyonColumn);
                    if (debugBlock == null) {
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

    private IBlockState getDebugBlockForCarve(int x, int y, int z, int surfaceAltitude, boolean canyonColumn) {
        if (canyonColumn && shouldCarveCanyonAtY(x, y, z)) {
            return CANYON_DEBUG_BLOCK;
        }
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

        double warpX = noise3(cheeseWarpNoise, x, y, z, .014) * 10.0;
        double warpY = noise3(cheeseWarpNoise, x + 73, y - 31, z + 19, .014) * 5.0;
        double warpZ = noise3(cheeseWarpNoise, x - 41, y + 17, z + 97, .014) * 10.0;
        double broad = normalize(noise3(cheeseShapeNoise, x + warpX, y * .72 + warpY, z + warpZ, .017));
        double pockets = 1.0 - Math.abs(noise3(cheeseDetailNoise, x + warpX * .35, y * .85 + warpY, z + warpZ * .35, .045));
        double density = broad * .55 + pockets * .45;
        double threshold = .46 + (1.0 - cheeseCaveSize) * .28 + (1.0 - fade) * .25;

        return density > threshold;
    }

    private boolean shouldCarveSpaghetti(int x, int y, int z, int surfaceAltitude) {
        if (!passesRegionChance(spaghettiRegionNoise, x, z, spaghettiSpawnChance, .006)) {
            return false;
        }

        float fade = caveFade(y, surfaceAltitude, false);
        if (fade <= 0f) {
            return false;
        }

        double roughness = normalize(noise3(spaghettiRoughnessNoise, x, y, z, .075));
        double thickness = spaghettiThickness * (.75 + roughness * .65) * fade;
        double a = Math.abs(noise3(spaghettiANoise, x, y * .66, z, .038));
        double b = Math.abs(noise3(spaghettiBNoise, x + 103, y * .66, z - 79, .038));

        return Math.max(a, b) < thickness;
    }

    private boolean shouldCarveNoodle(int x, int y, int z, int surfaceAltitude) {
        if (!passesRegionChance(noodleRegionNoise, x, z, noodleSpawnChance, .009)) {
            return false;
        }

        float fade = caveFade(y, surfaceAltitude, false);
        if (fade <= 0f) {
            return false;
        }

        double toggle = noise3(noodleToggleNoise, x, y, z, .026);
        if (toggle < -.18) {
            return false;
        }

        double thickness = noodleThickness * (.75 + normalize(toggle) * .5) * fade;
        double a = Math.abs(noise3(noodleANoise, x, y, z, .086));
        double b = Math.abs(noise3(noodleBNoise, x - 47, y + 23, z + 61, .086));

        return Math.max(a, b) < thickness;
    }

    private boolean isCanyonColumn(int x, int z) {
        if (!passesRegionChance(canyonRegionNoise, x, z, canyonSpawnChance, .0018)) {
            return false;
        }

        double warpX = canyonWarpNoise.noise2(x * .01, z * .01) * 22.0;
        double warpZ = canyonWarpNoise.noise2((x + 211) * .01, (z - 157) * .01) * 22.0;
        double axis = Math.abs(canyonAxisNoise.noise2((x + warpX) * .0065, (z + warpZ) * .0065));
        double roughness = normalize(canyonWallNoise.noise2(x * .035, z * .035));
        double width = canyonWidth * (.65 + roughness * .8);

        return axis < width;
    }

    private boolean shouldCarveCanyonAtY(int x, int y, int z) {
        if (y <= bottomY + 2) {
            return false;
        }

        float bottomFade = smoothStep((float)(y - bottomY - 2) / 10f);
        double ledgeNoise = normalize(noise3(canyonWallNoise, x, y, z, .07));

        return bottomFade > .35f || ledgeNoise > .52;
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

    private double noise3(OpenSimplex2S noise, double x, double y, double z, double frequency) {
        return noise.noise3_XZBeforeY(x * frequency, y * frequency, z * frequency);
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
}
