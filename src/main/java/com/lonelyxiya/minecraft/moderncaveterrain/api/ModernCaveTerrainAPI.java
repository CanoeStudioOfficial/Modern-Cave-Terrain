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
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;
import net.minecraftforge.common.BiomeDictionary;
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
     * by a {@link ChunkPrimer}. This is intended for cave-biome decoration systems such as lush caves and
     * dripstone caves.</p>
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
                classifyCaveBiome(world, blockX, blockY, blockZ, fluidState), fluidState);
    }

    private static ModernCaveTerrainCaveBiomeType classifyCaveBiome(World world, int blockX, int blockY, int blockZ,
                                                              IBlockState fluidState) {
        if (fluidState != null && fluidState.getBlock() == Blocks.LAVA) {
            return ModernCaveTerrainCaveBiomeType.LAVA;
        }
        if (fluidState != null && fluidState.getBlock() == Blocks.WATER) {
            return ModernCaveTerrainCaveBiomeType.UNDERWATER;
        }

        double region = smoothNoise(world.getSeed() ^ 0x424341564542494FL, blockX * 0.012D, 0.0D, blockZ * 0.012D);
        double detail = smoothNoise(world.getSeed() ^ 0x4452495053544F4EL, blockX * 0.018D, blockY * 0.03D, blockZ * 0.018D);
        Biome biome = world.getBiome(new BlockPos(blockX, 0, blockZ));
        boolean wetSurface = biome.getRainfall() >= 0.8F
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.WET)
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.JUNGLE)
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.SWAMP);

        if (wetSurface && blockY >= 8 && blockY <= 70 && region + detail * 0.25D > 0.12D) {
            return ModernCaveTerrainCaveBiomeType.LUSH;
        }
        if (blockY >= 4 && blockY <= 72 && region * 0.35D - detail > 0.18D) {
            return ModernCaveTerrainCaveBiomeType.DRIPSTONE;
        }
        if (blockY <= 18 && region < -0.45D) {
            return ModernCaveTerrainCaveBiomeType.DEEP_DARK;
        }

        return ModernCaveTerrainCaveBiomeType.NORMAL;
    }

    private static double smoothNoise(long seed, double x, double y, double z) {
        int x0 = floor(x);
        int y0 = floor(y);
        int z0 = floor(z);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        int z1 = z0 + 1;
        double tx = smoothstep(x - x0);
        double ty = smoothstep(y - y0);
        double tz = smoothstep(z - z0);
        double x00 = lerp(tx, valueNoise(seed, x0, y0, z0), valueNoise(seed, x1, y0, z0));
        double x10 = lerp(tx, valueNoise(seed, x0, y1, z0), valueNoise(seed, x1, y1, z0));
        double x01 = lerp(tx, valueNoise(seed, x0, y0, z1), valueNoise(seed, x1, y0, z1));
        double x11 = lerp(tx, valueNoise(seed, x0, y1, z1), valueNoise(seed, x1, y1, z1));
        return lerp(tz, lerp(ty, x00, x10), lerp(ty, x01, x11));
    }

    private static double valueNoise(long seed, int x, int y, int z) {
        long hash = seed;
        hash ^= x * 341873128712L;
        hash ^= y * 132897987541L;
        hash ^= z * 42317861L;
        hash ^= hash >> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >> 33;
        hash *= 0xc4ceb9fe1a85ec53L;
        hash ^= hash >> 33;
        return ((hash >>> 11) * 0x1.0p-53D) * 2.0D - 1.0D;
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static double smoothstep(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static double lerp(double factor, double from, double to) {
        return from + factor * (to - from);
    }
}
