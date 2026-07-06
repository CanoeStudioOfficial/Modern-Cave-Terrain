package com.lonelyxiya.minecraft.moderncaveterrain.world;

import com.lonelyxiya.minecraft.moderncaveterrain.ModernCaveTerrain;
import com.lonelyxiya.minecraft.moderncaveterrain.config.util.ConfigHolder;
import com.lonelyxiya.minecraft.moderncaveterrain.config.ModernCaveTerrainSettings;
import com.lonelyxiya.minecraft.moderncaveterrain.enums.CaveType;
import com.lonelyxiya.minecraft.moderncaveterrain.enums.RegionSize;
import com.lonelyxiya.minecraft.moderncaveterrain.noise.FastNoise;
import com.lonelyxiya.minecraft.moderncaveterrain.noise.ColumnNoiseBuffer;
import com.lonelyxiya.minecraft.moderncaveterrain.world.carver.CarverNoiseRange;
import com.lonelyxiya.minecraft.moderncaveterrain.world.carver.ICarver;
import com.lonelyxiya.minecraft.moderncaveterrain.world.carver.cave.CaveCarver;
import com.lonelyxiya.minecraft.moderncaveterrain.world.carver.cave.CaveCarverBuilder;
import com.lonelyxiya.minecraft.moderncaveterrain.world.carver.cave.mojang.Mojang118CaveCarver;
import com.lonelyxiya.minecraft.moderncaveterrain.world.carver.cave.mojang.Mojang118NoiseChunk;
import com.lonelyxiya.minecraft.moderncaveterrain.world.carver.vanilla.VanillaCaveCarver;
import com.lonelyxiya.minecraft.moderncaveterrain.world.carver.vanilla.VanillaCaveCarverBuilder;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.ArrayList;
import java.util.List;

public class CaveCarverController {
    private static final int[] FULL_CARVING_MASK_ROWS = buildFullCarvingMaskRows();

    private World world;
    private VanillaCaveCarver surfaceCaveCarver; // only used if surface caves enabled
    private FastNoise caveRegionController;
    private List<CarverNoiseRange> noiseRanges = new ArrayList<>();

    // Vars from config
    private boolean isDebugViewEnabled;
    private boolean isOverrideSurfaceDetectionEnabled;
    private boolean isSurfaceCavesEnabled;
    private boolean isFloodedUndergroundEnabled;
    private boolean hasNonVanillaCaves;

