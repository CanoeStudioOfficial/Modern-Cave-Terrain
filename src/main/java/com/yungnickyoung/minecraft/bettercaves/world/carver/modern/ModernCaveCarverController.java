package com.yungnickyoung.minecraft.bettercaves.world.carver.modern;

import com.yungnickyoung.minecraft.bettercaves.BetterCaves;
import com.yungnickyoung.minecraft.bettercaves.api.BetterCavesAPI;
import com.yungnickyoung.minecraft.bettercaves.config.util.ConfigHolder;
import com.yungnickyoung.minecraft.bettercaves.world.carver.CarverUtils;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

public class ModernCaveCarverController {
    private final World world;
    private final ModernNoiseCaveCarver[] noiseCarvers;
    private final ModernCanyonCarver canyonCarver;
    private final ModernCarvedPositionBuffer carvedPositions = new ModernCarvedPositionBuffer();

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
        ModernCarvingContext context = new ModernCarvingContext(world, primer, chunkX, chunkZ,
            surfaceAltitudes, liquidBlocks, hasApiCallbacks);

        if (canyonCarver.isEnabled()) {
            canyonCarver.carveChunk(context);
        }

        if (!noiseCavesEnabled) {
            return;
        }

        carvedPositions.reset();
        collectNoiseCavePositions(context, carvedPositions);
        carveNoiseCavePositions(context, carvedPositions);
    }

    private void collectNoiseCavePositions(ModernCarvingContext context, ModernCarvedPositionBuffer positions) {
        for (int localX = 0; localX < 16; localX++) {
            int blockX = context.blockX(localX);
            for (int localZ = 0; localZ < 16; localZ++) {
                int blockZ = context.blockZ(localZ);
                int surfaceAltitude = context.surfaceAltitude(localX, localZ);
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

                for (int y = columnTopY; y >= bottomY; y--) {
                    IBlockState currentState = context.primer.getBlockState(localX, y, localZ);
                    Material currentMaterial = currentState.getMaterial();
                    if (currentMaterial == Material.AIR || currentMaterial == Material.WATER || currentMaterial == Material.LAVA) {
                        continue;
                    }

                    float fade = caveFade(y, surfaceAltitude, false);
                    if (fade <= 0f) {
                        continue;
                    }

                    int carverIndex = getCarverIndexForBlock(columnCarveMask, blockX, y, blockZ, fade);
                    if (carverIndex >= 0) {
                        positions.add(localX, y, localZ, carverIndex);
                    }
                }
            }
        }
    }

    private void carveNoiseCavePositions(final ModernCarvingContext context, ModernCarvedPositionBuffer positions) {
        positions.forEach(new ModernCarvedPositionBuffer.PositionConsumer() {
            @Override
            public void accept(int localX, int y, int localZ, int carverIndex) {
                carveNoiseCavePosition(context, localX, y, localZ, noiseCarvers[carverIndex]);
            }
        });
    }

    private void carveNoiseCavePosition(ModernCarvingContext context, int localX, int y, int localZ,
                                        ModernNoiseCaveCarver carver) {
        IBlockState currentState = context.primer.getBlockState(localX, y, localZ);
        Material currentMaterial = currentState.getMaterial();
        if (currentMaterial == Material.AIR || currentMaterial == Material.WATER || currentMaterial == Material.LAVA) {
            return;
        }

        int blockX = context.blockX(localX);
        int blockZ = context.blockZ(localZ);

        if (debugVisualizerEnabled) {
            context.mutableBlockPos.setPos(blockX, y, blockZ);
            CarverUtils.debugDigBlock(context.primer, context.mutableBlockPos, carver.getDebugBlock(), true);
            return;
        }

        if (context.hasApiCallbacks) {
            BlockPos blockPos = new BlockPos(blockX, y, blockZ);
            IBlockState proposedState = y <= liquidAltitude
                ? context.fallbackLiquidBlock(localX, localZ)
                : Blocks.AIR.getDefaultState();
            if (proposedState == null) {
                return;
            }

            IBlockState carveState = BetterCavesAPI.resolveModernCaveCarvedBlock(context.world, blockPos,
                currentState, proposedState, carver.getType());
            CarverUtils.digBlock(context.world, context.primer, blockPos, carveState, null, -1,
                replaceFloatingGravel);
        }
        else {
            context.mutableBlockPos.setPos(blockX, y, blockZ);
            CarverUtils.digBlock(context.world, context.primer, context.mutableBlockPos,
                Blocks.AIR.getDefaultState(), context.fallbackLiquidBlock(localX, localZ),
                liquidAltitude, replaceFloatingGravel);
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

    private int getCarverIndexForBlock(int columnCarveMask, int x, int y, int z, float fade) {
        for (int i = 0; i < noiseCarvers.length; i++) {
            ModernNoiseCaveCarver carver = noiseCarvers[i];
            if ((columnCarveMask & carver.getColumnMask()) != 0 && carver.shouldCarve(x, y, z, fade)) {
                return i;
            }
        }
        return -1;
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
