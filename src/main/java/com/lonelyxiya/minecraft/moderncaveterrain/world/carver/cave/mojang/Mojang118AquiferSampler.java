package com.lonelyxiya.minecraft.moderncaveterrain.world.carver.cave.mojang;

import com.lonelyxiya.minecraft.moderncaveterrain.noise.MojangNormalNoise;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

import java.util.HashMap;
import java.util.Map;

/**
 * Java 8 port of the modern noise-based aquifer idea.
 *
 * <p>The implementation keeps 1.12.2 chunk carving constraints, but follows the modern flow: nearby jittered
 * aquifer cells choose fluid status, pressure between neighboring statuses can turn a would-be cave cell back
 * into stone, and only the remaining open cell receives air/water/lava.</p>
 */
public class Mojang118AquiferSampler {
    private static final int MODERN_SEA_LEVEL = 63;
    private static final int MODERN_LAVA_LEVEL = -54;
    private static final int NO_FLUID_LEVEL = Integer.MIN_VALUE / 4;
    private static final int CELL_WIDTH = 16;
    private static final int CELL_HEIGHT = 40;
    private static final double PRESSURE_SIMILARITY_RANGE = 25.0D;

    private final long seed;
    private final MojangNormalNoise barrierNoise;
    private final MojangNormalNoise floodednessNoise;
    private final MojangNormalNoise fluidLevelSpreadNoise;
    private final MojangNormalNoise lavaNoise;
    private final Map<Long, Long> locationCache = new HashMap<>();
    private final Map<Long, FluidStatus> statusCache = new HashMap<>();

    public Mojang118AquiferSampler(long seed) {
        this.seed = seed;
        this.barrierNoise = MojangNormalNoise.create(seed, "aquifer_barrier", -3, 1.0D);
        this.floodednessNoise = MojangNormalNoise.create(seed, "aquifer_fluid_level_floodedness", -7, 1.0D);
        this.fluidLevelSpreadNoise = MojangNormalNoise.create(seed, "aquifer_fluid_level_spread", -5, 1.0D);
        this.lavaNoise = MojangNormalNoise.create(seed, "aquifer_lava", -1, 1.0D);
    }

    public IBlockState computeSubstance(int blockX, int blockY, int blockZ, double density,
                                        Mojang118NoiseChunk noiseChunk, int seaLevel, boolean flooded) {
        if (density > 0.0D) {
            return null;
        }

        double mojangY = Mojang118NoiseChunk.toMojangY(blockY);
        FluidStatus globalFluid = computeGlobalFluid(mojangY, seaLevel, flooded);
        IBlockState globalState = globalFluid.at(mojangY);
        if (globalState.getBlock() == Blocks.LAVA) {
            return globalState;
        }

        int gridX = gridX(blockX - 5);
        int gridY = gridY(mojangY + 1.0D);
        int gridZ = gridZ(blockZ - 5);
        ClosestCells closest = findClosestCells(blockX, mojangY, blockZ, gridX, gridY, gridZ);
        FluidStatus status1 = getAquiferStatus(closest.index1, noiseChunk, seaLevel, flooded);
        IBlockState substance = status1.at(mojangY);
        double similarity12 = similarity(closest.distance1, closest.distance2);

        if (similarity12 <= 0.0D) {
            return substance;
        }

        if (substance.getBlock() == Blocks.WATER
                && computeGlobalFluid(Mojang118NoiseChunk.toMojangY(blockY - 1), seaLevel, flooded)
                .at(Mojang118NoiseChunk.toMojangY(blockY - 1)).getBlock() == Blocks.LAVA) {
            return substance;
        }

        FluidStatus status2 = getAquiferStatus(closest.index2, noiseChunk, seaLevel, flooded);
        double barrier = barrierPressure(blockX, mojangY, blockZ, status1, status2);
        if (density + similarity12 * barrier > 0.0D) {
            return null;
        }

        FluidStatus status3 = getAquiferStatus(closest.index3, noiseChunk, seaLevel, flooded);
        double similarity13 = similarity(closest.distance1, closest.distance3);
        if (similarity13 > 0.0D) {
            double pressure13 = similarity12 * similarity13 * barrierPressure(blockX, mojangY, blockZ, status1, status3);
            if (density + pressure13 > 0.0D) {
                return null;
            }
        }

        double similarity23 = similarity(closest.distance2, closest.distance3);
        if (similarity23 > 0.0D) {
            double pressure23 = similarity12 * similarity23 * barrierPressure(blockX, mojangY, blockZ, status2, status3);
            if (density + pressure23 > 0.0D) {
                return null;
            }
        }

        return substance;
    }

