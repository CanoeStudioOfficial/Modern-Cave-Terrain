package com.lonelyxiya.minecraft.moderncaveterrain.world.carver.cave.mojang;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Adapts modern aquifer fluid post-processing to the 1.12.2 chunk lifecycle.
 *
 * <p>Modern generation marks unstable fluid positions on a proto-chunk and updates them after the chunk becomes
 * live. A {@code ChunkPrimer} has no equivalent queue, so positions are retained until Forge finishes populating
 * the corresponding chunk. Static 1.12.2 liquids are then converted to their flowing form and scheduled for a
 * normal liquid tick without vetoing the fluid placement.</p>
 */
public final class Mojang118FluidPostProcessor {
    public static final Mojang118FluidPostProcessor INSTANCE = new Mojang118FluidPostProcessor();

    private static final String PENDING_FLUIDS_NBT_KEY = "ModernCaveTerrainAquiferFluidUpdates";
    private static final Map<World, Map<Long, BitSet>> PENDING_BY_WORLD = new WeakHashMap<>();
    private static final Map<World, Set<Long>> READY_BY_WORLD = new WeakHashMap<>();

    private Mojang118FluidPostProcessor() {
    }

    static void enqueue(World world, int chunkX, int chunkZ, BitSet positions) {
        storePending(world, chunkX, chunkZ, positions, true);
    }

