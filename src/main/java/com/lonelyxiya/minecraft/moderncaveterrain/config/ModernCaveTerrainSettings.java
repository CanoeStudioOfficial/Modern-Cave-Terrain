package com.lonelyxiya.minecraft.moderncaveterrain.config;

/**
 * Settings and information for Modern Cave Terrain.
 * All fields are {@code static}.
 *
 * For the user-facing configuration options, see the Configuration class.
 */
public class ModernCaveTerrainSettings {
    /** MOD INFORMATION CONSTANTS
     * These will not be used if USE_META_DATA is true. Instead, data will be used from resources/mcmod.info.
     */
    public static final boolean USE_META_DATA = true;
    public static final String MOD_ID = "moderncaveterrain";
    public static final String NAME = "Modern Cave Terrain";
    public static final String VERSION = "2.1.0";

    public static final String SERVER_PROXY = "com.lonelyxiya.minecraft.moderncaveterrain.proxy.ServerProxy";
    public static final String CLIENT_PROXY = "com.lonelyxiya.minecraft.moderncaveterrain.proxy.ClientProxy";

    public static final String CUSTOM_CONFIG_PATH = "moderncaveterrain-1_12_2";

    public static final int SUB_CHUNK_SIZE = 4;
    public static final int NOISE_VERTICAL_SAMPLE_STEP = 4;
    public static final float[] START_COEFFS = new float[SUB_CHUNK_SIZE];
    public static final float[] END_COEFFS = new float[SUB_CHUNK_SIZE];

    static {
        // Calculate coefficients used for bilinear interpolation during noise calculation.
        // These are initialized one time here to avoid redundant computation later on.
        for (int n = 0; n < SUB_CHUNK_SIZE; n++) {
            START_COEFFS[n] = (float)(SUB_CHUNK_SIZE - 1 - n) / (SUB_CHUNK_SIZE - 1);
            END_COEFFS[n] = (float)(n) / (SUB_CHUNK_SIZE - 1);
        }
    }

    private ModernCaveTerrainSettings() {} // private constructor prevents instantiation
}
