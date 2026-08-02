package com.lonelyxiya.minecraft.moderncaveterrain.world.carver.cave.mojang;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Mojang118CaveDensitySamplerTest {
    private static final double EPSILON = 1.0E-12D;

    @Test
    void lerp3ReturnsEachCornerAtTheCellBounds() {
        double[] corners = {2.0D, 3.0D, 5.0D, 7.0D, 11.0D, 13.0D, 17.0D, 19.0D};

        assertEquals(corners[0], lerp3(0.0D, 0.0D, 0.0D, corners), EPSILON);
        assertEquals(corners[1], lerp3(1.0D, 0.0D, 0.0D, corners), EPSILON);
        assertEquals(corners[2], lerp3(0.0D, 1.0D, 0.0D, corners), EPSILON);
        assertEquals(corners[3], lerp3(1.0D, 1.0D, 0.0D, corners), EPSILON);
        assertEquals(corners[4], lerp3(0.0D, 0.0D, 1.0D, corners), EPSILON);
        assertEquals(corners[5], lerp3(1.0D, 0.0D, 1.0D, corners), EPSILON);
        assertEquals(corners[6], lerp3(0.0D, 1.0D, 1.0D, corners), EPSILON);
        assertEquals(corners[7], lerp3(1.0D, 1.0D, 1.0D, corners), EPSILON);
    }

    @Test
    void lerp3CenterIsTheAverageOfAllCorners() {
        double[] corners = {2.0D, 3.0D, 5.0D, 7.0D, 11.0D, 13.0D, 17.0D, 19.0D};

        assertEquals(9.625D, lerp3(0.5D, 0.5D, 0.5D, corners), EPSILON);
    }

    @Test
    @SuppressWarnings("unchecked")
    void negativeCoordinatesShareCornersWithinACellAndResetClearsTheCache()
            throws ReflectiveOperationException {
        Mojang118CaveDensitySampler sampler = new Mojang118CaveDensitySampler(42L, 1.0F, 1.0F);
        Field cacheField = Mojang118CaveDensitySampler.class.getDeclaredField("interpolatedDensityCache");
        cacheField.setAccessible(true);
        Map<Long, Double> cache = (Map<Long, Double>) cacheField.get(sampler);

        sampler.sampleDensity(-1, 0, -1);
        assertEquals(8, cache.size());

        sampler.sampleDensity(-4, 0, -4);
        assertEquals(8, cache.size(), "-4 through -1 must remain in the same 4x8x4 cell");

        sampler.sampleDensity(-5, 0, -1);
        assertEquals(12, cache.size(), "crossing -4/-5 must add the four corners of the neighboring X cell");

        sampler.resetForChunk();
        assertTrue(cache.isEmpty());
    }

    private static double lerp3(double xFactor, double yFactor, double zFactor, double[] corners) {
        return Mojang118CaveDensitySampler.lerp3(xFactor, yFactor, zFactor,
                corners[0], corners[1], corners[2], corners[3],
                corners[4], corners[5], corners[6], corners[7]);
    }
}
