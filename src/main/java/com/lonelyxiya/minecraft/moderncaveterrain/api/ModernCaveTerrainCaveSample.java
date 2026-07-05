package com.lonelyxiya.minecraft.moderncaveterrain.api;

import net.minecraft.block.state.IBlockState;

/**
 * Immutable cave-generation sample for one block position.
 */
public final class ModernCaveTerrainCaveSample {
    private final int blockX;
    private final int blockY;
    private final int blockZ;
    private final double density;
    private final double threshold;
    private final boolean open;
    private final ModernCaveTerrainCaveType caveType;
    private final ModernCaveTerrainCaveBiomeType caveBiomeType;
    private final IBlockState fluidState;

    public ModernCaveTerrainCaveSample(int blockX, int blockY, int blockZ, double density, double threshold,
                                 boolean open, ModernCaveTerrainCaveType caveType,
                                 ModernCaveTerrainCaveBiomeType caveBiomeType, IBlockState fluidState) {
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.density = density;
        this.threshold = threshold;
        this.open = open;
        this.caveType = caveType;
        this.caveBiomeType = caveBiomeType;
        this.fluidState = fluidState;
    }

    public int getBlockX() {
        return blockX;
    }

    public int getBlockY() {
        return blockY;
    }

    public int getBlockZ() {
        return blockZ;
    }

    public double getDensity() {
        return density;
    }

    public double getThreshold() {
        return threshold;
    }

    public boolean isOpen() {
        return open;
    }

    public ModernCaveTerrainCaveType getCaveType() {
        return caveType;
    }

    public ModernCaveTerrainCaveBiomeType getCaveBiomeType() {
        return caveBiomeType;
    }

    public IBlockState getFluidState() {
        return fluidState;
    }
}