    private static synchronized void storePending(World world, int chunkX, int chunkZ, BitSet positions,
                                                  boolean markReady) {
        if (world == null || positions.isEmpty()) {
            return;
        }

        Map<Long, BitSet> pendingByChunk = PENDING_BY_WORLD.computeIfAbsent(world, ignored -> new HashMap<>());
        long chunkKey = chunkKey(chunkX, chunkZ);
        BitSet pending = pendingByChunk.get(chunkKey);
        if (pending == null) {
            pendingByChunk.put(chunkKey, (BitSet) positions.clone());
        }
        else {
            pending.or(positions);
        }
        if (markReady) {
            READY_BY_WORLD.computeIfAbsent(world, ignored -> new HashSet<>()).add(chunkKey);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPopulateChunkPost(PopulateChunkEvent.Post event) {
        markReady(event.getWorld(), event.getChunkX(), event.getChunkZ());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkEvent.Load event) {
        ChunkPos chunkPos = event.getChunk().getPos();
        markReadyNeighborhood(event.getWorld(), chunkPos.x, chunkPos.z);
    }

    @SubscribeEvent
    public void onChunkDataSave(ChunkDataEvent.Save event) {
        ChunkPos chunkPos = event.getChunk().getPos();
        BitSet pending = copyPending(event.getWorld(), chunkPos.x, chunkPos.z);
        NBTTagCompound data = event.getData();
        if (pending == null || pending.isEmpty()) {
            data.removeTag(PENDING_FLUIDS_NBT_KEY);
        }
        else {
            data.setByteArray(PENDING_FLUIDS_NBT_KEY, encodePending(pending));
        }
    }

    @SubscribeEvent
    public void onChunkDataLoad(ChunkDataEvent.Load event) {
        NBTTagCompound data = event.getData();
        if (!data.hasKey(PENDING_FLUIDS_NBT_KEY)) {
            return;
        }

        byte[] encoded = data.getByteArray(PENDING_FLUIDS_NBT_KEY);
        if (encoded.length > 0) {
            ChunkPos chunkPos = event.getChunk().getPos();
            enqueue(event.getWorld(), chunkPos.x, chunkPos.z, decodePending(encoded));
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }

        Set<Long> readyChunks = takeReady(event.world);
        if (readyChunks == null) {
            return;
        }

        for (long chunkKey : readyChunks) {
            int chunkX = (int) (chunkKey >> 32);
            int chunkZ = (int) chunkKey;
            Chunk chunk = getLoadedChunk(event.world, chunkX, chunkZ);
            if (chunk != null && chunk.isTerrainPopulated() && process(event.world, chunkX, chunkZ)) {
                chunk.markDirty();
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        synchronized (Mojang118FluidPostProcessor.class) {
            PENDING_BY_WORLD.remove(event.getWorld());
            READY_BY_WORLD.remove(event.getWorld());
        }
    }

    private static boolean process(World world, int chunkX, int chunkZ) {
        if (world.isRemote) {
            return false;
        }

        BitSet positions = take(world, chunkX, chunkZ);
        if (positions == null) {
            return false;
        }

        int minBlockX = chunkX * 16;
        int minBlockZ = chunkZ * 16;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BitSet deferred = new BitSet(16 * 256 * 16);
        BitSet retry = new BitSet(16 * 256 * 16);
        for (int index = positions.nextSetBit(0); index >= 0; index = positions.nextSetBit(index + 1)) {
            int localX = index >>> 12 & 15;
            int localZ = index >>> 8 & 15;
            int y = index & 255;
            pos.setPos(minBlockX + localX, y, minBlockZ + localZ);
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            boolean water = block == Blocks.WATER || block == Blocks.FLOWING_WATER;
            boolean lava = block == Blocks.LAVA || block == Blocks.FLOWING_LAVA;
            if ((water || lava) && !world.isAreaLoaded(pos, water ? 4 : 2, false)) {
                deferred.set(index);
                continue;
            }

            if (block == Blocks.WATER || block == Blocks.LAVA) {
                Block flowingBlock = block == Blocks.WATER ? Blocks.FLOWING_WATER : Blocks.FLOWING_LAVA;
                IBlockState flowingState = flowingBlock.getDefaultState()
                        .withProperty(BlockLiquid.LEVEL, state.getValue(BlockLiquid.LEVEL));
                if (!world.setBlockState(pos, flowingState, 2)
                        && world.getBlockState(pos).getBlock() != flowingBlock) {
                    retry.set(index);
                    continue;
                }
                world.scheduleUpdate(pos, flowingBlock, block.tickRate(world));
            }
            else if (block == Blocks.FLOWING_WATER || block == Blocks.FLOWING_LAVA) {
                world.scheduleUpdate(pos, block, block.tickRate(world));
            }
        }

        if (!deferred.isEmpty()) {
            storePending(world, chunkX, chunkZ, deferred, false);
        }
        if (!retry.isEmpty()) {
            storePending(world, chunkX, chunkZ, retry, true);
        }
        return true;
    }

    private static synchronized BitSet take(World world, int chunkX, int chunkZ) {
        Map<Long, BitSet> pendingByChunk = PENDING_BY_WORLD.get(world);
        if (pendingByChunk == null) {
            return null;
        }

        BitSet positions = pendingByChunk.remove(chunkKey(chunkX, chunkZ));
        if (pendingByChunk.isEmpty()) {
            PENDING_BY_WORLD.remove(world);
        }
        return positions;
    }

    private static synchronized BitSet copyPending(World world, int chunkX, int chunkZ) {
        Map<Long, BitSet> pendingByChunk = PENDING_BY_WORLD.get(world);
        if (pendingByChunk == null) {
            return null;
        }

        BitSet pending = pendingByChunk.get(chunkKey(chunkX, chunkZ));
        return pending == null ? null : (BitSet) pending.clone();
    }

    private static synchronized void markReady(World world, int chunkX, int chunkZ) {
        Map<Long, BitSet> pendingByChunk = PENDING_BY_WORLD.get(world);
        long key = chunkKey(chunkX, chunkZ);
        if (pendingByChunk == null || !pendingByChunk.containsKey(key)) {
            return;
        }

        READY_BY_WORLD.computeIfAbsent(world, ignored -> new HashSet<>()).add(key);
    }

    private static synchronized void markReadyNeighborhood(World world, int chunkX, int chunkZ) {
        Map<Long, BitSet> pendingByChunk = PENDING_BY_WORLD.get(world);
        if (pendingByChunk == null) {
            return;
        }

        Set<Long> ready = null;
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                long key = chunkKey(chunkX + offsetX, chunkZ + offsetZ);
                if (pendingByChunk.containsKey(key)) {
                    if (ready == null) {
                        ready = READY_BY_WORLD.computeIfAbsent(world, ignored -> new HashSet<>());
                    }
                    ready.add(key);
                }
            }
        }
    }

    private static synchronized Set<Long> takeReady(World world) {
        Set<Long> ready = READY_BY_WORLD.remove(world);
        return ready == null ? null : new HashSet<>(ready);
    }

    private static Chunk getLoadedChunk(World world, int chunkX, int chunkZ) {
        IChunkProvider provider = world.getChunkProvider();
        if (provider instanceof ChunkProviderServer) {
            return ((ChunkProviderServer) provider).getLoadedChunk(chunkX, chunkZ);
        }

        BlockPos probe = new BlockPos(chunkX * 16, 0, chunkZ * 16);
        return world.isBlockLoaded(probe, false) ? world.getChunk(chunkX, chunkZ) : null;
    }

    static int positionIndex(int localX, int y, int localZ) {
        return localX << 12 | localZ << 8 | y;
    }

    static byte[] encodePending(BitSet positions) {
        return positions.toByteArray();
    }

    static BitSet decodePending(byte[] encoded) {
        BitSet positions = BitSet.valueOf(encoded);
        if (positions.length() > 65_536) {
            positions.clear(65_536, positions.length());
        }
        return positions;
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (long) chunkX << 32 ^ (long) chunkZ & 0xffffffffL;
    }
}
