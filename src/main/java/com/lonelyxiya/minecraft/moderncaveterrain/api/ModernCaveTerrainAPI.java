package com.lonelyxiya.minecraft.moderncaveterrain.api;

import com.lonelyxiya.minecraft.moderncaveterrain.ModernCaveTerrain;
import com.lonelyxiya.minecraft.moderncaveterrain.config.ModernCaveTerrainSettings;
import com.lonelyxiya.minecraft.moderncaveterrain.config.io.ConfigLoader;
import com.lonelyxiya.minecraft.moderncaveterrain.config.util.ConfigHolder;
import com.lonelyxiya.minecraft.moderncaveterrain.event.EventModernCaveTerrainGen;
import com.lonelyxiya.minecraft.moderncaveterrain.util.ModernCaveTerrainUtils;
import com.lonelyxiya.minecraft.moderncaveterrain.world.WaterRegionController;
import com.lonelyxiya.minecraft.moderncaveterrain.world.MapGenModernCaveTerrain;
import com.lonelyxiya.minecraft.moderncaveterrain.world.carver.cave.mojang.Mojang118AquiferSampler;
import com.lonelyxiya.minecraft.moderncaveterrain.world.carver.cave.mojang.Mojang118CaveDensitySampler;
import com.lonelyxiya.minecraft.moderncaveterrain.world.mineshaft.MapGenModernMineshaft;
import com.lonelyxiya.minecraft.moderncaveterrain.world.ravine.MapGenModernCanyon;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Public API entry point for Modern Cave Terrain.
 *
 * <p>Other mods should use this class instead of depending on Modern Cave Terrain'
 * internal config, event, and worldgen classes directly.</p>
 */
public final class ModernCaveTerrainAPI {
    public static final String MOD_ID = ModernCaveTerrainSettings.MOD_ID;
    public static final String NAME = ModernCaveTerrainSettings.NAME;
    public static final String VERSION = ModernCaveTerrainSettings.VERSION;
    private static final List<ModernCaveTerrainCaveDecorator> CAVE_DECORATORS = new CopyOnWriteArrayList<>();
    private static final List<ModernCaveTerrainUndergroundBiomeResolver> UNDERGROUND_BIOME_RESOLVERS =
            new CopyOnWriteArrayList<>();

    private ModernCaveTerrainAPI() {}

    /**
     * Registers Modern Cave Terrain' terrain generation replacement handler.
     */
    public static void registerTerrainGenerationHandler() {
        MinecraftForge.TERRAIN_GEN_BUS.register(new EventModernCaveTerrainGen());
    }

    /**
     * Creates a Modern Cave Terrain cave generator for an {@link InitMapGenEvent}.
     *
     * @param event map generation initialization event
     * @return Modern Cave Terrain cave generator typed as a vanilla map generator
     */
    public static MapGenBase createCaveGenerator(InitMapGenEvent event) {
        return new MapGenModernCaveTerrain(event);
    }

    /**
     * Checks whether a map generator is Modern Cave Terrain' cave generator.
     *
     * @param generator map generator to inspect
     * @return true when the generator is a Modern Cave Terrain cave generator
     */
    public static boolean isCaveGenerator(MapGenBase generator) {
        return generator instanceof MapGenModernCaveTerrain;
    }

    /**
     * Creates a Modern Cave Terrain mineshaft generator for an {@link InitMapGenEvent}.
     *
     * @param event map generation initialization event
     * @return Modern Cave Terrain mineshaft generator typed as a vanilla map generator
     */
    public static MapGenBase createMineshaftGenerator(InitMapGenEvent event) {
        return new MapGenModernMineshaft(event);
    }

    /**
     * Checks whether a map generator is Modern Cave Terrain' mineshaft generator.
     *
     * @param generator map generator to inspect
     * @return true when the generator is a Modern Cave Terrain mineshaft generator
     */
    public static boolean isMineshaftGenerator(MapGenBase generator) {
        return generator instanceof MapGenModernMineshaft;
    }

