package com.yungnickyoung.minecraft.bettercaves.world.ravine;

import com.yungnickyoung.minecraft.bettercaves.config.io.ConfigLoader;
import com.yungnickyoung.minecraft.bettercaves.config.util.ConfigHolder;
import com.yungnickyoung.minecraft.bettercaves.util.BetterCavesUtils;
import com.yungnickyoung.minecraft.bettercaves.world.WaterRegionController;
import com.yungnickyoung.minecraft.bettercaves.world.carver.CarverUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.MapGenRavine;
import net.minecraftforge.event.terraingen.InitMapGenEvent;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * Replaces vanilla ravine pathing with a 1.18-style canyon carver adapted to 1.12 ChunkPrimer generation.
 */
public class MapGenBetterRavine extends MapGenRavine {
    private static final IBlockState LAVA = Blocks.LAVA.getDefaultState();
    private static final IBlockState WATER = Blocks.WATER.getDefaultState();
    private static final int CANYON_RANGE = 4;
    private static final int CANYON_MAX_DISTANCE = (CANYON_RANGE * 2 - 1) * 16;
    private static final float CANYON_START_PROBABILITY = 0.01F;
    private static final int CANYON_MIN_Y = 10;
    private static final int CANYON_MAX_Y = 67;
    private static final double CANYON_Y_SCALE = 3.0D;
    private static final int CANYON_WIDTH_SMOOTHNESS = 3;
    private static final float CANYON_VERTICAL_RADIUS_DEFAULT_FACTOR = 1.0F;
    private static final float CANYON_VERTICAL_RADIUS_CENTER_FACTOR = 0.0F;
    private static final int MIN_CARVE_Y = 1;
    private static final int MAX_CARVE_Y = 248;
    private ConfigHolder config;
    private WaterRegionController waterRegionController;
    private MapGenBase defaultRavineGen;
    private static final int OCEAN_MASK_BORDER = 2;

    IBlockState[] currChunkLiquidBlocks;
    boolean[] currChunkOceanMask;
    float[] currChunkFloodFactors;
    Biome[] currChunkBiomes;
    int currChunkOceanMaskWidth;
    int currChunkX, currChunkZ;

    public MapGenBetterRavine(InitMapGenEvent event) {
        this.defaultRavineGen = event.getOriginalGen();
    }

    @Override
    public synchronized void generate(World worldIn, int x, int z, @Nonnull ChunkPrimer primer) {
        // Only operate on whitelisted dimensions.
        if (!BetterCavesUtils.isDimensionWhitelisted(worldIn.provider.getDimension())) {
            defaultRavineGen.generate(worldIn, x, z, primer);
            return;
        }

        if (config == null || world != worldIn) { // Lazy initialization
            this.initialize(worldIn);
        }

        if (config.enableVanillaRavines.get()) {
            generateModernCanyons(worldIn, x, z, primer);
        }
    }

    @Override
    protected void digBlock(ChunkPrimer primer, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
        int localX = BetterCavesUtils.getLocal(x);
        int localZ = BetterCavesUtils.getLocal(z);

        prepareChunkContext(chunkX, chunkZ);

        digBlockLocalWithContext(primer, localX, y, localZ);
    }

    private void digBlockLocalWithContext(ChunkPrimer primer, int localX, int y, int localZ) {
        int columnIndex = localX * 16 + localZ;
        IBlockState liquidBlockState = currChunkLiquidBlocks[columnIndex];
        if (liquidBlockState == null && y <= config.liquidAltitude.get()) {
            liquidBlockState = LAVA;
        }

        // Don't dig boundaries between flooded and unflooded openings.
        boolean flooded = config.enableFloodedRavines.get()
            && currChunkOceanMask != null
            && currChunkOceanMask[(localX + OCEAN_MASK_BORDER) * currChunkOceanMaskWidth + localZ + OCEAN_MASK_BORDER]
            && y < world.getSeaLevel();
        if (flooded) {
            float smoothAmpFactor = currChunkFloodFactors[columnIndex];
            if (smoothAmpFactor <= .25f) { // Wall between flooded and normal caves.
                return;
            }
        }

        IBlockState airBlockState = flooded ? WATER : AIR;
        CarverUtils.digBlockLocal(primer, localX, y, localZ, currChunkBiomes[columnIndex], airBlockState, liquidBlockState, config.liquidAltitude.get(), config.replaceFloatingGravel.get());
    }

    // Disable built-in water block checks.
    // Without this, ravines in water regions will be sliced up.
    @Override
    protected boolean isOceanBlock(ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ) {
        return false;
    }

    private void initialize(World worldIn) {
        this.world = worldIn;
        int dimensionID = worldIn.provider.getDimension();
        this.config = ConfigLoader.loadConfigFromFileForDimension(dimensionID);
        this.waterRegionController = new WaterRegionController(world, config);
        this.currChunkLiquidBlocks = null;
        this.currChunkOceanMask = null;
        this.currChunkFloodFactors = null;
        this.currChunkBiomes = null;
    }

