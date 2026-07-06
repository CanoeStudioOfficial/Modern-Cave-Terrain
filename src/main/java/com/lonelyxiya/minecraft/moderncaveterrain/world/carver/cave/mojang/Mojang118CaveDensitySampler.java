package com.lonelyxiya.minecraft.moderncaveterrain.world.carver.cave.mojang;

import com.lonelyxiya.minecraft.moderncaveterrain.noise.MojangNormalNoise;

/**
 * A Java 8-friendly approximation of the 1.18 overworld cave density graph.
 *
 * <p>The function layout follows Mojang's cheese, spaghetti, noodle and pillar density functions. The
 * underlying sampler uses a Java 8-friendly subset of Mojang's NormalNoise stack so it can live cleanly in
 * the 1.12.2 codebase.</p>
 */
public class Mojang118CaveDensitySampler {
    private static final double MOJANG_MIN_Y = -64.0D;
    private static final double MOJANG_HEIGHT = 384.0D;
    private static final double OLD_WORLD_MAX_Y = 255.0D;
    private static final int SURFACE_ENTRANCE_MAX_DEPTH = 34;

    private final MojangNormalNoise caveCheese;
    private final MojangNormalNoise caveLayer;
    private final MojangNormalNoise caveEntrance;
    private final MojangNormalNoise spaghetti2D;
    private final MojangNormalNoise spaghetti2DElevation;
    private final MojangNormalNoise spaghetti2DModulator;
    private final MojangNormalNoise spaghetti2DThickness;
    private final MojangNormalNoise spaghetti3D1;
    private final MojangNormalNoise spaghetti3D2;
    private final MojangNormalNoise spaghetti3DRarity;
    private final MojangNormalNoise spaghetti3DThickness;
    private final MojangNormalNoise spaghettiRoughness;
    private final MojangNormalNoise spaghettiRoughnessModulator;
    private final MojangNormalNoise noodle;
    private final MojangNormalNoise noodleThickness;
    private final MojangNormalNoise noodleRidgeA;
    private final MojangNormalNoise noodleRidgeB;
    private final MojangNormalNoise pillar;
    private final MojangNormalNoise pillarRareness;
    private final MojangNormalNoise pillarThickness;

    private final double horizontalScale;
    private final double verticalScale;

    public Mojang118CaveDensitySampler(long seed, float horizontalScale, float verticalScale) {
        this.horizontalScale = horizontalScale;
        this.verticalScale = verticalScale;

        this.caveCheese = noise(seed, "cave_cheese", -8, 0.5D, 1.0D, 2.0D, 1.0D, 2.0D, 1.0D, 0.0D, 2.0D, 0.0D);
        this.caveLayer = noise(seed, "cave_layer", -8, 1.0D);
        this.caveEntrance = noise(seed, "cave_entrance", -7, 0.4D, 0.5D, 1.0D);
        this.spaghetti2D = noise(seed, "spaghetti_2d", -7, 1.0D);
        this.spaghetti2DElevation = noise(seed, "spaghetti_2d_elevation", -8, 1.0D);
        this.spaghetti2DModulator = noise(seed, "spaghetti_2d_modulator", -11, 1.0D);
        this.spaghetti2DThickness = noise(seed, "spaghetti_2d_thickness", -11, 1.0D);
        this.spaghetti3D1 = noise(seed, "spaghetti_3d_1", -7, 1.0D);
        this.spaghetti3D2 = noise(seed, "spaghetti_3d_2", -7, 1.0D);
        this.spaghetti3DRarity = noise(seed, "spaghetti_3d_rarity", -11, 1.0D);
        this.spaghetti3DThickness = noise(seed, "spaghetti_3d_thickness", -8, 1.0D);
        this.spaghettiRoughness = noise(seed, "spaghetti_roughness", -5, 1.0D);
        this.spaghettiRoughnessModulator = noise(seed, "spaghetti_roughness_modulator", -8, 1.0D);
        this.noodle = noise(seed, "noodle", -8, 1.0D);
        this.noodleThickness = noise(seed, "noodle_thickness", -8, 1.0D);
        this.noodleRidgeA = noise(seed, "noodle_ridge_a", -7, 1.0D);
        this.noodleRidgeB = noise(seed, "noodle_ridge_b", -7, 1.0D);
        this.pillar = noise(seed, "pillar", -7, 1.0D, 1.0D);
        this.pillarRareness = noise(seed, "pillar_rareness", -8, 1.0D);
        this.pillarThickness = noise(seed, "pillar_thickness", -8, 1.0D);
    }

