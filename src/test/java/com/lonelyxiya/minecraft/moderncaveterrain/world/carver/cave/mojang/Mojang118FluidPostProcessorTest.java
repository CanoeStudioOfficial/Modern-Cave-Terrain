package com.lonelyxiya.minecraft.moderncaveterrain.world.carver.cave.mojang;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Mojang118FluidPostProcessorTest {
    @Test
    void positionIndexEncodesTheChunkVolumeBoundaries() {
        assertEquals(0, Mojang118FluidPostProcessor.positionIndex(0, 0, 0));
        assertEquals(65_535, Mojang118FluidPostProcessor.positionIndex(15, 255, 15));
    }

    @Test
    void positionIndexKeepsCoordinatesSeparateAndDecodable() {
        int[][] positions = {
                {0, 0, 0},
                {1, 2, 3},
                {15, 0, 0},
                {0, 255, 0},
                {0, 0, 15},
                {7, 128, 11},
                {15, 255, 15}
        };
        Set<Integer> indices = new HashSet<>();

        for (int[] position : positions) {
            int index = Mojang118FluidPostProcessor.positionIndex(position[0], position[1], position[2]);

            assertTrue(indices.add(index), "position index collision at " + index);
            assertEquals(position[0], index >>> 12 & 15);
            assertEquals(position[1], index & 255);
            assertEquals(position[2], index >>> 8 & 15);
        }
    }

    @Test
    void noiseChunkAccumulatesFluidPositionsWithoutAWorldMock() throws ReflectiveOperationException {
        Mojang118NoiseChunk noiseChunk = new Mojang118NoiseChunk(null, 0, 0, 0);
        noiseChunk.markFluidForPostProcessing(1, 2, 3);
        noiseChunk.markFluidForPostProcessing(7, 128, 11);
        noiseChunk.markFluidForPostProcessing(1, 2, 3);

        Field field = Mojang118NoiseChunk.class.getDeclaredField("fluidPostProcessingPositions");
        field.setAccessible(true);
        BitSet positions = (BitSet) field.get(noiseChunk);

        assertEquals(2, positions.cardinality());
        assertTrue(positions.get(Mojang118FluidPostProcessor.positionIndex(1, 2, 3)));
        assertTrue(positions.get(Mojang118FluidPostProcessor.positionIndex(7, 128, 11)));
    }

    @Test
    void pendingPositionsRoundTripThroughChunkNbtEncoding() {
        BitSet positions = new BitSet(65_536);
        positions.set(Mojang118FluidPostProcessor.positionIndex(0, 0, 0));
        positions.set(Mojang118FluidPostProcessor.positionIndex(7, 128, 11));
        positions.set(Mojang118FluidPostProcessor.positionIndex(15, 255, 15));

        byte[] encoded = Mojang118FluidPostProcessor.encodePending(positions);

        assertEquals(positions, Mojang118FluidPostProcessor.decodePending(encoded));
    }
}