    private void generateModernCanyons(World worldIn, int chunkX, int chunkZ, ChunkPrimer primer) {
        prepareChunkContext(chunkX, chunkZ);

        Random seedRandom = new Random(worldIn.getSeed());
        long xSeed = seedRandom.nextLong();
        long zSeed = seedRandom.nextLong();
        boolean[] carvingMask = new boolean[16 * 256 * 16];

        for (int sourceChunkX = chunkX - CANYON_RANGE; sourceChunkX <= chunkX + CANYON_RANGE; sourceChunkX++) {
            for (int sourceChunkZ = chunkZ - CANYON_RANGE; sourceChunkZ <= chunkZ + CANYON_RANGE; sourceChunkZ++) {
                long chunkSeed = (long) sourceChunkX * xSeed ^ (long) sourceChunkZ * zSeed ^ worldIn.getSeed();
                Random random = new Random(chunkSeed);

                if (random.nextFloat() <= CANYON_START_PROBABILITY) {
                    carveCanyonFromSourceChunk(primer, chunkX, chunkZ, sourceChunkX, sourceChunkZ, random, carvingMask);
                }
            }
        }
    }

    private void carveCanyonFromSourceChunk(ChunkPrimer primer, int targetChunkX, int targetChunkZ, int sourceChunkX, int sourceChunkZ, Random random, boolean[] carvingMask) {
        double x = sourceChunkX * 16 + random.nextInt(16);
        double y = CANYON_MIN_Y + random.nextInt(CANYON_MAX_Y - CANYON_MIN_Y + 1);
        double z = sourceChunkZ * 16 + random.nextInt(16);
        float horizontalRotation = random.nextFloat() * ((float) Math.PI * 2.0F);
        float verticalRotation = randomBetween(random, -0.125F, 0.125F);
        float thickness = sampleTrapezoid(random, 0.0F, 6.0F, 2.0F);
        int distance = (int) ((float) CANYON_MAX_DISTANCE * randomBetween(random, 0.75F, 1.0F));

        doModernCanyonCarve(primer, targetChunkX, targetChunkZ, random.nextLong(), x, y, z, thickness, horizontalRotation, verticalRotation, 0, distance, carvingMask);
    }

    private void doModernCanyonCarve(ChunkPrimer primer, int targetChunkX, int targetChunkZ, long tunnelSeed, double x, double y, double z,
                                     float thickness, float horizontalRotation, float verticalRotation, int step, int distance, boolean[] carvingMask) {
        Random random = new Random(tunnelSeed);
        float[] widthFactorPerHeight = initWidthFactors(random);
        float yRota = 0.0F;
        float xRota = 0.0F;

        for (int currentStep = step; currentStep < distance; currentStep++) {
            double horizontalRadius = 1.5D + Math.sin((float) currentStep * Math.PI / (float) distance) * thickness;
            double verticalRadius = horizontalRadius * CANYON_Y_SCALE;

            horizontalRadius *= randomBetween(random, 0.75F, 1.0F);
            verticalRadius = updateVerticalRadius(random, verticalRadius, distance, currentStep);

            float verticalCos = (float) Math.cos(verticalRotation);
            float verticalSin = (float) Math.sin(verticalRotation);
            x += Math.cos(horizontalRotation) * verticalCos;
            y += verticalSin;
            z += Math.sin(horizontalRotation) * verticalCos;

            verticalRotation *= 0.7F;
            verticalRotation += xRota * 0.05F;
            horizontalRotation += yRota * 0.05F;
            xRota *= 0.8F;
            yRota *= 0.5F;
            xRota += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            yRota += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

            if (random.nextInt(4) != 0) {
                if (!canReach(targetChunkX, targetChunkZ, x, z, currentStep, distance, thickness)) {
                    return;
                }

                carveEllipsoid(primer, targetChunkX, targetChunkZ, x, y, z, horizontalRadius, verticalRadius, widthFactorPerHeight, carvingMask);
            }
        }
    }

    private float[] initWidthFactors(Random random) {
        float[] widthFactorPerHeight = new float[256];
        float widthFactor = 1.0F;

        for (int y = 0; y < 256; y++) {
            if (y == 0 || random.nextInt(CANYON_WIDTH_SMOOTHNESS) == 0) {
                widthFactor = 1.0F + random.nextFloat() * random.nextFloat();
            }

            widthFactorPerHeight[y] = widthFactor * widthFactor;
        }

        return widthFactorPerHeight;
    }

    private double updateVerticalRadius(Random random, double verticalRadius, int distance, int currentStep) {
        float verticalMultiplier = 1.0F - Math.abs(0.5F - (float) currentStep / (float) distance) * 2.0F;
        float factor = CANYON_VERTICAL_RADIUS_DEFAULT_FACTOR + CANYON_VERTICAL_RADIUS_CENTER_FACTOR * verticalMultiplier;

        return factor * verticalRadius * randomBetween(random, 0.75F, 1.0F);
    }

