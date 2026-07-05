package com.lonelyxiya.minecraft.moderncaveterrain.api;

/**
 * Qualified config option names exposed for API consumers.
 */
public final class ModernCaveTerrainConfigOptions {
    public static final String CAVE_REGION_SIZE = "general.underground generation.caves.Cave Region Size";
    public static final String CAVE_REGION_CUSTOM_SIZE = "general.underground generation.caves.Cave Region Size Custom Value";
    public static final String CAVE_SPAWN_CHANCE = "general.underground generation.caves.Cave Spawn Chance";

    public static final String CUBIC_CAVE_BOTTOM = "general.underground generation.caves.type 1 caves.Type 1 Cave Minimum Altitude";
    public static final String CUBIC_CAVE_TOP = "general.underground generation.caves.type 1 caves.Type 1 Cave Maximum Altitude";
    public static final String CUBIC_CAVE_SURFACE_CUTOFF_DEPTH = "general.underground generation.caves.type 1 caves.Type 1 Cave Surface Cutoff Depth";
    public static final String CUBIC_CAVE_Y_COMPRESSION = "general.underground generation.caves.type 1 caves.Compression - Vertical";
    public static final String CUBIC_CAVE_XZ_COMPRESSION = "general.underground generation.caves.type 1 caves.Compression - Horizontal";
    public static final String CUBIC_CAVE_PRIORITY = "general.underground generation.caves.type 1 caves.Type 1 Cave Priority";

    public static final String SIMPLEX_CAVE_BOTTOM = "general.underground generation.caves.type 2 caves.Type 2 Cave Minimum Altitude";
    public static final String SIMPLEX_CAVE_TOP = "general.underground generation.caves.type 2 caves.Type 2 Cave Maximum Altitude";
    public static final String SIMPLEX_CAVE_SURFACE_CUTOFF_DEPTH = "general.underground generation.caves.type 2 caves.Type 2 Cave Surface Cutoff Depth";
    public static final String SIMPLEX_CAVE_Y_COMPRESSION = "general.underground generation.caves.type 2 caves.Compression - Vertical";
    public static final String SIMPLEX_CAVE_XZ_COMPRESSION = "general.underground generation.caves.type 2 caves.Compression - Horizontal";
    public static final String SIMPLEX_CAVE_PRIORITY = "general.underground generation.caves.type 2 caves.Type 2 Cave Priority";

    public static final String MOJANG_118_CAVE_BOTTOM = "general.underground generation.caves.1 18-style caves.1.18-Style Cave Minimum Altitude";
    public static final String MOJANG_118_CAVE_TOP = "general.underground generation.caves.1 18-style caves.1.18-Style Cave Maximum Altitude";
    public static final String MOJANG_118_CAVE_SURFACE_CUTOFF_DEPTH = "general.underground generation.caves.1 18-style caves.1.18-Style Cave Surface Cutoff Depth";
    public static final String MOJANG_118_CAVE_PRIORITY = "general.underground generation.caves.1 18-style caves.1.18-Style Cave Priority";
    public static final String MOJANG_118_CAVE_DENSITY_THRESHOLD = "general.underground generation.caves.1 18-style caves.advanced settings.Density Threshold";
    public static final String MOJANG_118_CAVE_HORIZONTAL_SCALE = "general.underground generation.caves.1 18-style caves.advanced settings.Horizontal Scale";
    public static final String MOJANG_118_CAVE_VERTICAL_SCALE = "general.underground generation.caves.1 18-style caves.advanced settings.Vertical Scale";

    public static final String SURFACE_CAVES_ENABLED = "general.underground generation.caves.surface caves.Enable Surface Caves";
    public static final String SURFACE_CAVE_BOTTOM = "general.underground generation.caves.surface caves.Surface Cave Minimum Altitude";
    public static final String SURFACE_CAVE_TOP = "general.underground generation.caves.surface caves.Surface Cave Maximum Altitude";
    public static final String SURFACE_CAVE_DENSITY = "general.underground generation.caves.surface caves.Surface Cave Density";