    public double sampleDensity(int blockX, int blockY, int blockZ) {
        double mojangY = toMojangY(blockY);
        double slopedCheese = slopedCheese(blockX, mojangY, blockZ);
        double surfaceWithEntrances = Math.min(slopedCheese, 5.0D * entrances(blockX, mojangY, blockZ));
        double caves = slopedCheese < 1.5625D
                ? surfaceWithEntrances
                : underground(blockX, mojangY, blockZ, slopedCheese);
        double fullNoise = Math.min(postProcess(slideOverworld(mojangY, caves)), noodle(blockX, mojangY, blockZ));

        return fullNoise;
    }

    public double applySurfaceAdjustment(double density, int blockX, int blockY, int blockZ, int surfaceY,
                                         int bottomY, int surfaceCutoff, boolean allowSurfaceEntrance,
                                         double densityThreshold) {
        int transitionBoundary = Math.max(bottomY, surfaceY - surfaceCutoff);
        int transitionHeight = Math.max(1, surfaceY - transitionBoundary);
        double entranceAperture = allowSurfaceEntrance
                ? surfaceEntranceAperture(density, blockX, blockY, blockZ, surfaceY, densityThreshold)
                : 0.0D;

        if (blockY >= transitionBoundary) {
            double surfaceFactor = (double) (blockY - transitionBoundary) / transitionHeight;
            density += surfaceFactor * 0.52D * (1.0D - entranceAperture);
        }

        if (entranceAperture <= 0.0D || blockY > surfaceY) {
            return density;
        }

        int depthBelowSurface = surfaceY - blockY;
        double mouthFade = clampedMap(depthBelowSurface, 0.0D, 10.0D, 1.0D, 0.0D);
        double throatFade = clampedMap(depthBelowSurface, 4.0D, 28.0D, 0.42D, 0.0D);

        return density - entranceAperture * (mouthFade * 0.20D + throatFade * 0.10D);
    }

    public boolean isSurfaceEntrance(int blockX, int blockY, int blockZ, int surfaceY, double densityThreshold) {
        if (blockY > surfaceY || surfaceY - blockY > SURFACE_ENTRANCE_MAX_DEPTH) {
            return false;
        }

        return surfaceEntranceStrength(blockX, blockY, blockZ, surfaceY, densityThreshold) > 0.64D;
    }

    public boolean shouldCarveSurfaceEntrance(double rawDensity, double adjustedDensity, int blockX, int blockY,
                                              int blockZ, int surfaceY, double densityThreshold) {
        if (blockY > surfaceY || surfaceY - blockY > SURFACE_ENTRANCE_MAX_DEPTH) {
            return false;
        }

        int depthBelowSurface = surfaceY - blockY;
        double entranceStrength = surfaceEntranceStrength(blockX, blockY, blockZ, surfaceY, densityThreshold);
        double graphAlreadyOpen = graphOpenStrength(rawDensity, densityThreshold);
        double depthFactor = clampedMap(depthBelowSurface, 0.0D, SURFACE_ENTRANCE_MAX_DEPTH, 1.0D, 0.18D);
        double openStrength = Math.max(entranceStrength, graphAlreadyOpen * depthFactor);
        double loosenedThreshold = densityThreshold + openStrength * clampedMap(depthBelowSurface, 0.0D,
                SURFACE_ENTRANCE_MAX_DEPTH, 0.22D, 0.05D);

        return openStrength > 0.58D && adjustedDensity <= loosenedThreshold;
    }