    public CaveCarverController(World worldIn, ConfigHolder config) {
        this.world = worldIn;
        this.isDebugViewEnabled = config.debugVisualizer.get();
        this.isOverrideSurfaceDetectionEnabled = config.overrideSurfaceDetection.get();
        this.isSurfaceCavesEnabled = config.isSurfaceCavesEnabled.get();
        this.isFloodedUndergroundEnabled = config.enableFloodedUnderground.get();
        this.surfaceCaveCarver = new VanillaCaveCarverBuilder()
            .bottomY(config.surfaceCaveBottom.get())
            .topY(config.surfaceCaveTop.get())
            .density(config.surfaceCaveDensity.get())
            .liquidAltitude(config.liquidAltitude.get())
            .replaceGravel(config.replaceFloatingGravel.get())
            .floodedUnderground(config.enableFloodedUnderground.get())
            .debugVisualizerEnabled(config.debugVisualizer.get())
            .debugVisualizerBlock(Blocks.EMERALD_BLOCK.getDefaultState())
            .build();

        // Configure cave region controller, which determines what type of cave should be
        // carved in any given region
        float caveRegionSize = calcCaveRegionSize(config.caveRegionSize.get(), config.caveRegionCustomSize.get());
        this.caveRegionController = new FastNoise();
        this.caveRegionController.SetSeed((int)worldIn.getSeed() + 222);
        this.caveRegionController.SetFrequency(caveRegionSize);
        this.caveRegionController.SetNoiseType(FastNoise.NoiseType.Cellular);
        this.caveRegionController.SetCellularDistanceFunction(FastNoise.CellularDistanceFunction.Natural);

        // Initialize all carvers using config options
        List<ICarver> carvers = new ArrayList<>();
        // Type 1 caves
        carvers.add(new CaveCarverBuilder(worldIn)
            .ofTypeFromConfig(CaveType.CUBIC, config)
            .debugVisualizerBlock(Blocks.PLANKS.getDefaultState())
            .build()
        );
        // Type 2 caves
        carvers.add(new CaveCarverBuilder(worldIn)
            .ofTypeFromConfig(CaveType.SIMPLEX, config)
            .debugVisualizerBlock(Blocks.COBBLESTONE.getDefaultState())
            .build()
        );
        // 1.18-style density caves
        carvers.add(new Mojang118CaveCarver(worldIn, config));
        // Vanilla caves
        carvers.add(new VanillaCaveCarverBuilder()
            .bottomY(config.vanillaCaveBottom.get())
            .topY(config.vanillaCaveTop.get())
            .density(config.vanillaCaveDensity.get())
            .priority(config.vanillaCavePriority.get())
            .liquidAltitude(config.liquidAltitude.get())
            .replaceGravel(config.replaceFloatingGravel.get())
            .floodedUnderground(config.enableFloodedUnderground.get())
            .debugVisualizerEnabled(config.debugVisualizer.get())
            .debugVisualizerBlock(Blocks.BRICK_BLOCK.getDefaultState())
            .build());

        // Remove carvers with no priority
        carvers.removeIf(carver -> carver.getPriority() == 0);
        this.hasNonVanillaCaves = carvers.stream().anyMatch(this::isColumnCarver);
        if (carvers.isEmpty()) {
            return;
        }

        // Initialize vars for calculating controller noise thresholds
        float maxPossibleNoiseThreshold = config.caveSpawnChance.get() * .01f * 2 - 1;
        int totalPriority = carvers.stream().map(ICarver::getPriority).reduce(0, Integer::sum);
        float totalRangeLength = maxPossibleNoiseThreshold - -1f;
        float currNoise = -1f;

        ModernCaveTerrain.LOGGER.debug("CAVE INFORMATION");
        ModernCaveTerrain.LOGGER.debug("--> MAX POSSIBLE THRESHOLD: " + maxPossibleNoiseThreshold);
        ModernCaveTerrain.LOGGER.debug("--> TOTAL PRIORITY: " + totalPriority);
        ModernCaveTerrain.LOGGER.debug("--> TOTAL RANGE LENGTH: " + totalRangeLength);

        for (ICarver carver : carvers) {
            ModernCaveTerrain.LOGGER.debug("--> CARVER");
            float noiseRangeLength = (float)carver.getPriority() / totalPriority * totalRangeLength;
            float rangeTop = currNoise + noiseRangeLength;
            CarverNoiseRange range = new CarverNoiseRange(currNoise, rangeTop, carver);
            currNoise = rangeTop;
            noiseRanges.add(range);

            ModernCaveTerrain.LOGGER.debug("    --> RANGE FOUND: " + range);
        }
    }

