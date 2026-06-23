package com.yungnickyoung.minecraft.bettercaves.api;

import com.yungnickyoung.minecraft.bettercaves.config.util.ConfigHolder;

import java.util.Collections;
import java.util.Set;

/**
 * Read-only view of a Better Caves configuration.
 *
 * <p>This wrapper lets other mods inspect Better Caves settings without depending
 * directly on the internal config loader classes.</p>
 */
public final class BetterCavesConfig {
    private final ConfigHolder config;

    BetterCavesConfig(ConfigHolder config) {
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
            throw new IllegalArgumentException("Better Caves config option '" + optionName
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

    ConfigHolder unwrap() {
        return config;
    }

    private ConfigHolder.ConfigOption<?> getOption(String optionName) {
        ConfigHolder.ConfigOption<?> option = config.properties.get(optionName);
        if (option == null) {
            throw new IllegalArgumentException("Unknown Better Caves config option: " + optionName);
        }
        return option;
    }
}