    public double surfaceEntranceStrength(int blockX, int blockY, int blockZ, int surfaceY,
                                          double densityThreshold) {
        if (blockY > surfaceY || surfaceY - blockY > SURFACE_ENTRANCE_MAX_DEPTH) {
            return 0.0D;
        }

        int depthBelowSurface = Math.max(0, surfaceY - blockY);
        double modernEntranceStrength = modernEntranceStrength(blockX, blockY, blockZ, surfaceY, densityThreshold);
        double verticalStrength = clampedMap(depthBelowSurface, 0.0D, SURFACE_ENTRANCE_MAX_DEPTH, 1.0D, 0.0D);

        return modernEntranceStrength * verticalStrength;
    }

    private double surfaceEntranceAperture(double density, int blockX, int blockY, int blockZ, int surfaceY,
                                           double densityThreshold) {
        int depthBelowSurface = Math.max(0, surfaceY - blockY);
        double modernEntranceStrength = surfaceEntranceStrength(blockX, blockY, blockZ, surfaceY, densityThreshold);
        double graphAlreadyOpen = graphOpenStrength(density, densityThreshold);
        double verticalStrength = clampedMap(depthBelowSurface, SURFACE_ENTRANCE_MAX_DEPTH, 0.0D, 0.0D, 1.0D);

        return Math.max(modernEntranceStrength, graphAlreadyOpen * verticalStrength);
    }

    private double modernEntranceStrength(int blockX, int blockY, int blockZ, int surfaceY,
                                          double densityThreshold) {
        double currentEntrance = entrances(blockX, toMojangY(blockY), blockZ);
        double surfaceEntrance = entrances(blockX, toMojangY(Math.max(0, surfaceY - 2)), blockZ);
        double entranceDensity = Math.min(currentEntrance, surfaceEntrance);

        return clampedMap(5.0D * entranceDensity, densityThreshold + 0.26D,
                densityThreshold - 0.08D, 0.0D, 1.0D);
    }

    private double graphOpenStrength(double density, double densityThreshold) {
        return clampedMap(density, densityThreshold + 0.20D, densityThreshold - 0.04D, 0.0D, 1.0D);
    }

    private double slopedCheese(int blockX, double mojangY, int blockZ) {
        double heightGradient = yClampedGradient(mojangY, -64, 320, 1.5D, -1.5D);
        double baseNoise = sample(caveCheese, blockX, mojangY, blockZ, 1.0D, 0.6666666666666666D);
        return heightGradient + baseNoise * 0.35D;
    }

    private double underground(int blockX, double mojangY, int blockZ, double slopedCheese) {
        double layerNoise = sample(caveLayer, blockX, mojangY, blockZ, 1.0D, 8.0D);
        double layerizedCaverns = 4.0D * layerNoise * layerNoise;
        double cheese = sample(caveCheese, blockX, mojangY, blockZ, 1.0D, 0.6666666666666666D);
        double solidifiedCheese = clamp(0.27D + cheese, -1.0D, 1.0D)
                + clamp(1.5D - 0.64D * slopedCheese, 0.0D, 0.5D);
        double baseCaveDensity = layerizedCaverns + solidifiedCheese;
        double subtractions = Math.min(Math.min(baseCaveDensity, entrances(blockX, mojangY, blockZ)),
                spaghetti2D(blockX, mojangY, blockZ) + spaghettiRoughness(blockX, mojangY, blockZ));
        double pillars = pillars(blockX, mojangY, blockZ);

        return Math.max(subtractions, pillars);
    }

    private double entrances(int blockX, double mojangY, int blockZ) {
        double rarity = sample(spaghetti3DRarity, blockX, mojangY, blockZ, 2.0D, 1.0D);
        double cave1 = quantizedSpaghetti3D(rarity, spaghetti3D1, blockX, mojangY, blockZ);
        double cave2 = quantizedSpaghetti3D(rarity, spaghetti3D2, blockX, mojangY, blockZ);
        double thickness = mappedNoise(spaghetti3DThickness, blockX, mojangY, blockZ, 1.0D, 1.0D, -0.065D, -0.088D);
        double spaghetti3D = clamp(Math.max(cave1, cave2) + thickness, -1.0D, 1.0D);
        double bigEntrances = sample(caveEntrance, blockX, mojangY, blockZ, 0.75D, 0.5D)
                + 0.37D + yClampedGradient(mojangY, -10, 30, 0.3D, 0.0D);

        return Math.min(bigEntrances, spaghettiRoughness(blockX, mojangY, blockZ) + spaghetti3D);
    }