    public void carveChunk(ChunkCaveContext chunkContext) {
        // Prevent unnecessary computation if caves are disabled
        if (noiseRanges.isEmpty() && !isSurfaceCavesEnabled) {
            return;
        }

        ChunkPrimer primer = chunkContext.getPrimer();
        int chunkX = chunkContext.getChunkX();
        int chunkZ = chunkContext.getChunkZ();
        boolean flooded;

        // Flag to keep track of whether or not we've already carved vanilla caves for this chunk, since
        // vanilla caves operate on a chunk-by-chunk basis rather than by column
        boolean shouldCarveVanillaCaves = false;

        // Since vanilla caves carve by chunk and not by column, we store an array
        // indicating which x-z coordinates are valid to be carved in
        int[] vanillaCarvingMaskRows = new int[16];

        if (noiseRanges.isEmpty()) {
            surfaceCaveCarver.generate(world, chunkX, chunkZ, primer, false, chunkContext.getLiquidBlocksFlat(), FULL_CARVING_MASK_ROWS, chunkContext.getOceanMaskFlat(), chunkContext.getOceanMaskWidth(), chunkContext.getBiomes());
            return;
        }

        int subChunkCount = 16 / ModernCaveTerrainSettings.SUB_CHUNK_SIZE;
        SubChunkColumnFields fields = new SubChunkColumnFields();
        fields.resetRanges(noiseRanges.size());
        ColumnNoiseBuffer[] noiseBuffers = new ColumnNoiseBuffer[noiseRanges.size()];
        Mojang118NoiseChunk mojang118NoiseChunk = null;

        // Break into subchunks for noise interpolation
        for (int subX = 0; subX < subChunkCount; subX++) {
            for (int subZ = 0; subZ < subChunkCount; subZ++) {
                int startX = subX * ModernCaveTerrainSettings.SUB_CHUNK_SIZE;
                int startZ = subZ * ModernCaveTerrainSettings.SUB_CHUNK_SIZE;
                int endX = startX + ModernCaveTerrainSettings.SUB_CHUNK_SIZE - 1;
                int endZ = startZ + ModernCaveTerrainSettings.SUB_CHUNK_SIZE - 1;

                fields.clear();
                fields.resetRanges(noiseRanges.size());

                for (int offsetX = 0; offsetX < ModernCaveTerrainSettings.SUB_CHUNK_SIZE; offsetX++) {
                    for (int offsetZ = 0; offsetZ < ModernCaveTerrainSettings.SUB_CHUNK_SIZE; offsetZ++) {
                        int columnIndex = offsetX * ModernCaveTerrainSettings.SUB_CHUNK_SIZE + offsetZ;
                        int localX = startX + offsetX;
                        int localZ = startZ + offsetZ;
                        int blockX = chunkContext.getBlockX(localX);
                        int blockZ = chunkContext.getBlockZ(localZ);
                        fields.clearColumn(columnIndex);
                        flooded = isFloodedUndergroundEnabled && !isDebugViewEnabled && chunkContext.isOceanColumn(localX, localZ);
                        if (flooded && !chunkContext.isSurroundedByOcean(localX, localZ)) {
                            continue;
                        }

                        // Get noise values used to determine cave region
                        float caveRegionNoise = caveRegionController.GetNoise(blockX, blockZ);

                        // Carve cave using matching carver
                        for (int rangeIndex = 0; rangeIndex < noiseRanges.size(); rangeIndex++) {
                            CarverNoiseRange range = noiseRanges.get(rangeIndex);
                            if (!range.contains(caveRegionNoise)) {
                                continue;
                            }
                            if (isColumnCarver(range.getCarver())) {
                                ICarver carver = range.getCarver();
                                int surfaceAltitude = chunkContext.getSurfaceAltitude(localX, localZ);
                                int topY = Math.min(surfaceAltitude, carver.getTopY());
                                if (isOverrideSurfaceDetectionEnabled) {
                                    topY = carver.getTopY();
                                }
                                if (isDebugViewEnabled) {
                                    topY = 128;
                                }
                                if (topY < getBottomY(carver)) {
                                    continue;
                                }
                                fields.activeRanges[columnIndex] = range;
                                fields.topYs[columnIndex] = topY;
                                fields.flooded[columnIndex] = flooded;
                                fields.addActiveColumn(columnIndex, rangeIndex);
                                break;
                            }
                            else if (range.getCarver() instanceof VanillaCaveCarver) {
                                vanillaCarvingMaskRows[localX] |= 1 << localZ;
                                shouldCarveVanillaCaves = true;
                                break;
                            }
                        }
                    }
                }

                for (int rangeIndex = 0; rangeIndex < noiseRanges.size(); rangeIndex++) {
                    CarverNoiseRange range = noiseRanges.get(rangeIndex);
                    if (!(range.getCarver() instanceof CaveCarver)) {
                        continue;
                    }

                    CaveCarver carver = (CaveCarver) range.getCarver();
                    int neededMaxHeight = Integer.MIN_VALUE;
                    int rangeCount = fields.getRangeCount(rangeIndex);
                    int[] rangeColumns = fields.getRangeColumns(rangeIndex);
                    for (int i = 0; i < rangeCount; i++) {
                        int columnIndex = rangeColumns[i];
                        neededMaxHeight = Math.max(neededMaxHeight, fields.topYs[columnIndex]);
                    }
                    if (neededMaxHeight == Integer.MIN_VALUE) {
                        continue;
                    }

                    ColumnNoiseBuffer noiseBuffer = noiseBuffers[rangeIndex];
                    if (noiseBuffer == null) {
                        noiseBuffer = new ColumnNoiseBuffer();
                        noiseBuffers[rangeIndex] = noiseBuffer;
                    }
                    carver.getNoiseGen().interpolateNoiseColumns(
                        noiseBuffer,
                        chunkContext.getBlockX(startX),
                        chunkContext.getBlockX(endX),
                        chunkContext.getBlockZ(startZ),
                        chunkContext.getBlockZ(endZ),
                        carver.getBottomY(),
                        neededMaxHeight,
                        rangeColumns,
                        rangeCount
                    );
                }

                if (fields.activeCount == 0) {
                    continue;
                }

                for (int i = 0; i < fields.activeCount; i++) {
                    int columnIndex = fields.activeColumns[i];
                    int offsetX = columnIndex / ModernCaveTerrainSettings.SUB_CHUNK_SIZE;
                    int offsetZ = columnIndex % ModernCaveTerrainSettings.SUB_CHUNK_SIZE;
                    CarverNoiseRange activeRange = fields.activeRanges[columnIndex];
                    int localX = startX + offsetX;
                    int localZ = startZ + offsetZ;
                    ICarver carver = activeRange.getCarver();
                    if (carver instanceof CaveCarver) {
                        CaveCarver caveCarver = (CaveCarver) carver;
                        caveCarver.carveColumn(primer, localX, localZ, fields.topYs[columnIndex], noiseBuffers[fields.rangeIndices[columnIndex]], fields.noiseSlots[columnIndex], chunkContext.getLiquidBlock(localX, localZ), fields.flooded[columnIndex], chunkContext.getBiome(localX, localZ));
                    }
                    else if (carver instanceof Mojang118CaveCarver) {
                        Mojang118CaveCarver mojang118CaveCarver = (Mojang118CaveCarver) carver;
                        if (mojang118NoiseChunk == null) {
                            mojang118NoiseChunk = new Mojang118NoiseChunk(world, chunkContext);
                        }
                        mojang118CaveCarver.carveColumn(primer, mojang118NoiseChunk, localX, localZ,
                                chunkContext.getBlockX(localX), chunkContext.getBlockZ(localZ),
                                fields.topYs[columnIndex], chunkContext.getLiquidBlock(localX, localZ),
                                fields.flooded[columnIndex], chunkContext.getBiome(localX, localZ));
                    }
                }
            }
        }
        if (shouldCarveVanillaCaves) {
            VanillaCaveCarver carver = null;
            for (CarverNoiseRange range : noiseRanges) {
                if (range.getCarver() instanceof VanillaCaveCarver) {
                    carver = (VanillaCaveCarver) range.getCarver();
                    break;
                }
            }
            if (carver != null) {
                carver.generate(world, chunkX, chunkZ, primer, true, chunkContext.getLiquidBlocksFlat(), vanillaCarvingMaskRows, chunkContext.getOceanMaskFlat(), chunkContext.getOceanMaskWidth(), chunkContext.getBiomes());
            }
        }
        // Generate surface caves if enabled
        if (isSurfaceCavesEnabled) {
            surfaceCaveCarver.generate(world, chunkX, chunkZ, primer, false, chunkContext.getLiquidBlocksFlat(), FULL_CARVING_MASK_ROWS, chunkContext.getOceanMaskFlat(), chunkContext.getOceanMaskWidth(), chunkContext.getBiomes());
        }
    }

