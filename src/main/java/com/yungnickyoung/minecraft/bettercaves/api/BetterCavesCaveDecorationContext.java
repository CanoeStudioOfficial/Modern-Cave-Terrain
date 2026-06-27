package com.yungnickyoung.minecraft.bettercaves.api;

import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.chunk.ChunkPrimer;

/**
 * Context passed to cave decorators.
 */
public final class BetterCavesCaveDecorationContext {
    private final World world;
    private final ChunkPrimer primer;
    private final int chunkX;
    private final int chunkZ;
    private final BetterCavesConfig config;

    public BetterCavesCaveDecorationContext(World world, ChunkPrimer primer, int chunkX, int chunkZ,
                                            BetterCavesConfig config) {
        this.world = world;
        this.primer = primer;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.config = config;
    }

    public World getWorld() {
        return world;
    }

    public ChunkPrimer getPrimer() {
        return primer;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public int getBlockX(int localX) {
        return chunkX * 16 + localX;
    }

    public int getBlockZ(int localZ) {
        return chunkZ * 16 + localZ;
    }

    public IBlockState getBlockState(int localX, int y, int localZ) {
        return primer.getBlockState(localX, y, localZ);
    }

    public void setBlockState(int localX, int y, int localZ, IBlockState state) {
        primer.setBlockState(localX, y, localZ, state);
    }

    public BetterCavesConfig getConfig() {
        return config;
    }

    public BetterCavesCaveSample sampleLocal(int localX, int y, int localZ) {
        return sample(getBlockX(localX), y, getBlockZ(localZ));
    }

    public BetterCavesCaveSample sample(int blockX, int blockY, int blockZ) {
        return BetterCavesAPI.sampleCave(world, config, blockX, blockY, blockZ);
    }
}
