package com.lonelyxiya.minecraft.moderncaveterrain.world.carver.cave.mojang;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Mojang118PreliminarySurfaceSamplerTest {
    @Test
    void emptyDensityColumnFallsBackToMinimumSurface() {
        double[] density = new double[33];
        Arrays.fill(density, -1.0D);

        assertEquals(1, Mojang118PreliminarySurfaceSampler.surfaceLevelFromDensityColumn(density));
    }

    @Test
    void densityColumnUsesVanillaEightBlockVerticalInterpolation() {
        double[] density = new double[33];
        Arrays.fill(density, -1.0D);
        density[8] = 1.0D;

        assertEquals(68, Mojang118PreliminarySurfaceSampler.surfaceLevelFromDensityColumn(density));
    }

    @Test
    void densityColumnClampsToTheLegacyWorldCeiling() {
        double[] density = new double[33];
        Arrays.fill(density, 1.0D);

        assertEquals(255, Mojang118PreliminarySurfaceSampler.surfaceLevelFromDensityColumn(density));
    }

    @Test
    void quartQuantizationUsesFloorCoordinatesAcrossTheOrigin() {
        int[][] samples = {
                {-17, -20}, {-16, -16}, {-15, -16}, {-5, -8}, {-4, -4}, {-1, -4},
                {0, 0}, {3, 0}, {4, 4}, {15, 12}, {16, 16}
        };

        for (int[] sample : samples) {
            assertEquals(sample[1], Mojang118NoiseChunk.quantizeToQuartBlock(sample[0]),
                    "block coordinate " + sample[0]);
        }
    }
}