    private double noodle(int blockX, double mojangY, int blockZ) {
        if (mojangY < -60.0D || mojangY > 320.0D) {
            return 64.0D;
        }

        double toggle = sample(noodle, blockX, mojangY, blockZ, 1.0D, 1.0D);
        if (toggle < 0.0D) {
            return 64.0D;
        }

        double thickness = mappedNoise(noodleThickness, blockX, mojangY, blockZ, 1.0D, 1.0D, -0.05D, -0.1D);
        double ridgeA = Math.abs(sample(noodleRidgeA, blockX, mojangY, blockZ, 2.6666666666666665D, 2.6666666666666665D));
        double ridgeB = Math.abs(sample(noodleRidgeB, blockX, mojangY, blockZ, 2.6666666666666665D, 2.6666666666666665D));

        return thickness + 1.5D * Math.max(ridgeA, ridgeB);
    }

    private double pillars(int blockX, double mojangY, int blockZ) {
        double pillarNoise = sample(pillar, blockX, mojangY, blockZ, 25.0D, 0.3D);
        double rareness = mappedNoise(pillarRareness, blockX, mojangY, blockZ, 1.0D, 1.0D, 0.0D, -2.0D);
        double thickness = mappedNoise(pillarThickness, blockX, mojangY, blockZ, 1.0D, 1.0D, 0.0D, 1.1D);
        double result = (2.0D * pillarNoise + rareness) * cube(thickness);

        return result < 0.03D ? -1000000.0D : result;
    }

    private double spaghetti2D(int blockX, double mojangY, int blockZ) {
        double rarity = sample(spaghetti2DModulator, blockX, mojangY, blockZ, 2.0D, 1.0D);
        double cave = quantizedSpaghetti2D(rarity, blockX, mojangY, blockZ);
        double elevation = mappedNoise(spaghetti2DElevation, blockX, mojangY, blockZ, 1.0D, 0.0D, -8.0D, 8.0D);
        double thickness = mappedNoise(spaghetti2DThickness, blockX, mojangY, blockZ, 2.0D, 1.0D, -0.6D, -1.3D);
        double slopedSpaghetti = Math.abs(elevation + yClampedGradient(mojangY, -64, 320, 8.0D, -40.0D));
        double layerRidged = cube(slopedSpaghetti + thickness);
        double caveNoise = cave + 0.083D * thickness;

        return clamp(Math.max(caveNoise, layerRidged), -1.0D, 1.0D);
    }

    private double spaghettiRoughness(int blockX, double mojangY, int blockZ) {
        double roughness = sample(spaghettiRoughness, blockX, mojangY, blockZ, 1.0D, 1.0D);
        double modulator = mappedNoise(spaghettiRoughnessModulator, blockX, mojangY, blockZ, 1.0D, 1.0D, 0.0D, -0.1D);

        return modulator * (Math.abs(roughness) - 0.4D);
    }

    private double quantizedSpaghetti2D(double rarity, int blockX, double mojangY, int blockZ) {
        if (rarity < -0.75D) {
            return scaledSpaghetti(spaghetti2D, blockX, mojangY, blockZ, 0.5D);
        }
        if (rarity < -0.5D) {
            return scaledSpaghetti(spaghetti2D, blockX, mojangY, blockZ, 0.75D);
        }
        if (rarity < 0.5D) {
            return scaledSpaghetti(spaghetti2D, blockX, mojangY, blockZ, 1.0D);
        }
        if (rarity < 0.75D) {
            return scaledSpaghetti(spaghetti2D, blockX, mojangY, blockZ, 2.0D);
        }

        return scaledSpaghetti(spaghetti2D, blockX, mojangY, blockZ, 3.0D);
    }

