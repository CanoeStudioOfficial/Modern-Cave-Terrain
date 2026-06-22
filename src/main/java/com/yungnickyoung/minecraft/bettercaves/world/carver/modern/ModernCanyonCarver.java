package com.yungnickyoung.minecraft.bettercaves.world.carver.modern;

import com.yungnickyoung.minecraft.bettercaves.api.BetterCavesAPI;
import com.yungnickyoung.minecraft.bettercaves.api.ModernCaveCarvingType;
import com.yungnickyoung.minecraft.bettercaves.config.util.ConfigHolder;
import com.yungnickyoung.minecraft.bettercaves.noise.OpenSimplex2S;
import com.yungnickyoung.minecraft.bettercaves.world.carver.CarverUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

class ModernCanyonCarver {
    private static final int CANYON_CELL_CHUNKS = 10;
    private static final int CANYON_SEARCH_RADIUS = 2;
    private static final int CANYON_CACHE_SIZE = 4096;
    private static final float SURFACE_CANYON_CHANCE = .22f;
    private static final IBlockState CANYON_DEBUG_BLOCK = Blocks.DIAMOND_BLOCK.getDefaultState();

    private final World world;
    private final boolean enabled;
    private final boolean debugVisualizerEnabled;
    private final boolean overrideSurfaceDetection;
    private final boolean replaceFloatingGravel;
    private final int bottomY;
    private final int topY;
    private final int liquidAltitude;
    private final float canyonSpawnChance;
    private final float canyonWidth;
    private final IBlockState canyonLavaBlock;
    private final OpenSimplex2S canyonWallNoise;
    private final Map<Long, Canyon> canyonCache = new LinkedHashMap<Long, Canyon>(CANYON_CACHE_SIZE, .75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Canyon> eldest) {
            return size() > CANYON_CACHE_SIZE;
        }
    };

    ModernCanyonCarver(World world, ConfigHolder config, int bottomY, int topY) {
        this.world = world;
        this.enabled = config.enableModernCanyons.get();
        this.debugVisualizerEnabled = config.debugVisualizer.get();
        this.overrideSurfaceDetection = config.overrideSurfaceDetection.get();
        this.replaceFloatingGravel = config.replaceFloatingGravel.get();
        this.bottomY = bottomY;
        this.topY = topY;
        this.liquidAltitude = config.liquidAltitude.get();
        this.canyonSpawnChance = clamp01(config.canyonSpawnChance.get() / 100f);
        this.canyonWidth = config.canyonWidth.get();
        this.canyonLavaBlock = getBlockFromString(config.lavaBlock.get(), Blocks.LAVA.getDefaultState());
        this.canyonWallNoise = new OpenSimplex2S(world.getSeed() + 934);
    }

    boolean isEnabled() {
        return enabled;
    }

    void carveChunk(ModernCarvingContext context) {
        int cellX = Math.floorDiv(context.chunkX, CANYON_CELL_CHUNKS);
        int cellZ = Math.floorDiv(context.chunkZ, CANYON_CELL_CHUNKS);

        for (int searchX = cellX - CANYON_SEARCH_RADIUS; searchX <= cellX + CANYON_SEARCH_RADIUS; searchX++) {
            for (int searchZ = cellZ - CANYON_SEARCH_RADIUS; searchZ <= cellZ + CANYON_SEARCH_RADIUS; searchZ++) {
                Canyon canyon = getCachedCanyon(searchX, searchZ);
                if (canyon != null) {
                    carveCanyon(context, canyon);
                }
            }
        }
    }

    private Canyon getCachedCanyon(int cellX, int cellZ) {
        long cacheKey = canyonCacheKey(cellX, cellZ);
        if (canyonCache.containsKey(cacheKey)) {
            return canyonCache.get(cacheKey);
        }

        Canyon canyon = createCanyon(cellX, cellZ);
        canyonCache.put(cacheKey, canyon);
        return canyon;
    }

    private Canyon createCanyon(int cellX, int cellZ) {
        Random random = new Random(mixSeed(cellX, cellZ));
        if (random.nextFloat() > canyonSpawnChance) {
            return null;
        }

        int cellSizeBlocks = CANYON_CELL_CHUNKS * 16;
        float widthScale = clamp(canyonWidth / .052f, .35f, 2.5f);
        boolean surface = random.nextFloat() < SURFACE_CANYON_CHANCE;
        double originX = cellX * cellSizeBlocks + random.nextInt(cellSizeBlocks);
        double originZ = cellZ * cellSizeBlocks + random.nextInt(cellSizeBlocks);
        double yaw = random.nextDouble() * Math.PI * 2.0;
        int length = surface ? 72 + random.nextInt(72) : 88 + random.nextInt(104);
        double width = (surface ? 2.75 + random.nextDouble() * 3.0 : 2.5 + random.nextDouble() * 3.0) * widthScale;
        double curveAmplitude = (8.0 + random.nextDouble() * 18.0) * widthScale;
        int ledgeOffset = random.nextInt(11);

        if (surface) {
            int height = 34 + random.nextInt(28);
            int canyonBottom = Math.max(4, liquidAltitude - 3 + random.nextInt(9));
            return new Canyon(surface, originX, originZ, yaw, length, width, curveAmplitude, 0,
                height, canyonBottom, canyonBottom <= liquidAltitude + 8, ledgeOffset, random.nextDouble() * Math.PI * 2.0);
        }

        int centerY = 26 + random.nextInt(40);
        int height = 36 + random.nextInt(40);
        int canyonBottom = Math.max(3, centerY - height / 2);
        return new Canyon(surface, originX, originZ, yaw, length, width, curveAmplitude, centerY,
            height, canyonBottom, canyonBottom <= liquidAltitude + 8, ledgeOffset, random.nextDouble() * Math.PI * 2.0);
    }

    private void carveCanyon(ModernCarvingContext context, Canyon canyon) {
        double yawX = Math.cos(canyon.yaw);
        double yawZ = Math.sin(canyon.yaw);
        double normalX = -yawZ;
        double normalZ = yawX;
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        if (!canyonTouchesChunk(canyon, context.chunkBlockX, context.chunkBlockZ)) {
            return;
        }

        for (int step = 0; step < canyon.length; step += 2) {
            double progress = canyon.length <= 1 ? 0.0 : (double)step / (double)(canyon.length - 1);
            double centeredStep = step - canyon.length * .5;
            double curve = Math.sin(progress * Math.PI * 2.0 + canyon.phase) * canyon.curveAmplitude;
            double centerX = canyon.originX + yawX * centeredStep + normalX * curve;
            double centerZ = canyon.originZ + yawZ * centeredStep + normalZ * curve;
            double stepWidth = canyon.width * (.78 + Math.sin(progress * Math.PI) * .35);
            if (centerX + stepWidth + 2.0 < context.chunkBlockX || centerX - stepWidth - 2.0 > context.chunkBlockX + 15 ||
                centerZ + stepWidth + 2.0 < context.chunkBlockZ || centerZ - stepWidth - 2.0 > context.chunkBlockZ + 15) {
                continue;
            }

            int minLocalX = clamp((int)Math.floor(centerX - stepWidth - 2.0) - context.chunkBlockX, 0, 15);
            int maxLocalX = clamp((int)Math.floor(centerX + stepWidth + 2.0) - context.chunkBlockX, 0, 15);
            int minLocalZ = clamp((int)Math.floor(centerZ - stepWidth - 2.0) - context.chunkBlockZ, 0, 15);
            int maxLocalZ = clamp((int)Math.floor(centerZ + stepWidth + 2.0) - context.chunkBlockZ, 0, 15);

            for (int localX = minLocalX; localX <= maxLocalX; localX++) {
                int blockX = context.blockX(localX);
                for (int localZ = minLocalZ; localZ <= maxLocalZ; localZ++) {
                    int blockZ = context.blockZ(localZ);
                    double dx = blockX + .5 - centerX;
                    double dz = blockZ + .5 - centerZ;
                    double radial = Math.sqrt(dx * dx + dz * dz) / stepWidth;
                    if (radial > 1.15) {
                        continue;
                    }

                    int surfaceAltitude = context.surfaceAltitude(localX, localZ);
                    int yTop = getCanyonTopY(canyon, surfaceAltitude);
                    int yBottom = getCanyonBottomY(canyon, yTop);
                    if (yTop <= yBottom) {
                        continue;
                    }

                    for (int y = yTop; y >= yBottom; y--) {
                        if (!shouldCarveCanyonBlock(canyon, radial, y, yTop, yBottom, blockX, blockZ, progress)) {
                            continue;
                        }
                        carveCanyonBlock(context, blockX, y, blockZ, canyon, mutableBlockPos);
                    }
                }
            }
        }
    }

    private boolean canyonTouchesChunk(Canyon canyon, int chunkBlockX, int chunkBlockZ) {
        double halfLength = canyon.length * .5;
        double yawX = Math.cos(canyon.yaw);
        double yawZ = Math.sin(canyon.yaw);
        double normalX = -yawZ;
        double normalZ = yawX;
        double xReach = Math.abs(yawX) * halfLength + Math.abs(normalX) * canyon.curveAmplitude + canyon.width + 3.0;
        double zReach = Math.abs(yawZ) * halfLength + Math.abs(normalZ) * canyon.curveAmplitude + canyon.width + 3.0;

        return canyon.originX + xReach >= chunkBlockX && canyon.originX - xReach <= chunkBlockX + 15 &&
            canyon.originZ + zReach >= chunkBlockZ && canyon.originZ - zReach <= chunkBlockZ + 15;
    }

    private int getCanyonTopY(Canyon canyon, int surfaceAltitude) {
        if (canyon.surface) {
            return overrideSurfaceDetection || debugVisualizerEnabled
                ? topY
                : clamp(Math.min(surfaceAltitude + 2, topY), bottomY, 254);
        }

        int top = clamp(canyon.centerY + canyon.height / 2, bottomY, topY);
        if (!overrideSurfaceDetection && !debugVisualizerEnabled) {
            top = Math.min(top, surfaceAltitude - 8);
        }
        return clamp(top, 1, 254);
    }

    private int getCanyonBottomY(Canyon canyon, int top) {
        if (canyon.surface) {
            return clamp(Math.max(canyon.bottomY, top - canyon.height), bottomY, topY);
        }
        return clamp(canyon.centerY - canyon.height / 2, bottomY, topY);
    }

    private boolean shouldCarveCanyonBlock(Canyon canyon, double radial, int y, int top, int bottom, int x, int z, double progress) {
        int span = Math.max(1, top - bottom);
        double depth = (double)(top - y) / (double)span;
        double widthAtY;
        if (canyon.surface) {
            double middleBulge = Math.sin(depth * Math.PI);
            double bottomTaper = depth > .72 ? 1.0 - (depth - .72) / .28 * .55 : 1.0;
            widthAtY = (.42 + middleBulge * .72) * Math.max(.38, Math.min(1.0, bottomTaper));
        }
        else {
            widthAtY = Math.sin(depth * Math.PI);
        }

        double wallRoughness = (normalize(noise3(canyonWallNoise, x - progress * 17.0, y, z + progress * 17.0, .052)) - .5) * .18;
        if (widthAtY <= .08 || radial > widthAtY + wallRoughness) {
            return false;
        }
        return !isCanyonLedge(canyon, radial, y, x, z, progress);
    }

    private boolean isCanyonLedge(Canyon canyon, double radial, int y, int x, int z, double progress) {
        if (radial < .58 || radial > .96) {
            return false;
        }

        int band = positiveMod(y + canyon.ledgeOffset, 13);
        if (band > 1) {
            return false;
        }

        double ledgeNoise = normalize(noise3(canyonWallNoise, x + progress * 23.0, y, z - progress * 23.0, .075));
        return ledgeNoise > .58;
    }

    private void carveCanyonBlock(ModernCarvingContext context, int x, int y, int z, Canyon canyon,
                                  BlockPos.MutableBlockPos mutableBlockPos) {
        mutableBlockPos.setPos(x, y, z);
        if (debugVisualizerEnabled) {
            CarverUtils.debugDigBlock(context.primer, mutableBlockPos, CANYON_DEBUG_BLOCK, true);
            return;
        }

        int lavaLevel = canyon.hasLavaLake ? canyon.bottomY + 2 : liquidAltitude;
        int localX = context.localX(x);
        int localZ = context.localZ(z);
        IBlockState currentState = context.primer.getBlockState(localX, y, localZ);
        IBlockState proposedState = y <= lavaLevel ? canyonLavaBlock : Blocks.AIR.getDefaultState();
        if (currentState.getMaterial() == Material.WATER || currentState.getMaterial() == Material.LAVA) {
            if (context.hasApiCallbacks) {
                BlockPos blockPos = new BlockPos(x, y, z);
                proposedState = BetterCavesAPI.resolveModernCaveCarvedBlock(world, blockPos, currentState,
                    proposedState, ModernCaveCarvingType.CANYON);
            }
            context.primer.setBlockState(localX, y, localZ, proposedState);
            return;
        }

        if (!context.hasApiCallbacks) {
            CarverUtils.digBlock(world, context.primer, mutableBlockPos, Blocks.AIR.getDefaultState(), canyonLavaBlock,
                lavaLevel, replaceFloatingGravel);
            return;
        }

        BlockPos blockPos = new BlockPos(x, y, z);
        IBlockState carveState = BetterCavesAPI.resolveModernCaveCarvedBlock(world, blockPos, currentState,
            proposedState, ModernCaveCarvingType.CANYON);
        CarverUtils.digBlock(world, context.primer, blockPos, carveState, null, -1, replaceFloatingGravel);
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

    private static long canyonCacheKey(int cellX, int cellZ) {
        return ((long)cellX << 32) ^ (cellZ & 0xffffffffL);
    }

    private static int positiveMod(int value, int divisor) {
        int result = value % divisor;
        return result < 0 ? result + divisor : result;
    }

    private static double normalize(double value) {
        return clamp01((float)((value + 1.0) * .5));
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
