package com.yungnickyoung.minecraft.bettercaves.world.carver.modern;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

class ModernCarvingContext {
    final World world;
    final ChunkPrimer primer;
    final int chunkX;
    final int chunkZ;
    final int chunkBlockX;
    final int chunkBlockZ;
    final boolean hasApiCallbacks;

    private final int[][] surfaceAltitudes;
    private final IBlockState[][] liquidBlocks;
    final BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

    ModernCarvingContext(World world, ChunkPrimer primer, int chunkX, int chunkZ, int[][] surfaceAltitudes,
                         IBlockState[][] liquidBlocks, boolean hasApiCallbacks) {
        this.world = world;
        this.primer = primer;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.chunkBlockX = chunkX * 16;
        this.chunkBlockZ = chunkZ * 16;
        this.surfaceAltitudes = surfaceAltitudes;
        this.liquidBlocks = liquidBlocks;
        this.hasApiCallbacks = hasApiCallbacks;
    }

    int blockX(int localX) {
        return chunkBlockX + localX;
    }

    int blockZ(int localZ) {
        return chunkBlockZ + localZ;
    }

    int localX(int blockX) {
        return blockX - chunkBlockX;
    }

    int localZ(int blockZ) {
        return blockZ - chunkBlockZ;
    }

    int surfaceAltitude(int localX, int localZ) {
        return surfaceAltitudes[localX][localZ];
    }

    IBlockState fallbackLiquidBlock(int localX, int localZ) {
        return liquidBlocks[localX][localZ];
    }
}
