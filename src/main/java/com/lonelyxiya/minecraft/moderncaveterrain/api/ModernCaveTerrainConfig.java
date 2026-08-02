package com.lonelyxiya.minecraft.moderncaveterrain.api;

import com.lonelyxiya.minecraft.moderncaveterrain.config.util.ConfigHolder;

import java.util.Collections;
import java.util.Set;

/**
 * Read-only view of a Modern Cave Terrain configuration.
 *
 * <p>This wrapper lets other mods inspect Modern Cave Terrain settings without depending
 * directly on the internal config loader classes.</p>
 */
public final class ModernCaveTerrainConfig {
    private final ConfigHolder config;

    ModernCaveTerrainConfig(ConfigHolder config) {
        this.config = config;
    }

    /**
     * Checks whether this config contains an option with the given qualified name.
     *
     * @param optionName full config option name, for example
     *                   {@code general.underground generation.miscellaneous.Liquid Altitude}
     * @return true when the option exists
     */
    public boolean hasOption(String optionName) {
        return config.properties.containsKey(optionName);
    }

    /**
     * Returns all qualified option names available in this config.
     *
     * @return unmodifiable set of option names
     */
    public Set<String> getOptionNames() {
        return Collections.unmodifiableSet(config.properties.keySet());
    }

    /**
     * Reads a config option and verifies its expected type.
     *
     * @param optionName full config option name
     * @param type expected value type
     * @param <T> expected value type
     * @return option value
     * @throws IllegalArgumentException if the option is missing or has a different type
     */
    public <T> T get(String optionName, Class<T> type) {
        ConfigHolder.ConfigOption<?> option = getOption(optionName);
        Object value = option.get();
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Modern Cave Terrain config option '" + optionName
                + "' is " + value.getClass().getName() + ", not " + type.getName());
        }
        return type.cast(value);
    }

    /**
     * Reads a config option without requiring the caller to know its exact type.
     *
     * @param optionName full config option name
     * @return option value
     * @throws IllegalArgumentException if the option is missing
     */
    public Object get(String optionName) {
        return getOption(optionName).get();
    }

    public boolean getBoolean(String optionName) {
        return get(optionName, Boolean.class);
    }

    public int getInt(String optionName) {
        return get(optionName, Integer.class);
    }

    public float getFloat(String optionName) {
        return get(optionName, Float.class);
    }

    public String getString(String optionName) {
        return get(optionName, String.class);
    }

    /**
     * Checks whether 1.18-style density caves are enabled in this config.
     *
     * @return true when the 1.18-style cave priority is greater than zero
     */
    public boolean isMojang118StyleCavesEnabled() {
        return getMojang118StyleCavePriority() > 0;
    }

    public int getMojang118StyleCavePriority() {
        return getInt(ModernCaveTerrainConfigOptions.MOJANG_118_CAVE_PRIORITY);
    }

    public int getMojang118StyleCaveBottom() {
        return getInt(ModernCaveTerrainConfigOptions.MOJANG_118_CAVE_BOTTOM);
    }

    public int getMojang118StyleCaveTop() {
        return getInt(ModernCaveTerrainConfigOptions.MOJANG_118_CAVE_TOP);
    }

    public int getMojang118StyleCaveSurfaceCutoffDepth() {
        return getInt(ModernCaveTerrainConfigOptions.MOJANG_118_CAVE_SURFACE_CUTOFF_DEPTH);
    }

    public float getMojang118StyleCaveDensityThreshold() {
        return getFloat(ModernCaveTerrainConfigOptions.MOJANG_118_CAVE_DENSITY_THRESHOLD);
    }

    public float getMojang118StyleCaveHorizontalScale() {
        return getFloat(ModernCaveTerrainConfigOptions.MOJANG_118_CAVE_HORIZONTAL_SCALE);
    }

    public float getMojang118StyleCaveVerticalScale() {
        return getFloat(ModernCaveTerrainConfigOptions.MOJANG_118_CAVE_VERTICAL_SCALE);
    }

    public int getLiquidAltitude() {
        return getInt(ModernCaveTerrainConfigOptions.LIQUID_ALTITUDE);
    }

    /** Returns whether ocean flooding and the modern global sea-level aquifer picker are enabled. */
    public boolean isFloodedUndergroundEnabled() {
        return getBoolean(ModernCaveTerrainConfigOptions.FLOODED_UNDERGROUND_ENABLED);
    }

    ConfigHolder unwrap() {
        return config;
    }

    private ConfigHolder.ConfigOption<?> getOption(String optionName) {
        ConfigHolder.ConfigOption<?> option = config.properties.get(optionName);
        if (option == null) {
            throw new IllegalArgumentException("Unknown Modern Cave Terrain config option: " + optionName);
        }
        return option;
    }
}
