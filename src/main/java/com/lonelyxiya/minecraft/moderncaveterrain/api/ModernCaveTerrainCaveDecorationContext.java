package com.lonelyxiya.minecraft.moderncaveterrain.api;

import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.chunk.ChunkPrimer;

/**
 * Context passed to cave decorators.
 */
public final class ModernCaveTerrainCaveDecorationContext {
    private final World world;
    private final ChunkPrimer primer;
    private final int chunkX;
    private final int chunkZ;
    private final ModernCaveTerrainConfig config;

    public ModernCaveTerrainCaveDecorationContext(World world, ChunkPrimer primer, int chunkX, int chunkZ,
                                            ModernCaveTerrainConfig config) {
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

    public ModernCaveTerrainConfig getConfig() {
        return config;
    }

    public ModernCaveTerrainCaveSample sampleLocal(int localX, int y, int localZ) {
        return sample(getBlockX(localX), y, getBlockZ(localZ));
    }

    public ModernCaveTerrainCaveSample sample(int blockX, int blockY, int blockZ) {
        return ModernCaveTerrainAPI.sampleCave(world, config, blockX, blockY, blockZ);
    }
}
