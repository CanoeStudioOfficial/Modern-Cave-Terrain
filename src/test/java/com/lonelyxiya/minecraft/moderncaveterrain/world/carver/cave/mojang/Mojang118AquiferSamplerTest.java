package com.lonelyxiya.minecraft.moderncaveterrain.world.carver.cave.mojang;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Mojang118AquiferSamplerTest {
    @Test
    void cellKeysRemainUniqueAcrossTheLegacyWorldBorder() {
        long negativeWorldBorderCell = Mojang118AquiferSampler.cellKey(-1_500_000, 7, 0);
        long positiveCell = Mojang118AquiferSampler.cellKey(597_152, 7, 0);

        assertNotEquals(negativeWorldBorderCell, positiveCell);
    }

    @Test
    void cellKeyKeepsCoordinatesInSeparateBitRanges() {
        long origin = Mojang118AquiferSampler.cellKey(0, 0, 0);

        assertNotEquals(origin, Mojang118AquiferSampler.cellKey(1, 0, 0));
        assertNotEquals(origin, Mojang118AquiferSampler.cellKey(0, 1, 0));
        assertNotEquals(origin, Mojang118AquiferSampler.cellKey(0, 0, 1));
    }

    @Test
    void directCellKeysDoNotRepeatTheOldChunkXorCollision() {
        long oldFirst = oldCellKey(0, 7, 1) ^ chunkKey(0, 0);
        long oldSecond = oldCellKey(0, 7, 0) ^ chunkKey(0, 1);
        assertEquals(oldFirst, oldSecond);

        assertNotEquals(Mojang118AquiferSampler.cellKey(0, 7, 1),
                Mojang118AquiferSampler.cellKey(0, 7, 0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void resetForChunkClearsAllPerNoiseChunkState() throws ReflectiveOperationException {
        Mojang118AquiferSampler sampler = new Mojang118AquiferSampler(42L);
        Map<Long, Long> locations = (Map<Long, Long>) field("locationCache").get(sampler);
        Map<Long, Object> statuses = (Map<Long, Object>) field("statusCache").get(sampler);
        Field updateFlag = field("shouldScheduleFluidUpdate");

        locations.put(1L, 2L);
        statuses.put(1L, null);
        updateFlag.setBoolean(sampler, true);
        sampler.resetForChunk();

        assertTrue(locations.isEmpty());
        assertTrue(statuses.isEmpty());
        assertFalse(sampler.shouldScheduleFluidUpdate());
    }

    @Test
    void floodedUndergroundToggleIsFixedForTheSamplerLifetime() throws ReflectiveOperationException {
        Mojang118AquiferSampler enabled = new Mojang118AquiferSampler(42L, 6, true);
        Mojang118AquiferSampler disabled = new Mojang118AquiferSampler(42L, 6, false);
        Field globalAquifersEnabled = field("globalAquifersEnabled");

        assertTrue(globalAquifersEnabled.getBoolean(enabled));
        assertFalse(globalAquifersEnabled.getBoolean(disabled));
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field field = Mojang118AquiferSampler.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static long oldCellKey(int cellX, int cellY, int cellZ) {
        return (((long) cellX & 0x1FFFFFL) << 43)
                ^ (((long) cellY & 0x1FFFFFL) << 22)
                ^ ((long) cellZ & 0x3FFFFFL);
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (((long) chunkX & 0xffffffffL) << 32) ^ ((long) chunkZ & 0xffffffffL);
    }
}