    public static final String VANILLA_CAVE_BOTTOM = "general.underground generation.caves.vanilla caves.Vanilla Cave Minimum Altitude";
    public static final String VANILLA_CAVE_TOP = "general.underground generation.caves.vanilla caves.Vanilla Cave Maximum Altitude";
    public static final String VANILLA_CAVE_DENSITY = "general.underground generation.caves.vanilla caves.Vanilla Cave Density";
    public static final String VANILLA_CAVE_PRIORITY = "general.underground generation.caves.vanilla caves.Vanilla Cave Priority";

    public static final String CAVERN_REGION_SIZE = "general.underground generation.caverns.Cavern Region Size";
    public static final String CAVERN_REGION_CUSTOM_SIZE = "general.underground generation.caverns.Cavern Region Size Custom Value";
    public static final String CAVERN_SPAWN_CHANCE = "general.underground generation.caverns.Cavern Spawn Chance";

    public static final String LIQUID_CAVERN_BOTTOM = "general.underground generation.caverns.liquid caverns.Liquid Cavern Minimum Altitude";
    public static final String LIQUID_CAVERN_TOP = "general.underground generation.caverns.liquid caverns.Liquid Cavern Maximum Altitude";
    public static final String LIQUID_CAVERN_Y_COMPRESSION = "general.underground generation.caverns.liquid caverns.Compression - Vertical";
    public static final String LIQUID_CAVERN_XZ_COMPRESSION = "general.underground generation.caverns.liquid caverns.Compression - Horizontal";
    public static final String LIQUID_CAVERN_PRIORITY = "general.underground generation.caverns.liquid caverns.Liquid Cavern Priority";

    public static final String FLOORED_CAVERN_BOTTOM = "general.underground generation.caverns.floored caverns.Floored Cavern Minimum Altitude";
    public static final String FLOORED_CAVERN_TOP = "general.underground generation.caverns.floored caverns.Floored Cavern Maximum Altitude";
    public static final String FLOORED_CAVERN_Y_COMPRESSION = "general.underground generation.caverns.floored caverns.Compression - Vertical";
    public static final String FLOORED_CAVERN_XZ_COMPRESSION = "general.underground generation.caverns.floored caverns.Compression - Horizontal";
    public static final String FLOORED_CAVERN_PRIORITY = "general.underground generation.caverns.floored caverns.Floored Cavern Priority";

    public static final String WATER_REGION_SPAWN_CHANCE = "general.underground generation.water regions.Water Region Spawn Chance";
    public static final String WATER_REGION_SIZE = "general.underground generation.water regions.Water Region Size";
    public static final String WATER_REGION_CUSTOM_SIZE = "general.underground generation.water regions.Water Region Size Custom Value";

    public static final String RAVINES_ENABLED = "general.underground generation.ravines.Enable Ravines";
    public static final String FLOODED_RAVINES_ENABLED = "general.underground generation.ravines.Enable Flooded Ravines";

    public static final String LAVA_BLOCK = "general.underground generation.miscellaneous.Lava Block";
    public static final String WATER_BLOCK = "general.underground generation.miscellaneous.Water Block";
    public static final String LIQUID_ALTITUDE = "general.underground generation.miscellaneous.Liquid Altitude";
    public static final String REPLACE_FLOATING_GRAVEL = "general.underground generation.miscellaneous.Prevent Cascading Gravel";
    public static final String OVERRIDE_SURFACE_DETECTION = "general.underground generation.miscellaneous.Override Surface Detection";
    public static final String FLOODED_UNDERGROUND_ENABLED = "general.underground generation.miscellaneous.Enable Flooded Underground";

    public static final String FLATTEN_BEDROCK = "general.bedrock generation.Flatten Bedrock";
    public static final String BEDROCK_WIDTH = "general.bedrock generation.Bedrock Layer Width";
    public static final String DEBUG_VISUALIZER = "general.debug settings.Enable DEBUG Visualizer";

    private ModernCaveTerrainConfigOptions() {}
}
