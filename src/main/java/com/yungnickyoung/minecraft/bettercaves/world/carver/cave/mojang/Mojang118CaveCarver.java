package com.yungnickyoung.minecraft.bettercaves.world.carver.cave.mojang;

import com.yungnickyoung.minecraft.bettercaves.BetterCaves;
import com.yungnickyoung.minecraft.bettercaves.config.util.ConfigHolder;
import com.yungnickyoung.minecraft.bettercaves.world.carver.CarverUtils;
import com.yungnickyoung.minecraft.bettercaves.world.carver.ICarver;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Arrays;

/**
 * Carves a 1.18-style cave density field into a 1.12.2 ChunkPrimer.
 */
public class Mojang118CaveCarver implements ICarver {
    private static final int CHUNK_SIZE = 16;
    private static final int WORLD_HEIGHT = 256;
    private static final int FLUID_FILL_VOLUME = CHUNK_SIZE * WORLD_HEIGHT * CHUNK_SIZE;

    private final Mojang118CaveDensitySampler densitySampler;
    private final Mojang118AquiferSampler aquiferSampler;
    private final int bottomY;
    private final int topY;
    private final int surfaceCutoff;
    private final int priority;
    private final int seaLevel;
    private final double densityThreshold;
    private final boolean replaceFloatingGravel;
    private final boolean debugVisualizer;
    private final IBlockState debugBlock;

    public Mojang118CaveCarver(World world, ConfigHolder config) {
        this.bottomY = config.mojang118CaveBottom.get();
        this.topY = config.mojang118CaveTop.get();
        this.surfaceCutoff = config.mojang118CaveSurfaceCutoffDepth.get();
        this.priority = config.mojang118CavePriority.get();
        this.seaLevel = world.getSeaLevel();
        this.densityThreshold = config.mojang118CaveDensityThreshold.get();
        this.replaceFloatingGravel = config.replaceFloatingGravel.get();
        this.debugVisualizer = config.debugVisualizer.get();
        this.debugBlock = Blocks.REDSTONE_BLOCK.getDefaultState();
        this.densitySampler = new Mojang118CaveDensitySampler(
                world.getSeed(),
                config.mojang118CaveHorizontalScale.get(),
                config.mojang118CaveVerticalScale.get()
        );
        this.aquiferSampler = new Mojang118AquiferSampler(world.getSeed());

        if (bottomY > topY) {
            BetterCaves.LOGGER.warn("Warning: Min altitude for 1.18-style caves should not be greater than max altitude.");
        }
    }

    public void carveColumn(ChunkPrimer primer, int localX, int localZ, int blockX, int blockZ, int topY,
                            IBlockState liquidBlock, boolean flooded, Biome biome) {
        if (bottomY < 0 || bottomY > 255 || topY < 0 || topY > 255 || topY < bottomY) {
            return;
        }

        IBlockState airState = Blocks.AIR.getDefaultState();
        IBlockState waterState = Blocks.WATER.getDefaultState();
        IBlockState airBlockState;
        IBlockState fluidState;
        int transitionBoundary = Math.max(bottomY, topY - surfaceCutoff);
        int transitionHeight = Math.max(1, topY - transitionBoundary);

        for (int y = topY; y >= bottomY; y--) {
            double density = densitySampler.sampleDensity(blockX, y, blockZ);
            if (y >= transitionBoundary) {
                double surfaceFactor = (double) (y - transitionBoundary) / transitionHeight;
                density += surfaceFactor * 0.45D;
            }

            boolean digBlock = density <= densityThreshold;
            if (debugVisualizer) {
                CarverUtils.debugDigBlockLocal(primer, localX, y, localZ, debugBlock, digBlock);
                continue;
            }

            if (!digBlock) {
                continue;
            }

            fluidState = flooded && y < seaLevel
                    ? waterState
                    : aquiferSampler.sampleFluidState(blockX, y, blockZ, topY, seaLevel, flooded);
            if (fluidState != null && isOppositeFluidAdjacent(primer, localX, y, localZ, fluidState)) {
                continue;
            }
            airBlockState = fluidState == null ? airState : fluidState;
            CarverUtils.digBlockLocal(primer, localX, y, localZ, biome, airBlockState, null, -1, replaceFloatingGravel);
        }
    }

