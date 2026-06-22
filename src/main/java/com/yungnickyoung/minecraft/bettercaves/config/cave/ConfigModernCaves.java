package com.yungnickyoung.minecraft.bettercaves.config.cave;

import net.minecraftforge.common.config.Config;

public class ConfigModernCaves {
    @Config.Name("Enable Modern Caves")
    @Config.Comment(
        "Set to true to enable 1.18+ style noise caves.\n" +
            "This adds cheese caves, noodle caves, spaghetti caves, canyons, and aquifers.\n" +
            "Default: true")
    @Config.RequiresWorldRestart
    public boolean enableModernCaves = true;

    @Config.Name("Modern Cave Minimum Altitude")
    @Config.Comment(
        "The minimum y-coordinate at which modern caves can generate.\n" +
            "Default: 1")
    @Config.RangeInt(min = 0, max = 255)
    @Config.RequiresWorldRestart
    public int caveBottom = 1;

    @Config.Name("Modern Cave Maximum Altitude")
    @Config.Comment(
        "The maximum y-coordinate at which modern caves can generate.\n" +
            "Default: 128")
    @Config.RangeInt(min = 0, max = 255)
    @Config.RequiresWorldRestart
    public int caveTop = 128;

    @Config.Name("Cheese Caves")
    @Config.Comment("Settings for large open 1.18-style cheese cave chambers.")
    public CheeseCaves cheeseCaves = new CheeseCaves();

    @Config.Name("Noodle Caves")
    @Config.Comment("Settings for thin winding 1.18-style noodle cave passages.")
    public NoodleCaves noodleCaves = new NoodleCaves();

    @Config.Name("Spaghetti Caves")
    @Config.Comment("Settings for wider 1.18-style spaghetti cave tunnels.")
    public SpaghettiCaves spaghettiCaves = new SpaghettiCaves();

    @Config.Name("Canyons")
    @Config.Comment("Settings for giant vertical fissure carving.")
    public Canyons canyons = new Canyons();

    @Config.Name("Aquifers")
    @Config.Comment("Settings for local 1.18-style underground water bodies.")
    public Aquifers aquifers = new Aquifers();

    public static class CheeseCaves {
        @Config.Name("Enable Cheese Caves")
        @Config.Comment("Default: true")
        @Config.RequiresWorldRestart
        public boolean enableCheeseCaves = true;

        @Config.Name("Cheese Cave Spawn Chance")
        @Config.Comment(
            "Percent chance for cheese cave chambers to appear in a local region.\n" +
                "Default: 65")
        @Config.RangeDouble(min = 0, max = 100)
        @Config.RequiresWorldRestart
        public float spawnChance = 65;

        @Config.Name("Cheese Cave Size")
        @Config.Comment(
            "Controls the size of cheese cave chambers. Higher values carve larger chambers.\n" +
                "Default: 0.64")
        @Config.RangeDouble(min = 0, max = 1)
        @Config.RequiresWorldRestart
        public float size = .64f;
    }

    public static class NoodleCaves {
        @Config.Name("Enable Noodle Caves")
        @Config.Comment("Default: true")
        @Config.RequiresWorldRestart
        public boolean enableNoodleCaves = true;

        @Config.Name("Noodle Cave Spawn Chance")
        @Config.Comment(
            "Percent chance for noodle cave passages to appear in a local region.\n" +
                "Default: 85")
        @Config.RangeDouble(min = 0, max = 100)
        @Config.RequiresWorldRestart
        public float spawnChance = 85;

        @Config.Name("Noodle Cave Thickness")
        @Config.Comment(
            "Controls noodle passage thickness. Higher values make less claustrophobic tunnels.\n" +
                "Default: 0.055")
        @Config.RangeDouble(min = 0.01, max = 0.2)
        @Config.RequiresWorldRestart
        public float thickness = .055f;
    }

    public static class SpaghettiCaves {
        @Config.Name("Enable Spaghetti Caves")
        @Config.Comment("Default: true")
        @Config.RequiresWorldRestart
        public boolean enableSpaghettiCaves = true;

        @Config.Name("Spaghetti Cave Spawn Chance")
        @Config.Comment(
            "Percent chance for spaghetti cave tunnels to appear in a local region.\n" +
                "Default: 90")
        @Config.RangeDouble(min = 0, max = 100)
        @Config.RequiresWorldRestart
        public float spawnChance = 90;

        @Config.Name("Spaghetti Cave Thickness")
        @Config.Comment(
            "Controls spaghetti tunnel thickness. Higher values make wider tunnels.\n" +
                "Default: 0.12")
        @Config.RangeDouble(min = 0.03, max = 0.35)
        @Config.RequiresWorldRestart
        public float thickness = .12f;
    }

    public static class Canyons {
        @Config.Name("Enable Canyons")
        @Config.Comment("Default: true")
        @Config.RequiresWorldRestart
        public boolean enableCanyons = true;

        @Config.Name("Canyon Spawn Chance")
        @Config.Comment(
            "Percent chance for giant vertical fissures to appear in a local region.\n" +
                "Default: 18")
        @Config.RangeDouble(min = 0, max = 100)
        @Config.RequiresWorldRestart
        public float spawnChance = 18;

        @Config.Name("Canyon Width")
        @Config.Comment(
            "Controls canyon fissure width. Higher values make broader cracks.\n" +
                "Default: 0.052")
        @Config.RangeDouble(min = 0.01, max = 0.2)
        @Config.RequiresWorldRestart
        public float width = .052f;
    }

    public static class Aquifers {
        @Config.Name("Enable Aquifers")
        @Config.Comment(
            "Set to true to use local water levels for modern caves.\n" +
                "Default: true")
        @Config.RequiresWorldRestart
        public boolean enableAquifers = true;

        @Config.Name("Aquifer Minimum Altitude")
        @Config.Comment(
            "The minimum y-coordinate at which aquifers can place water.\n" +
                "Default: 18")
        @Config.RangeInt(min = 0, max = 255)
        @Config.RequiresWorldRestart
        public int aquiferBottom = 18;

        @Config.Name("Aquifer Maximum Altitude")
        @Config.Comment(
            "The maximum y-coordinate at which aquifers can place water.\n" +
                "Default: 62")
        @Config.RangeInt(min = 0, max = 255)
        @Config.RequiresWorldRestart
        public int aquiferTop = 62;

        @Config.Name("Aquifer Water Level Min")
        @Config.Comment(
            "Minimum local water level used by aquifers.\n" +
                "Default: 18")
        @Config.RangeInt(min = 0, max = 255)
        @Config.RequiresWorldRestart
        public int waterLevelMin = 18;

        @Config.Name("Aquifer Water Level Max")
        @Config.Comment(
            "Maximum local water level used by aquifers.\n" +
                "Default: 56")
        @Config.RangeInt(min = 0, max = 255)
        @Config.RequiresWorldRestart
        public int waterLevelMax = 56;

        @Config.Name("Aquifer Water Chance")
        @Config.Comment(
            "Percent chance for local aquifer cells to contain water.\n" +
                "Default: 35")
        @Config.RangeDouble(min = 0, max = 100)
        @Config.RequiresWorldRestart
        public float waterChance = 35;
    }
}