    private void carveEllipsoid(ChunkPrimer primer, int chunkX, int chunkZ, double x, double y, double z,
                                double horizontalRadius, double verticalRadius, float[] widthFactorPerHeight, boolean[] carvingMask) {
        int chunkMinX = chunkX * 16;
        int chunkMinZ = chunkZ * 16;
        double centerX = chunkMinX + 8.0D;
        double centerZ = chunkMinZ + 8.0D;
        double maxDelta = 16.0D + horizontalRadius * 2.0D;

        if (Math.abs(x - centerX) > maxDelta || Math.abs(z - centerZ) > maxDelta) {
            return;
        }

        int minXIndex = Math.max(floor(x - horizontalRadius) - chunkMinX - 1, 0);
        int maxXIndex = Math.min(floor(x + horizontalRadius) - chunkMinX, 15);
        int minY = Math.max(floor(y - verticalRadius) - 1, MIN_CARVE_Y);
        int maxY = Math.min(floor(y + verticalRadius) + 1, MAX_CARVE_Y);
        int minZIndex = Math.max(floor(z - horizontalRadius) - chunkMinZ - 1, 0);
        int maxZIndex = Math.min(floor(z + horizontalRadius) - chunkMinZ, 15);

        for (int localX = minXIndex; localX <= maxXIndex; localX++) {
            int worldX = chunkMinX + localX;
            double xd = ((double) worldX + 0.5D - x) / horizontalRadius;

            for (int localZ = minZIndex; localZ <= maxZIndex; localZ++) {
                int worldZ = chunkMinZ + localZ;
                double zd = ((double) worldZ + 0.5D - z) / horizontalRadius;

                if (xd * xd + zd * zd >= 1.0D) {
                    continue;
                }

                for (int worldY = maxY; worldY > minY; worldY--) {
                    double yd = ((double) worldY - 0.5D - y) / verticalRadius;

                    if (!shouldSkip(widthFactorPerHeight, xd, yd, zd, worldY)) {
                        int maskIndex = (localX * 16 + localZ) * 256 + worldY;
                        if (carvingMask[maskIndex]) {
                            continue;
                        }

                        carvingMask[maskIndex] = true;
                        digBlockLocalWithContext(primer, localX, worldY, localZ);
                    }
                }
            }
        }
    }

    private boolean shouldSkip(float[] widthFactorPerHeight, double xd, double yd, double zd, int y) {
        int yIndex = Math.max(1, Math.min(y, widthFactorPerHeight.length - 1));

        return (xd * xd + zd * zd) * widthFactorPerHeight[yIndex - 1] + yd * yd / 6.0D >= 1.0D;
    }

    private boolean canReach(int chunkX, int chunkZ, double x, double z, int currentStep, int totalSteps, float thickness) {
        double xMid = chunkX * 16 + 8.0D;
        double zMid = chunkZ * 16 + 8.0D;
        double xd = x - xMid;
        double zd = z - zMid;
        double remaining = totalSteps - currentStep;
        double radius = thickness + 2.0F + 16.0F;

        return xd * xd + zd * zd - remaining * remaining <= radius * radius;
    }

    private void prepareChunkContext(int chunkX, int chunkZ) {
        if (currChunkLiquidBlocks == null || chunkX != currChunkX || chunkZ != currChunkZ) {
            currChunkLiquidBlocks = waterRegionController.getLiquidBlocksFlatForChunk(chunkX, chunkZ);
            currChunkOceanMaskWidth = 16 + OCEAN_MASK_BORDER * 2;
            if (config.enableFloodedRavines.get()) {
                currChunkOceanMask = BetterCavesUtils.getOceanMaskFlat(world, chunkX, chunkZ, OCEAN_MASK_BORDER);
                currChunkFloodFactors = buildFloodFactors(currChunkOceanMask, currChunkOceanMaskWidth);
            }
            else {
                currChunkOceanMask = null;
                currChunkFloodFactors = null;
            }
            currChunkBiomes = buildBiomes(chunkX, chunkZ);
            currChunkX = chunkX;
            currChunkZ = chunkZ;
        }
    }

    private float sampleTrapezoid(Random random, float min, float max, float plateau) {
        float range = max - min;
        float plateauStart = (range - plateau) / 2.0F;
        float plateauEnd = range - plateauStart;

        return min + random.nextFloat() * plateauEnd + random.nextFloat() * plateauStart;
    }

    private float randomBetween(Random random, float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private int floor(double value) {
        int integer = (int) value;

        return value < (double) integer ? integer - 1 : integer;
    }

    private float[] buildFloodFactors(boolean[] oceanMask, int oceanMaskWidth) {
        float[] factors = new float[16 * 16];
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                factors[localX * 16 + localZ] = BetterCavesUtils.biomeDistanceFactor(localX, localZ, OCEAN_MASK_BORDER, oceanMask, oceanMaskWidth, false);
            }
        }
        return factors;
    }

    private Biome[] buildBiomes(int chunkX, int chunkZ) {
        Biome[] biomes = new Biome[16 * 16];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int baseX = chunkX * 16;
        int baseZ = chunkZ * 16;
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                pos.setPos(baseX + localX, 1, baseZ + localZ);
                biomes[localX * 16 + localZ] = world.getBiome(pos);
            }
        }
        return biomes;
    }
}
