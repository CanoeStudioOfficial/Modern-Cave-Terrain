package com.yungnickyoung.minecraft.bettercaves.api;

import com.yungnickyoung.minecraft.bettercaves.BetterCaves;
import com.yungnickyoung.minecraft.bettercaves.config.BCSettings;
import com.yungnickyoung.minecraft.bettercaves.config.io.ConfigLoader;
import com.yungnickyoung.minecraft.bettercaves.config.util.ConfigHolder;
import com.yungnickyoung.minecraft.bettercaves.event.EventBetterCaveGen;
import com.yungnickyoung.minecraft.bettercaves.util.BetterCavesUtils;
import com.yungnickyoung.minecraft.bettercaves.world.WaterRegionController;
import com.yungnickyoung.minecraft.bettercaves.world.MapGenBetterCaves;
import com.yungnickyoung.minecraft.bettercaves.world.mineshaft.MapGenBetterMineshaft;
import com.yungnickyoung.minecraft.bettercaves.world.ravine.MapGenBetterRavine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.gen.MapGenBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.fml.common.Loader;

import java.io.File;

/**
 * Public API entry point for Better Caves.
 *
 * <p>Other mods should use this class instead of depending on Better Caves'
 * internal config, event, and worldgen classes directly.</p>
 */
public final class BetterCavesAPI {
    public static final String MOD_ID = BCSettings.MOD_ID;
    public static final String NAME = BCSettings.NAME;
    public static final String VERSION = BCSettings.VERSION;

    private BetterCavesAPI() {}

    /**
     * Registers Better Caves' terrain generation replacement handler.
     */
    public static void registerTerrainGenerationHandler() {
        MinecraftForge.TERRAIN_GEN_BUS.register(new EventBetterCaveGen());
    }

    /**
     * Creates a Better Caves cave generator for an {@link InitMapGenEvent}.
     *
     * @param event map generation initialization event
     * @return Better Caves cave generator typed as a vanilla map generator
     */
    public static MapGenBase createCaveGenerator(InitMapGenEvent event) {
        return new MapGenBetterCaves(event);
    }

    /**
     * Creates a Better Caves mineshaft generator for an {@link InitMapGenEvent}.
     *
     * @param event map generation initialization event
     * @return Better Caves mineshaft generator typed as a vanilla map generator
     */
    public static MapGenBase createMineshaftGenerator(InitMapGenEvent event) {
        return new MapGenBetterMineshaft(event);
    }

    /**
     * Creates a Better Caves ravine generator for an {@link InitMapGenEvent}.
     *
     * @param event map generation initialization event
     * @return Better Caves ravine generator typed as a vanilla map generator
     */
    public static MapGenBase createRavineGenerator(InitMapGenEvent event) {
        return new MapGenBetterRavine(event);
    }

    /**
     * Loads the effective Better Caves config for a dimension.
     *
     * @param dimensionId dimension ID
     * @return read-only API view of the dimension config
     */
    public static BetterCavesConfig getConfigForDimension(int dimensionId) {
        return new BetterCavesConfig(ConfigLoader.loadConfigFromFileForDimension(dimensionId));
    }

    /**
     * Checks whether Better Caves generation is allowed for a dimension.
     *
     * @param dimensionId dimension ID
     * @return true when Better Caves should operate in the dimension
     */
    public static boolean canGenerateInDimension(int dimensionId) {
        return BetterCavesUtils.isDimensionWhitelisted(dimensionId);
    }

    /**
     * Gets the Better Caves custom dimension config directory.
     *
     * @return config directory used for dimension-specific Better Caves configs
     */
    public static File getCustomConfigDirectory() {
        if (BetterCaves.customConfigDir != null) {
            return BetterCaves.customConfigDir;
        }
        return new File(Loader.instance().getConfigDir(), BCSettings.CUSTOM_CONFIG_PATH);
    }

    /**
     * Builds the liquid block map Better Caves would use for a chunk.
     *
     * @param world world instance
     * @param chunkX chunk x coordinate
     * @param chunkZ chunk z coordinate
     * @return flat 16x16 liquid block array, indexed as {@code localX * 16 + localZ}
     */
    public static IBlockState[] getLiquidBlocksForChunk(World world, int chunkX, int chunkZ) {
        ConfigHolder config = ConfigLoader.loadConfigFromFileForDimension(world.provider.getDimension());
        return getLiquidBlocksForChunk(world, new BetterCavesConfig(config), chunkX, chunkZ);
    }

    /**
     * Builds the liquid block map Better Caves would use for a chunk with a supplied config.
     *
     * @param world world instance
     * @param config Better Caves API config
     * @param chunkX chunk x coordinate
     * @param chunkZ chunk z coordinate
     * @return flat 16x16 liquid block array, indexed as {@code localX * 16 + localZ}
     */
    public static IBlockState[] getLiquidBlocksForChunk(World world, BetterCavesConfig config, int chunkX, int chunkZ) {
        return new WaterRegionController(world, config.unwrap()).getLiquidBlocksFlatForChunk(chunkX, chunkZ);
    }
}