    /**
     * Creates a Modern Cave Terrain ravine generator for an {@link InitMapGenEvent}.
     *
     * @param event map generation initialization event
     * @return Modern Cave Terrain ravine generator typed as a vanilla map generator
     */
    public static MapGenBase createRavineGenerator(InitMapGenEvent event) {
        return new MapGenModernCanyon(event);
    }

    /**
     * Checks whether a map generator is Modern Cave Terrain' ravine generator.
     *
     * @param generator map generator to inspect
     * @return true when the generator is a Modern Cave Terrain ravine generator
     */
    public static boolean isRavineGenerator(MapGenBase generator) {
        return generator instanceof MapGenModernCanyon;
    }

    /**
     * Loads the effective Modern Cave Terrain config for a dimension.
     *
     * @param dimensionId dimension ID
     * @return read-only API view of the dimension config
     */
    public static ModernCaveTerrainConfig getConfigForDimension(int dimensionId) {
        return new ModernCaveTerrainConfig(ConfigLoader.loadConfigFromFileForDimension(dimensionId));
    }

    /**
     * Checks whether Modern Cave Terrain generation is allowed for a dimension.
     *
     * @param dimensionId dimension ID
     * @return true when Modern Cave Terrain should operate in the dimension
     */
    public static boolean canGenerateInDimension(int dimensionId) {
        return ModernCaveTerrainUtils.isDimensionWhitelisted(dimensionId);
    }

    /**
     * Checks whether 1.18-style density caves are enabled for a dimension.
     *
     * @param dimensionId dimension ID
     * @return true when the effective dimension config gives 1.18-style caves a non-zero priority
     */
    public static boolean isMojang118StyleCavesEnabled(int dimensionId) {
        return getConfigForDimension(dimensionId).isMojang118StyleCavesEnabled();
    }

    /**
     * Checks whether 1.18-style density caves are enabled for a world's dimension.
     *
     * @param world world instance
     * @return true when the effective dimension config gives 1.18-style caves a non-zero priority
     */
    public static boolean isMojang118StyleCavesEnabled(World world) {
        return isMojang118StyleCavesEnabled(world.provider.getDimension());
    }

    /**
     * Gets the Modern Cave Terrain custom dimension config directory.
     *
     * @return config directory used for dimension-specific Modern Cave Terrain configs
     */
    public static File getCustomConfigDirectory() {
        if (ModernCaveTerrain.customConfigDir != null) {
            return ModernCaveTerrain.customConfigDir;
        }
        return new File(Loader.instance().getConfigDir(), ModernCaveTerrainSettings.CUSTOM_CONFIG_PATH);
    }

    /**
     * Builds the liquid block map Modern Cave Terrain would use for a chunk.
     *
     * @param world world instance
     * @param chunkX chunk x coordinate
     * @param chunkZ chunk z coordinate
     * @return flat 16x16 liquid block array, indexed as {@code localX * 16 + localZ}
     */
    public static IBlockState[] getLiquidBlocksForChunk(World world, int chunkX, int chunkZ) {
        ConfigHolder config = ConfigLoader.loadConfigFromFileForDimension(world.provider.getDimension());
        return getLiquidBlocksForChunk(world, new ModernCaveTerrainConfig(config), chunkX, chunkZ);
    }

    /**
     * Builds the liquid block map Modern Cave Terrain would use for a chunk with a supplied config.
     *
     * @param world world instance
     * @param config Modern Cave Terrain API config
     * @param chunkX chunk x coordinate
     * @param chunkZ chunk z coordinate
     * @return flat 16x16 liquid block array, indexed as {@code localX * 16 + localZ}
     */
    public static IBlockState[] getLiquidBlocksForChunk(World world, ModernCaveTerrainConfig config, int chunkX, int chunkZ) {
        return new WaterRegionController(world, config.unwrap()).getLiquidBlocksFlatForChunk(chunkX, chunkZ);
    }

