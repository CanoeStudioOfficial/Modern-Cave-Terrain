package com.lonelyxiya.minecraft.moderncaveterrain.config.cave;

import net.minecraftforge.common.config.Config;

public class ConfigMojang118Cave {
    @Config.Name("1.18-Style Cave Minimum Altitude")
    @Config.Comment(
            "The minimum y-coordinate at which 1.18-style density caves can generate.\n" +
            "Default: 1")
    @Config.RangeInt(min = 0, max = 255)
    @Config.RequiresWorldRestart
    public int caveBottom = 1;

    @Config.Name("1.18-Style Cave Maximum Altitude")
    @Config.Comment(
            "The maximum y-coordinate at which 1.18-style density caves can generate.\n" +
            "Default: 255")
    @Config.RangeInt(min = 0, max = 255)
    @Config.RequiresWorldRestart
    public int caveTop = 255;

    @Config.Name("1.18-Style Cave Surface Cutoff Depth")
    @Config.Comment(
            "The depth from a given point on the surface at which 1.18-style caves start to close off.\n" +
            "Default: 8")
    @Config.RangeInt(min = 0, max = 255)
    @Config.RequiresWorldRestart
    public int caveSurfaceCutoff = 8;

    @Config.Name("1.18-Style Cave Priority")
    @Config.Comment(
            "Determines how frequently 1.18-style density caves spawn. 0 = will not spawn at all.\n" +
            "Default: 10")
    @Config.RangeInt(min = 0, max = 10)
    @Config.RequiresWorldRestart
    public int cavePriority = 10;

    @Config.Ignore
    @Config.Name("Advanced Settings")
    @Config.Comment("Don't mess with these if you don't know what you're doing.")
    public Advanced advancedSettings = new Advanced();

    public class Advanced {
        @Config.Name("Density Threshold")
        @Config.Comment(
                "Density threshold for determining which blocks get mined out as part of 1.18-style cave generation.\n" +
                "    Blocks with generated density less than or equal to this threshold will be dug out.\n" +
                "Default: 0.0")
        @Config.RangeDouble(min = -1.0, max = 1.0)
        @Config.RequiresWorldRestart
        public float densityThreshold = 0.0f;

        @Config.Name("Horizontal Scale")
        @Config.Comment(
                "Scales 1.18-style cave noise horizontally. Higher values make cave features tighter.\n" +
                "Default: 1.0")
        @Config.RangeDouble(min = 0.25, max = 4.0)
        @Config.RequiresWorldRestart
        public float horizontalScale = 1.0f;

        @Config.Name("Vertical Scale")
        @Config.Comment(
                "Scales 1.18-style cave noise vertically. Higher values make cave features shorter.\n" +
                "Default: 1.0")
        @Config.RangeDouble(min = 0.25, max = 4.0)
        @Config.RequiresWorldRestart
        public float verticalScale = 1.0f;
    }
}
