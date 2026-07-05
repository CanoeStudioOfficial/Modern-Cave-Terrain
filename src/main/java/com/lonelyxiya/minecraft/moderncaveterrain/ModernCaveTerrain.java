package com.lonelyxiya.minecraft.moderncaveterrain;

// Modern Cave Terrain
import com.lonelyxiya.minecraft.moderncaveterrain.config.ModernCaveTerrainSettings;
import com.lonelyxiya.minecraft.moderncaveterrain.api.ModernCaveTerrainAPI;
import com.lonelyxiya.minecraft.moderncaveterrain.proxy.IProxy;

// Minecraft Forge API
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Entry point for Modern Cave Terrain
 */
@Mod(modid = ModernCaveTerrainSettings.MOD_ID, name = ModernCaveTerrainSettings.NAME, version = ModernCaveTerrainSettings.VERSION, useMetadata = ModernCaveTerrainSettings.USE_META_DATA, acceptableRemoteVersions = "*")
public class ModernCaveTerrain {
    public static final Logger LOGGER = LogManager.getLogger(ModernCaveTerrainSettings.MOD_ID);

    /** File referring to the overarching directory for custom dimension configs **/
    public static File customConfigDir;

    @SidedProxy(clientSide = ModernCaveTerrainSettings.CLIENT_PROXY, serverSide = ModernCaveTerrainSettings.SERVER_PROXY)
    public static IProxy proxy;

    /**
     * Pre-Initialization FML Life Cycle event handling method which is automatically
     * called by Forge. Runs before anything else. Read your config, create blocks, items, etc, and
     * register them with the game registry.
     *
     * @param event the event
     */
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit();

        // Create custom dimension config directory if doesn't already exist
        customConfigDir = new File(Loader.instance().getConfigDir(), ModernCaveTerrainSettings.CUSTOM_CONFIG_PATH);
        try {
            String filePath = customConfigDir.getCanonicalPath();
            if (customConfigDir.mkdir())
                ModernCaveTerrain.LOGGER.info("Creating directory for dimension-specific Modern Cave Terrain configs at " + filePath);
        } catch (IOException e) {
            ModernCaveTerrain.LOGGER.warn("ERROR creating Modern Cave Terrain config directory.");
        }
    }

    /**
     * Initialization FML Life Cycle event handling method which is automatically
     * called by Forge. Build data structures needed, register network handlers and
     * world generators, etc.
     *
     * @param event the event
     */
    @EventHandler
    // Perform mod setup.
    // Build whatever data structures are needed, register network handlers, etc.
    public void init(FMLInitializationEvent event) {
        // Register world generation events
        ModernCaveTerrainAPI.registerTerrainGenerationHandler(); // Replace vanilla cave generation
    }
}