    /**
     * Registers a chunk-level cave decorator.
     *
     * <p>Decorators are invoked after Modern Cave Terrain finishes carving a chunk, while the chunk is still represented
     * by a {@link ChunkPrimer}. This is intended for cave-biome decoration systems driven by the generic
     * pseudo-3D underground biome API.</p>
     *
     * @param decorator decorator instance
     */
    public static void registerCaveDecorator(ModernCaveTerrainCaveDecorator decorator) {
        if (decorator == null) {
            throw new IllegalArgumentException("Modern Cave Terrain cave decorator cannot be null");
        }
        CAVE_DECORATORS.add(decorator);
    }

    /**
     * Unregisters a cave decorator.
     *
     * @param decorator decorator instance
     * @return true when the decorator was registered and removed
     */
    public static boolean unregisterCaveDecorator(ModernCaveTerrainCaveDecorator decorator) {
        return CAVE_DECORATORS.remove(decorator);
    }

    /**
     * Registers an underground biome resolver.
     *
     * <p>Resolvers run after the built-in pseudo-3D multi-noise sample and may replace it. This lets decoration
     * mods add or redirect underground biome bands without replacing Modern Cave Terrain internals.</p>
     *
     * @param resolver resolver instance
     */
    public static void registerUndergroundBiomeResolver(ModernCaveTerrainUndergroundBiomeResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("Modern Cave Terrain underground biome resolver cannot be null");
        }
        UNDERGROUND_BIOME_RESOLVERS.add(resolver);
    }

    /**
     * Unregisters an underground biome resolver.
     *
     * @param resolver resolver instance
     * @return true when the resolver was registered and removed
     */
    public static boolean unregisterUndergroundBiomeResolver(ModernCaveTerrainUndergroundBiomeResolver resolver) {
        return UNDERGROUND_BIOME_RESOLVERS.remove(resolver);
    }

    /**
     * Called by Modern Cave Terrain internals after carving.
     */
    public static void decorateCaves(World world, ChunkPrimer primer, int chunkX, int chunkZ, ConfigHolder config) {
        if (CAVE_DECORATORS.isEmpty()) {
            return;
        }

        ModernCaveTerrainCaveDecorationContext context = new ModernCaveTerrainCaveDecorationContext(
                world, primer, chunkX, chunkZ, new ModernCaveTerrainConfig(config));
        for (ModernCaveTerrainCaveDecorator decorator : CAVE_DECORATORS) {
            decorator.decorate(context);
        }
    }

    /**
     * Samples Modern Cave Terrain' modern 1.18-style cave field at a block position.
     *
     * @param world world instance
     * @param blockX block x
     * @param blockY block y
     * @param blockZ block z
     * @return immutable cave sample
     */
    public static ModernCaveTerrainCaveSample sampleCave(World world, int blockX, int blockY, int blockZ) {
        return sampleCave(world, getConfigForDimension(world.provider.getDimension()), blockX, blockY, blockZ);
    }

    /**
     * Samples Modern Cave Terrain' modern 1.18-style cave field at a block position with a supplied config.
     */
    public static ModernCaveTerrainCaveSample sampleCave(World world, ModernCaveTerrainConfig config, int blockX, int blockY, int blockZ) {
        if (!config.isMojang118StyleCavesEnabled()) {
            return new ModernCaveTerrainCaveSample(blockX, blockY, blockZ, 1.0D, 0.0D, false,
                    ModernCaveTerrainCaveType.NONE, ModernCaveTerrainCaveBiomeType.NONE, null);
        }

        int topY = Math.min(world.getActualHeight() - 1, config.getMojang118StyleCaveTop());
        int bottomY = Math.max(0, config.getMojang118StyleCaveBottom());
        if (blockY < bottomY || blockY > topY) {
            return new ModernCaveTerrainCaveSample(blockX, blockY, blockZ, 1.0D,
                    config.getMojang118StyleCaveDensityThreshold(), false,
                    ModernCaveTerrainCaveType.NONE, ModernCaveTerrainCaveBiomeType.NONE, null);
        }

        int surfaceY = Math.min(topY, world.getHeight(new BlockPos(blockX, 0, blockZ)).getY());
        Mojang118CaveDensitySampler densitySampler = new Mojang118CaveDensitySampler(
                world.getSeed(),
                config.getMojang118StyleCaveHorizontalScale(),
                config.getMojang118StyleCaveVerticalScale());
        double density = densitySampler.sampleDensity(blockX, blockY, blockZ);
        density = densitySampler.applySurfaceAdjustment(density, blockX, blockY, blockZ, surfaceY, bottomY,
                config.getMojang118StyleCaveSurfaceCutoffDepth(), surfaceY > world.getSeaLevel());

        double threshold = config.getMojang118StyleCaveDensityThreshold();
        boolean open = density <= threshold;
        Mojang118AquiferSampler aquiferSampler = new Mojang118AquiferSampler(world.getSeed());
        IBlockState substance = aquiferSampler.computeSubstance(blockX, blockY, blockZ, density, surfaceY, world.getSeaLevel(), surfaceY <= world.getSeaLevel());
        IBlockState fluidState = substance != null && substance.getBlock() != Blocks.AIR ? substance : null;

        return new ModernCaveTerrainCaveSample(blockX, blockY, blockZ, density, threshold, open,
                open ? ModernCaveTerrainCaveType.MOJANG_118 : ModernCaveTerrainCaveType.NONE,
                sampleUndergroundBiome(world, config, blockX, blockY, blockZ, fluidState).getCaveBiomeType(),
                fluidState);
    }

    /**
     * Samples the pseudo-3D underground biome field at a block position.
     */
    public static ModernCaveTerrainUndergroundBiomeSample sampleUndergroundBiome(World world, int blockX, int blockY,
                                                                                 int blockZ) {
        return sampleUndergroundBiome(world, getConfigForDimension(world.provider.getDimension()), blockX, blockY,
                blockZ, null);
    }

    /**
     * Samples the pseudo-3D underground biome field at a block position with a known fluid state.
     */
    public static ModernCaveTerrainUndergroundBiomeSample sampleUndergroundBiome(World world, int blockX, int blockY,
                                                                                 int blockZ,
                                                                                 IBlockState fluidState) {
        return sampleUndergroundBiome(world, getConfigForDimension(world.provider.getDimension()), blockX, blockY,
                blockZ, fluidState);
    }

    /**
     * Samples the pseudo-3D underground biome field at a block position with a supplied config.
     */
    public static ModernCaveTerrainUndergroundBiomeSample sampleUndergroundBiome(World world,
                                                                                 ModernCaveTerrainConfig config,
                                                                                 int blockX, int blockY,
                                                                                 int blockZ) {
        return sampleUndergroundBiome(world, config, blockX, blockY, blockZ, null);
    }

    /**
     * Samples the pseudo-3D underground biome field at a block position with a supplied config and fluid state.
     */
    public static ModernCaveTerrainUndergroundBiomeSample sampleUndergroundBiome(World world,
                                                                                 ModernCaveTerrainConfig config,
                                                                                 int blockX, int blockY,
                                                                                 int blockZ,
                                                                                 IBlockState fluidState) {
        ModernCaveTerrainUndergroundBiomeSample sample =
                ModernCaveTerrainUndergroundBiomeSampler.sample(world, config, blockX, blockY, blockZ, fluidState);
        for (ModernCaveTerrainUndergroundBiomeResolver resolver : UNDERGROUND_BIOME_RESOLVERS) {
            ModernCaveTerrainUndergroundBiomeSample replacement = resolver.resolve(world, sample);
            if (replacement != null) {
                sample = replacement;
            }
        }
        return sample;
    }
}