    private static int[] buildFullCarvingMaskRows() {
        int[] mask = new int[16];
        for (int x = 0; x < 16; x++) {
            mask[x] = 0xFFFF;
        }
        return mask;
    }

    private boolean isColumnCarver(ICarver carver) {
        return carver instanceof CaveCarver || carver instanceof Mojang118CaveCarver;
    }

    private int getBottomY(ICarver carver) {
        if (carver instanceof CaveCarver) {
            return ((CaveCarver) carver).getBottomY();
        }
        if (carver instanceof Mojang118CaveCarver) {
            return ((Mojang118CaveCarver) carver).getBottomY();
        }

        return 0;
    }

    /**
     * @return frequency value for cave region controller
     */
    private float calcCaveRegionSize(RegionSize caveRegionSize, float caveRegionCustomSize) {
        switch (caveRegionSize) {
            case Small:
                return .008f;
            case Large:
                return .0032f;
            case ExtraLarge:
                return .001f;
            case Custom:
                return caveRegionCustomSize;
            default: // Medium
                return .005f;
        }
    }

    public boolean hasWork() {
        return !noiseRanges.isEmpty() || isSurfaceCavesEnabled;
    }

    public boolean needsSurfaceAltitudes() {
        return hasNonVanillaCaves;
    }

    public boolean needsOceanMask() {
        return isFloodedUndergroundEnabled && hasWork();
    }

    public boolean needsBiomeCache() {
        return hasWork();
    }

    public int getMaxSurfaceSearchY() {
        int maxTopY = 0;
        for (CarverNoiseRange range : noiseRanges) {
            if (isColumnCarver(range.getCarver())) {
                maxTopY = Math.max(maxTopY, range.getCarver().getTopY());
            }
        }
        return isDebugViewEnabled ? Math.max(maxTopY, 128) : maxTopY;
    }
}
