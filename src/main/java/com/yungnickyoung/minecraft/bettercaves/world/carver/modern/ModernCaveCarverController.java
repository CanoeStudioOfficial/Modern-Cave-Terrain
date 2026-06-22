package com.yungnickyoung.minecraft.bettercaves.world.carver.modern;

import com.yungnickyoung.minecraft.bettercaves.BetterCaves;
import com.yungnickyoung.minecraft.bettercaves.api.BetterCavesAPI;
import com.yungnickyoung.minecraft.bettercaves.config.util.ConfigHolder;
import com.yungnickyoung.minecraft.bettercaves.world.carver.CarverUtils;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

public class ModernCaveCarverController {
    private final World world;
    private final AquiferSampler aquiferSampler;
    private final ModernNoiseCaveCarver[] noiseCarvers;
    private final ModernCanyonCarver canyonCarver;

    private final boolean enabled;
    private final boolean debugVisualizerEnabled;
    private final boolean overrideSurfaceDetection;
    private final boolean replaceFloatingGravel;
    private final boolean noiseCavesEnabled;

    private final int bottomY;
    private final int topY;
    private final int liquidAltitude;

    public ModernCaveCarverController(World worldIn, ConfigHolder config) {
        this.world = worldIn;
        this.enabled = config.enableModernCaves.get();
        this.debugVisualizerEnabled = config.debugVisualizer.get();
        this.overrideSurfaceDetection = config.overrideSurfaceDetection.get();
        this.replaceFloatingGravel = config.replaceFloatingGravel.get();
        this.noiseCavesEnabled = config.enableCheeseCaves.get() || config.enableNoodleCaves.get() ||
            config.enableSpaghettiCaves.get();
        this.bottomY = clamp(Math.min(config.modernCaveBottom.get(), config.modernCaveTop.get()), 1, 254);
        this.topY = clamp(Math.max(config.modernCaveBottom.get(), config.modernCaveTop.get()), 1, 254);
        this.liquidAltitude = config.liquidAltitude.get();
        this.aquiferSampler = new AquiferSampler(worldIn, config);

        long seed = worldIn.getSeed();
        this.noiseCarvers = new ModernNoiseCaveCarver[] {
            new CheeseCaveCarver(seed, config),
            new SpaghettiCaveCarver(seed, config),
            new NoodleCaveCarver(seed, config)
        };
        this.canyonCarver = new ModernCanyonCarver(worldIn, config, bottomY, topY);

        if (config.modernCaveBottom.get() > config.modernCaveTop.get()) {
            BetterCaves.LOGGER.warn("Warning: Min altitude for modern caves should not be greater than max altitude. Values were swapped.");
        }
    }

    public void carveChunk(ChunkPrimer primer, int chunkX, int chunkZ, int[][] surfaceAltitudes, IBlockState[][] liquidBlocks) {
        if (!enabled || (!noiseCavesEnabled && !canyonCarver.isEnabled())) {
            return;
        }

        boolean hasApiCallbacks = BetterCavesAPI.hasModernCaveCarvingCallbacks();
        if (canyonCarver.isEnabled()) {
            canyonCarver.carveChunk(primer, chunkX, chunkZ, surfaceAltitudes, hasApiCallbacks);
        }

        if (!noiseCavesEnabled) {
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

                int columnCarveMask = createColumnCarveMask(blockX, blockZ);
                if (columnCarveMask == 0) {
                    continue;
                }

                IBlockState fallbackLiquidBlock = liquidBlocks[localX][localZ];
                AquiferSampler.ColumnSample aquiferColumnSample = aquiferSampler.sampleColumn(blockX, blockZ, surfaceAltitude);
                BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

                for (int y = columnTopY; y >= bottomY; y--) {
                    IBlockState currentState = primer.getBlockState(localX, y, localZ);
                    Material currentMaterial = currentState.getMaterial();
                    if (currentMaterial == Material.AIR || currentMaterial == Material.WATER || currentMaterial == Material.LAVA) {
                        continue;
                    }

                    float fade = caveFade(y, surfaceAltitude, false);
                    if (fade <= 0f) {
                        continue;
                    }

                    ModernNoiseCaveCarver carver = getCarverForBlock(columnCarveMask, blockX, y, blockZ, fade);
                    if (carver == null) {
                        continue;
                    }

                    AquiferSampler.Sample aquiferSample = aquiferSampler.sample(blockX, y, blockZ, surfaceAltitude,
                        fallbackLiquidBlock, aquiferColumnSample);
                    if (aquiferSample.isBlocked()) {
                        continue;
                    }
                    if (aquiferSample.getBlockState().getMaterial() == Material.WATER &&
                        isUnsafeAquiferWaterPlacement(primer, localX, y, localZ)) {
                        continue;
                    }

                    if (debugVisualizerEnabled) {
                        mutableBlockPos.setPos(blockX, y, blockZ);
                        CarverUtils.debugDigBlock(primer, mutableBlockPos, carver.getDebugBlock(), true);
                    }
                    else {
                        IBlockState carveState = aquiferSample.getBlockState();
                        if (hasApiCallbacks) {
                            BlockPos blockPos = new BlockPos(blockX, y, blockZ);
                            carveState = BetterCavesAPI.resolveModernCaveCarvedBlock(world, blockPos,
                                currentState, carveState, carver.getType());
                            CarverUtils.digBlock(world, primer, blockPos, carveState, null, -1, replaceFloatingGravel);
                        }
                        else {
                            mutableBlockPos.setPos(blockX, y, blockZ);
                            CarverUtils.digBlock(world, primer, mutableBlockPos, carveState, null, -1, replaceFloatingGravel);
                        }
                    }
                }
            }
        }
    }

    private int createColumnCarveMask(int x, int z) {
        int mask = 0;
        for (ModernNoiseCaveCarver carver : noiseCarvers) {
            if (carver.isColumnActive(x, z)) {
                mask |= carver.getColumnMask();
            }
        }
        return mask;
    }

    private ModernNoiseCaveCarver getCarverForBlock(int columnCarveMask, int x, int y, int z, float fade) {
        for (ModernNoiseCaveCarver carver : noiseCarvers) {
            if ((columnCarveMask & carver.getColumnMask()) != 0 && carver.shouldCarve(x, y, z, fade)) {
                return carver;
            }
        }
        return null;
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
