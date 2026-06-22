package com.yungnickyoung.minecraft.bettercaves.api;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@FunctionalInterface
public interface ModernCaveCarvingCallback {
    /**
     * Allows integration mods to replace the block Better Caves is about to carve.
     *
     * @param world the world being generated
     * @param pos the absolute block position being carved
     * @param originalState the block state currently in the chunk primer
     * @param proposedState the block state Better Caves would place
     * @param carvingType the modern cave source that selected this block
     * @return the state to place, or null to keep the current proposed state
     */
    IBlockState getCarvedBlock(World world, BlockPos pos, IBlockState originalState,
                               IBlockState proposedState, ModernCaveCarvingType carvingType);
}