    public IBlockState sampleFluidState(int blockX, int blockY, int blockZ, int surfaceY, int seaLevel,
                                        boolean flooded) {
        Mojang118NoiseChunk noiseChunk = new Mojang118NoiseChunk(null, blockX, blockZ, surfaceY);
        double mojangY = Mojang118NoiseChunk.toMojangY(blockY);
        FluidStatus status = computeFluid(blockX, mojangY, blockZ, noiseChunk, seaLevel, flooded);
        IBlockState state = status.at(mojangY);
        return state.getBlock() == Blocks.AIR ? null : state;
    }

    public double toMojangYForApi(int blockY) {
        return Mojang118NoiseChunk.toMojangY(blockY);
    }

    private ClosestCells findClosestCells(int blockX, double mojangY, int blockZ, int gridX, int gridY, int gridZ) {
        ClosestCells closest = new ClosestCells();

        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    int cellX = gridX + dx;
                    int cellY = gridY + dy;
                    int cellZ = gridZ + dz;
                    long cellKey = cellKey(cellX, cellY, cellZ);
                    long location = getAquiferLocation(cellX, cellY, cellZ, cellKey);
                    int locationX = unpackX(location);
                    int locationY = unpackY(location);
                    int locationZ = unpackZ(location);
                    int xDiff = locationX - blockX;
                    int yDiff = locationY - (int) Math.floor(mojangY);
                    int zDiff = locationZ - blockZ;
                    int distance = xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;

                    closest.accept(cellKey, distance);
                }
            }
        }

        return closest;
    }

    private long getAquiferLocation(int cellX, int cellY, int cellZ, long cellKey) {
        Long cached = locationCache.get(cellKey);
        if (cached != null) {
            return cached;
        }

        long random = mix(seed ^ 0x9E3779B97F4A7C15L, cellX, cellY, cellZ);
        int offsetX = positiveInt(random, 10);
        random = mix(random, cellZ, cellX, cellY);
        int offsetY = positiveInt(random, 9);
        random = mix(random, cellY, cellZ, cellX);
        int offsetZ = positiveInt(random, 10);
        long location = pack(fromGridX(cellX, offsetX), fromGridY(cellY, offsetY), fromGridZ(cellZ, offsetZ));
        locationCache.put(cellKey, location);
        return location;
    }

    private FluidStatus getAquiferStatus(long cellKey, Mojang118NoiseChunk noiseChunk, int seaLevel, boolean flooded) {
        Long packedLocation = locationCache.get(cellKey);
        if (packedLocation == null) {
            return new FluidStatus(NO_FLUID_LEVEL, Blocks.AIR.getDefaultState());
        }

        long statusKey = cellKey ^ noiseChunk.getCacheKey() ^ (flooded ? 0xC2B2AE3D27D4EB4FL : 0L);
        FluidStatus cached = statusCache.get(statusKey);
        if (cached != null) {
            return cached;
        }

        FluidStatus status = computeFluid(unpackX(packedLocation), unpackY(packedLocation), unpackZ(packedLocation),
                noiseChunk, seaLevel, flooded);
        statusCache.put(statusKey, status);
        return status;
    }

    private FluidStatus computeFluid(int x, double mojangY, int z, Mojang118NoiseChunk noiseChunk, int seaLevel,
                                     boolean flooded) {
        FluidStatus globalFluid = computeGlobalFluid(mojangY, seaLevel, flooded);
        int lowestPreliminarySurface = Integer.MAX_VALUE;
        int aquiferY = (int) Math.floor(mojangY);
        int topOfAquiferCell = aquiferY + 12;
        int bottomOfAquiferCell = aquiferY - 12;
        boolean surfaceAtCenterIsUnderGlobalFluidLevel = false;

        for (int[] offset : Mojang118NoiseChunk.SURFACE_SAMPLING_OFFSETS_IN_CHUNKS) {
            int sampleX = x + offset[0] * 16;
            int sampleZ = z + offset[1] * 16;
            int preliminarySurfaceLevel = noiseChunk.preliminarySurfaceLevel(sampleX, sampleZ);
            int adjustedSurfaceLevel = noiseChunk.adjustedSurfaceLevel(preliminarySurfaceLevel);
            boolean center = offset[0] == 0 && offset[1] == 0;

            if (center && bottomOfAquiferCell > adjustedSurfaceLevel) {
                return globalFluid;
            }

            boolean topPokesAboveSurface = topOfAquiferCell > adjustedSurfaceLevel;
            if (topPokesAboveSurface || center) {
                FluidStatus globalFluidAtSurface = computeGlobalFluid(adjustedSurfaceLevel, seaLevel, flooded);
                if (globalFluidAtSurface.at(adjustedSurfaceLevel).getBlock() != Blocks.AIR) {
                    if (center) {
                        surfaceAtCenterIsUnderGlobalFluidLevel = true;
                    }
                    if (topPokesAboveSurface) {
                        return globalFluidAtSurface;
                    }
                }
            }

            lowestPreliminarySurface = Math.min(lowestPreliminarySurface, preliminarySurfaceLevel);
        }

        int fluidLevel = computeSurfaceLevel(x, mojangY, z, globalFluid, lowestPreliminarySurface,
                surfaceAtCenterIsUnderGlobalFluidLevel);
        return new FluidStatus(fluidLevel, computeFluidType(x, mojangY, z, globalFluid, fluidLevel));
    }

    private FluidStatus computeGlobalFluid(double mojangY, int seaLevel, boolean flooded) {
        int seaMojangY = (int) Math.floor(Mojang118NoiseChunk.toMojangY(seaLevel));
        if (mojangY <= MODERN_LAVA_LEVEL) {
            return new FluidStatus(MODERN_LAVA_LEVEL + 1, Blocks.LAVA.getDefaultState());
        }

        if (flooded) {
            return new FluidStatus(seaMojangY, Blocks.WATER.getDefaultState());
        }

        return new FluidStatus(NO_FLUID_LEVEL, Blocks.AIR.getDefaultState());
    }

    private int computeSurfaceLevel(int blockX, double mojangY, int blockZ, FluidStatus globalFluid,
                                    int lowestPreliminarySurface, boolean surfaceUnderWater) {
        double fluidDepth = lowestPreliminarySurface + 8.0D - mojangY;
        double floodednessFactor = surfaceUnderWater ? clampedMap(fluidDepth, 0.0D, 64.0D, 1.0D, 0.0D) : 0.0D;
        double floodedness = clamp(sample(floodednessNoise, blockX, mojangY, blockZ, 1.0D, 0.67D), -1.0D, 1.0D);
        double fullyFloodedThreshold = lerp(1.0D - floodednessFactor, -0.3D, 0.8D);
        double partiallyFloodedThreshold = lerp(1.0D - floodednessFactor, -0.8D, 0.4D);
        double partiallyFloodedness = floodedness - partiallyFloodedThreshold;
        double fullyFloodedness = floodedness - fullyFloodedThreshold;

        if (fullyFloodedness > 0.0D) {
            return globalFluid.fluidLevel;
        }
        if (partiallyFloodedness > 0.0D) {
            return computeRandomizedFluidSurfaceLevel(blockX, mojangY, blockZ, lowestPreliminarySurface);
        }

        return NO_FLUID_LEVEL;
    }

    private int computeRandomizedFluidSurfaceLevel(int blockX, double mojangY, int blockZ, int lowestPreliminarySurface) {
        int cellX = floorDiv(blockX, 16);
        int cellY = floorDiv((int) Math.floor(mojangY), 40);
        int cellZ = floorDiv(blockZ, 16);
        int cellMiddleY = cellY * 40 + 20;
        double spread = fluidLevelSpreadNoise.getValue(cellX, cellY, cellZ) * 10.0D;
        int quantizedSpread = quantize(spread, 3);
        int targetFluidSurfaceLevel = cellMiddleY + quantizedSpread;

        return Math.min(lowestPreliminarySurface, targetFluidSurfaceLevel);
    }

    private IBlockState computeFluidType(int blockX, double mojangY, int blockZ, FluidStatus globalFluid, int fluidSurfaceLevel) {
        IBlockState fluidType = globalFluid.fluidType.getBlock() == Blocks.AIR
                ? Blocks.WATER.getDefaultState()
                : globalFluid.fluidType;

        if (fluidSurfaceLevel <= -10 && fluidSurfaceLevel != NO_FLUID_LEVEL && fluidType.getBlock() != Blocks.LAVA) {
            int cellX = floorDiv(blockX, 64);
            int cellY = floorDiv((int) Math.floor(mojangY), 40);
            int cellZ = floorDiv(blockZ, 64);
            double lavaNoiseValue = lavaNoise.getValue(cellX, cellY, cellZ);
            if (Math.abs(lavaNoiseValue) > 0.3D) {
                fluidType = Blocks.LAVA.getDefaultState();
            }
        }

        return fluidType;
    }

    private double barrierPressure(int blockX, double mojangY, int blockZ, FluidStatus status1, FluidStatus status2) {
        IBlockState type1 = status1.at(mojangY);
        IBlockState type2 = status2.at(mojangY);
        if (isWaterLavaPair(type1, type2)) {
            return 2.0D;
        }

        int fluidLevelDiff = Math.abs(status1.fluidLevel - status2.fluidLevel);
        if (fluidLevelDiff == 0) {
            return 0.0D;
        }

        double averageFluidLevel = 0.5D * (status1.fluidLevel + status2.fluidLevel);
        double distanceFromAverage = mojangY + 0.5D - averageFluidLevel;
        double base = fluidLevelDiff / 2.0D;
        double distanceFromEdge = base - Math.abs(distanceFromAverage);
        double gradient;
        if (distanceFromAverage > 0.0D) {
            gradient = distanceFromEdge > 0.0D ? distanceFromEdge / 1.5D : distanceFromEdge / 2.5D;
        } else {
            double bottomBiased = 3.0D + distanceFromEdge;
            gradient = bottomBiased > 0.0D ? bottomBiased / 3.0D : bottomBiased / 10.0D;
        }

        double noise = gradient >= -2.0D && gradient <= 2.0D
                ? barrierNoise.getValue(blockX * 0.5D, mojangY * 0.5D, blockZ * 0.5D)
                : 0.0D;

        return 2.0D * (noise + gradient);
    }

    private boolean isWaterLavaPair(IBlockState first, IBlockState second) {
        return first.getBlock() == Blocks.WATER && second.getBlock() == Blocks.LAVA
                || first.getBlock() == Blocks.LAVA && second.getBlock() == Blocks.WATER;
    }

    private double sample(MojangNormalNoise noise, int blockX, double mojangY, int blockZ, double xzScale, double yScale) {
        return noise.getValue(blockX * xzScale, mojangY * yScale, blockZ * xzScale);
    }

    private static double similarity(int distanceSqr1, int distanceSqr2) {
        return 1.0D - (double) (distanceSqr2 - distanceSqr1) / PRESSURE_SIMILARITY_RANGE;
    }

    private static int gridX(int blockCoord) {
        return floorDiv(blockCoord, CELL_WIDTH);
    }

    private static int gridY(double y) {
        return floorDiv((int) Math.floor(y), CELL_HEIGHT);
    }

    private static int gridZ(int blockCoord) {
        return floorDiv(blockCoord, CELL_WIDTH);
    }

    private static int fromGridX(int gridCoord, int blockOffset) {
        return gridCoord * CELL_WIDTH + blockOffset;
    }

    private static int fromGridY(int gridCoord, int blockOffset) {
        return gridCoord * CELL_HEIGHT + blockOffset;
    }

    private static int fromGridZ(int gridCoord, int blockOffset) {
        return gridCoord * CELL_WIDTH + blockOffset;
    }

    private static long cellKey(int cellX, int cellY, int cellZ) {
        return (((long) cellX & 0x1FFFFFL) << 43) ^ (((long) cellY & 0x1FFFFFL) << 22) ^ ((long) cellZ & 0x3FFFFFL);
    }

    private static long pack(int x, int y, int z) {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) y & 0xFFFL) << 26) | ((long) z & 0x3FFFFFFL);
    }

    private static int unpackX(long packed) {
        return signExtend((int) (packed >> 38), 26);
    }

    private static int unpackY(long packed) {
        return signExtend((int) (packed >> 26), 12);
    }

    private static int unpackZ(long packed) {
        return signExtend((int) packed, 26);
    }

    private static int signExtend(int value, int bits) {
        int shift = 32 - bits;
        return value << shift >> shift;
    }

    private static long mix(long seed, int x, int y, int z) {
        long value = seed;
        value ^= (long) x * 341873128712L;
        value ^= (long) y * 132897987541L;
        value ^= (long) z * 42317861L;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private static int positiveInt(long value, int bound) {
        return (int) Math.floorMod(value, (long) bound);
    }

    private static int floorDiv(int x, int y) {
        int result = x / y;
        if ((x ^ y) < 0 && result * y != x) {
            result--;
        }
        return result;
    }

    private static int quantize(double value, int resolution) {
        return (int) Math.floor(value / (double) resolution) * resolution;
    }

    private static double clampedMap(double value, double oldMin, double oldMax, double newMin, double newMax) {
        return lerp(clamp((value - oldMin) / (oldMax - oldMin), 0.0D, 1.0D), newMin, newMax);
    }

    private static double lerp(double factor, double from, double to) {
        return from + factor * (to - from);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class ClosestCells {
        private int distance1 = Integer.MAX_VALUE;
        private int distance2 = Integer.MAX_VALUE;
        private int distance3 = Integer.MAX_VALUE;
        private long index1;
        private long index2;
        private long index3;

        private void accept(long index, int distance) {
            if (distance1 >= distance) {
                index3 = index2;
                distance3 = distance2;
                index2 = index1;
                distance2 = distance1;
                index1 = index;
                distance1 = distance;
            } else if (distance2 >= distance) {
                index3 = index2;
                distance3 = distance2;
                index2 = index;
                distance2 = distance;
            } else if (distance3 >= distance) {
                index3 = index;
                distance3 = distance;
            }
        }
    }

    private static class FluidStatus {
        private final int fluidLevel;
        private final IBlockState fluidType;

        private FluidStatus(int fluidLevel, IBlockState fluidType) {
            this.fluidLevel = fluidLevel;
            this.fluidType = fluidType;
        }

        private IBlockState at(double mojangY) {
            return mojangY < fluidLevel ? fluidType : Blocks.AIR.getDefaultState();
        }
    }
}
