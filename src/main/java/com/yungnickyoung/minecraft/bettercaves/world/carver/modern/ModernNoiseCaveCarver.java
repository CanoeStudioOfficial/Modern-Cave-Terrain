package com.yungnickyoung.minecraft.bettercaves.world.carver.modern;

import com.yungnickyoung.minecraft.bettercaves.api.ModernCaveCarvingType;
import net.minecraft.block.state.IBlockState;

interface ModernNoiseCaveCarver {
    int getColumnMask();

    ModernCaveCarvingType getType();

    IBlockState getDebugBlock();

    boolean isColumnActive(int x, int z);

    boolean shouldCarve(int x, int y, int z, float fade);
}