    public void completeAquiferFluidBodies(ChunkPrimer primer) {
        if (debugVisualizer) {
            return;
        }

        boolean[] visited = new boolean[FLUID_FILL_VOLUME];
        int[] queue = new int[FLUID_FILL_VOLUME];
        int[] component = new int[FLUID_FILL_VOLUME];

        completeFluidBodies(primer, Blocks.WATER.getDefaultState(), Material.WATER, visited, queue, component);
        Arrays.fill(visited, false);
        completeFluidBodies(primer, Blocks.LAVA.getDefaultState(), Material.LAVA, visited, queue, component);
    }

    private void completeFluidBodies(ChunkPrimer primer, IBlockState fluidState, Material fluidMaterial,
                                     boolean[] visited, int[] queue, int[] component) {
        int minY = Math.max(0, bottomY);
        int maxY = Math.min(WORLD_HEIGHT - 1, topY);

        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {
                for (int y = maxY; y >= minY; y--) {
                    int startIndex = index(localX, y, localZ);
                    if (visited[startIndex] || primer.getBlockState(localX, y, localZ).getMaterial() != fluidMaterial) {
                        continue;
                    }

                    int componentCount = collectFluidComponent(primer, fluidMaterial, visited, queue, component, localX, y, localZ, minY, maxY);
                    if (componentCount > 0) {
                        fillConnectedAirBelowSurface(primer, fluidState, fluidMaterial, visited, queue, component, componentCount, minY);
                    }
                }
            }
        }
    }

    private int collectFluidComponent(ChunkPrimer primer, Material fluidMaterial, boolean[] visited, int[] queue,
                                      int[] component, int startX, int startY, int startZ, int minY, int maxY) {
        int head = 0;
        int tail = 0;
        int componentCount = 0;

        queue[tail++] = index(startX, startY, startZ);
        visited[index(startX, startY, startZ)] = true;

        while (head < tail) {
            int current = queue[head++];
            component[componentCount++] = current;
            int localX = unpackX(current);
            int y = unpackY(current);
            int localZ = unpackZ(current);

            tail = enqueueFluidNeighbor(primer, fluidMaterial, visited, queue, tail, localX + 1, y, localZ, minY, maxY);
            tail = enqueueFluidNeighbor(primer, fluidMaterial, visited, queue, tail, localX - 1, y, localZ, minY, maxY);
            tail = enqueueFluidNeighbor(primer, fluidMaterial, visited, queue, tail, localX, y + 1, localZ, minY, maxY);
            tail = enqueueFluidNeighbor(primer, fluidMaterial, visited, queue, tail, localX, y - 1, localZ, minY, maxY);
            tail = enqueueFluidNeighbor(primer, fluidMaterial, visited, queue, tail, localX, y, localZ + 1, minY, maxY);
            tail = enqueueFluidNeighbor(primer, fluidMaterial, visited, queue, tail, localX, y, localZ - 1, minY, maxY);
        }

        return componentCount;
    }

    private void fillConnectedAirBelowSurface(ChunkPrimer primer, IBlockState fluidState, Material fluidMaterial,
                                              boolean[] visited, int[] queue, int[] component, int componentCount,
                                              int minY) {
        int head = 0;
        int tail = 0;
        int surfaceY = minY;

        for (int i = 0; i < componentCount; i++) {
            int current = component[i];
            surfaceY = Math.max(surfaceY, unpackY(current));
            queue[tail++] = current;
        }

        while (head < tail) {
            int current = queue[head++];
            int localX = unpackX(current);
            int y = unpackY(current);
            int localZ = unpackZ(current);

            tail = fillAirNeighbor(primer, fluidState, fluidMaterial, visited, queue, tail, localX + 1, y, localZ, minY, surfaceY);
            tail = fillAirNeighbor(primer, fluidState, fluidMaterial, visited, queue, tail, localX - 1, y, localZ, minY, surfaceY);
            tail = fillAirNeighbor(primer, fluidState, fluidMaterial, visited, queue, tail, localX, y + 1, localZ, minY, surfaceY);
            tail = fillAirNeighbor(primer, fluidState, fluidMaterial, visited, queue, tail, localX, y - 1, localZ, minY, surfaceY);
            tail = fillAirNeighbor(primer, fluidState, fluidMaterial, visited, queue, tail, localX, y, localZ + 1, minY, surfaceY);
            tail = fillAirNeighbor(primer, fluidState, fluidMaterial, visited, queue, tail, localX, y, localZ - 1, minY, surfaceY);
        }
    }

    private int enqueueFluidNeighbor(ChunkPrimer primer, Material fluidMaterial, boolean[] visited, int[] queue,
                                     int tail, int localX, int y, int localZ, int minY, int maxY) {
        if (!isInside(localX, y, localZ, minY, maxY)) {
            return tail;
        }

        int neighborIndex = index(localX, y, localZ);
        if (visited[neighborIndex] || primer.getBlockState(localX, y, localZ).getMaterial() != fluidMaterial) {
            return tail;
        }

        visited[neighborIndex] = true;
        queue[tail++] = neighborIndex;
        return tail;
    }

    private int fillAirNeighbor(ChunkPrimer primer, IBlockState fluidState, Material fluidMaterial, boolean[] visited,
                                int[] queue, int tail, int localX, int y, int localZ, int minY, int surfaceY) {
        if (!isInside(localX, y, localZ, minY, surfaceY)) {
            return tail;
        }

        int neighborIndex = index(localX, y, localZ);
        if (visited[neighborIndex]) {
            return tail;
        }

        Material material = primer.getBlockState(localX, y, localZ).getMaterial();
        if (material == fluidMaterial) {
            visited[neighborIndex] = true;
            queue[tail++] = neighborIndex;
        }
        else if (material == Material.AIR && !isOppositeFluidAdjacent(primer, localX, y, localZ, fluidState)) {
            primer.setBlockState(localX, y, localZ, fluidState);
            visited[neighborIndex] = true;
            queue[tail++] = neighborIndex;
        }

        return tail;
    }

    private boolean isInside(int localX, int y, int localZ, int minY, int maxY) {
        return localX >= 0 && localX < CHUNK_SIZE && localZ >= 0 && localZ < CHUNK_SIZE && y >= minY && y <= maxY;
    }

    private int index(int localX, int y, int localZ) {
        return (localX * WORLD_HEIGHT + y) * CHUNK_SIZE + localZ;
    }

    private int unpackX(int index) {
        return index / (WORLD_HEIGHT * CHUNK_SIZE);
    }

    private int unpackY(int index) {
        return index / CHUNK_SIZE % WORLD_HEIGHT;
    }

    private int unpackZ(int index) {
        return index % CHUNK_SIZE;
    }

    private boolean isOppositeFluidAdjacent(ChunkPrimer primer, int localX, int y, int localZ, IBlockState fluidState) {
        return isOppositeFluid(primer, localX, y + 1, localZ, fluidState)
                || isOppositeFluid(primer, localX, y - 1, localZ, fluidState)
                || isOppositeFluid(primer, localX + 1, y, localZ, fluidState)
                || isOppositeFluid(primer, localX - 1, y, localZ, fluidState)
                || isOppositeFluid(primer, localX, y, localZ + 1, fluidState)
                || isOppositeFluid(primer, localX, y, localZ - 1, fluidState);
    }

    private boolean isOppositeFluid(ChunkPrimer primer, int localX, int y, int localZ, IBlockState fluidState) {
        if (localX < 0 || localX > 15 || y < 0 || y > 255 || localZ < 0 || localZ > 15) {
            return false;
        }

        Material material = primer.getBlockState(localX, y, localZ).getMaterial();
        Material targetMaterial = fluidState.getMaterial();

        return targetMaterial == Material.WATER && material == Material.LAVA
                || targetMaterial == Material.LAVA && material == Material.WATER;
    }

    public int getPriority() {
        return priority;
    }

    public int getBottomY() {
        return bottomY;
    }

    public int getTopY() {
        return topY;
    }
}