    private double quantizedSpaghetti3D(double rarity, MojangNormalNoise noise, int blockX, double mojangY, int blockZ) {
        if (rarity < -0.5D) {
            return scaledSpaghetti(noise, blockX, mojangY, blockZ, 0.75D);
        }
        if (rarity < 0.0D) {
            return scaledSpaghetti(noise, blockX, mojangY, blockZ, 1.0D);
        }
        if (rarity < 0.5D) {
            return scaledSpaghetti(noise, blockX, mojangY, blockZ, 1.5D);
        }

        return scaledSpaghetti(noise, blockX, mojangY, blockZ, 2.0D);
    }

    private double scaledSpaghetti(MojangNormalNoise noise, int blockX, double mojangY, int blockZ, double rarity) {
        return Math.abs(rarity * sample(noise, blockX, mojangY, blockZ, 1.0D / rarity, 1.0D / rarity));
    }

    private double slideOverworld(double mojangY, double caves) {
        return slide(mojangY, caves, -64, 384, 80, 64, -0.078125D, 0, 24, 0.1171875D);
    }

    private double slide(double mojangY, double caves, int minY, int height, int topStartY, int topEndY,
                         double topTarget, int bottomStartY, int bottomEndY, double bottomTarget) {
        double topFactor = yClampedGradient(mojangY, minY + height - topStartY, minY + height - topEndY, 1.0D, 0.0D);
        double value = lerp(topFactor, topTarget, caves);
        double bottomFactor = yClampedGradient(mojangY, minY + bottomStartY, minY + bottomEndY, 0.0D, 1.0D);

        return lerp(bottomFactor, bottomTarget, value);
    }

    private double postProcess(double density) {
        return squeeze(density * 0.64D);
    }

    private double mappedNoise(MojangNormalNoise noise, int blockX, double mojangY, int blockZ, double xzScale,
                               double yScale, double min, double max) {
        double value = sample(noise, blockX, mojangY, blockZ, xzScale, yScale);

        return (min + max) * 0.5D + (max - min) * 0.5D * value;
    }

    private double sample(MojangNormalNoise noise, int blockX, double mojangY, int blockZ, double xzScale, double yScale) {
        double x = blockX * xzScale * horizontalScale;
        double y = mojangY * yScale * verticalScale;
        double z = blockZ * xzScale * horizontalScale;

        return noise.getValue(x, y, z);
    }

    private static MojangNormalNoise noise(long seed, String name, int firstOctave, double... amplitudes) {
        return MojangNormalNoise.create(seed, name, firstOctave, amplitudes);
    }

    private static double toMojangY(int blockY) {
        return MOJANG_MIN_Y + blockY / OLD_WORLD_MAX_Y * MOJANG_HEIGHT;
    }

    private static double yClampedGradient(double y, double fromY, double toY, double fromValue, double toValue) {
        if (y <= fromY) {
            return fromValue;
        }
        if (y >= toY) {
            return toValue;
        }

        return lerp((y - fromY) / (toY - fromY), fromValue, toValue);
    }

    private static double lerp(double factor, double from, double to) {
        return from + factor * (to - from);
    }

    private static double cube(double value) {
        return value * value * value;
    }

    private static double squeeze(double value) {
        double clamped = clamp(value, -1.0D, 1.0D);

        return clamped / 2.0D - clamped * clamped * clamped / 24.0D;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampedMap(double value, double fromMin, double fromMax, double toMin, double toMax) {
        if (fromMin < fromMax) {
            if (value <= fromMin) {
                return toMin;
            }
            if (value >= fromMax) {
                return toMax;
            }
        }
        else {
            if (value >= fromMin) {
                return toMin;
            }
            if (value <= fromMax) {
                return toMax;
            }
        }

        return lerp((value - fromMin) / (fromMax - fromMin), toMin, toMax);
    }

}
