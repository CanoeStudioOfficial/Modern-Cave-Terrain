package com.yungnickyoung.minecraft.bettercaves.config.cave;

import net.minecraftforge.common.config.Config;

public class ConfigModernCaves {
    @Config.Name("Enable Modern Caves")
    @Config.Comment(
        "Set to true to enable 1.18+ style noise caves.\n" +
            "This adds cheese caves, noodle caves, spaghetti caves, and canyons.\n" +
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

        @Config.Name("Cheese Cave Noise Frequency")
        @Config.Comment(
            "CaveGenerator-style 3D noise frequency for cheese cave chambers.\n" +
                "Default: 0.02")
        @Config.RangeDouble(min = 0.001, max = 0.2)
        @Config.RequiresWorldRestart
        public float noiseFrequency = .02f;

        @Config.Name("Cheese Cave Noise Threshold")
        @Config.Comment(
            "Base threshold for cheese cave noise. Lower values make fewer chambers.\n" +
                "Default: -0.5")
        @Config.RangeDouble(min = -1, max = 1)
        @Config.RequiresWorldRestart
        public float noiseThreshold = -.5f;

        @Config.Name("Cheese Cave Vertical Stretch")
        @Config.Comment(
            "Vertical stretch for cheese cave noise. Values below 1 make taller open rooms.\n" +
                "Default: 0.5")
        @Config.RangeDouble(min = 0.1, max = 4)
        @Config.RequiresWorldRestart
        public float verticalStretch = .5f;

        @Config.Name("Cheese Cave Perturb Amp")
        @Config.Comment(
            "Coordinate warp strength for cheese cave noise.\n" +
                "Default: 10")
        @Config.RangeDouble(min = 0, max = 32)
        @Config.RequiresWorldRestart
        public float perturbAmp = 10f;

        @Config.Name("Cheese Cave Perturb Frequency")
        @Config.Comment(
            "Coordinate warp frequency for cheese cave noise.\n" +
                "Default: 0.025")
        @Config.RangeDouble(min = 0, max = 0.2)
        @Config.RequiresWorldRestart
        public float perturbFrequency = .025f;

        @Config.Name("Cheese Cave Detail Frequency")
        @Config.Comment(
            "Secondary detail noise frequency for breaking up cheese cave walls.\n" +
                "Default: 0.045")
        @Config.RangeDouble(min = 0.001, max = 0.3)
        @Config.RequiresWorldRestart
        public float detailFrequency = .045f;

        @Config.Name("Cheese Cave Detail Weight")
        @Config.Comment(
            "How much secondary noise roughens cheese cave chambers.\n" +
                "Default: 0.22")
        @Config.RangeDouble(min = 0, max = 1)
        @Config.RequiresWorldRestart
        public float detailWeight = .22f;
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

        @Config.Name("Noodle Cave Noise Frequency")
        @Config.Comment(
            "CaveGenerator-style 3D noise frequency for noodle cave passages.\n" +
                "Default: 0.086")
        @Config.RangeDouble(min = 0.001, max = 0.3)
        @Config.RequiresWorldRestart
        public float noiseFrequency = .086f;

        @Config.Name("Noodle Cave Vertical Stretch")
        @Config.Comment(
            "Vertical stretch for noodle cave noise.\n" +
                "Default: 1.0")
        @Config.RangeDouble(min = 0.1, max = 4)
        @Config.RequiresWorldRestart
        public float verticalStretch = 1f;

        @Config.Name("Noodle Cave Perturb Amp")
        @Config.Comment(
            "Coordinate warp strength for noodle cave noise.\n" +
                "Default: 1.5")
        @Config.RangeDouble(min = 0, max = 16)
        @Config.RequiresWorldRestart
        public float perturbAmp = 1.5f;

        @Config.Name("Noodle Cave Perturb Frequency")
        @Config.Comment(
            "Coordinate warp frequency for noodle cave noise.\n" +
                "Default: 0.035")
        @Config.RangeDouble(min = 0, max = 0.2)
        @Config.RequiresWorldRestart
        public float perturbFrequency = .035f;

        @Config.Name("Noodle Cave Toggle Frequency")
        @Config.Comment(
            "Low-frequency noise gate that breaks noodle caves into separate runs.\n" +
                "Default: 0.026")
        @Config.RangeDouble(min = 0.001, max = 0.2)
        @Config.RequiresWorldRestart
        public float toggleFrequency = .026f;

        @Config.Name("Noodle Cave Toggle Threshold")
        @Config.Comment(
            "Threshold for the noodle cave gate. Higher values make fewer continuous passages.\n" +
                "Default: -0.18")
        @Config.RangeDouble(min = -1, max = 1)
        @Config.RequiresWorldRestart
        public float toggleThreshold = -.18f;
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

        @Config.Name("Spaghetti Cave Noise Frequency")
        @Config.Comment(
            "CaveGenerator-style 3D noise frequency for spaghetti cave tunnels.\n" +
                "Default: 0.038")
        @Config.RangeDouble(min = 0.001, max = 0.3)
        @Config.RequiresWorldRestart
        public float noiseFrequency = .038f;

        @Config.Name("Spaghetti Cave Vertical Stretch")
        @Config.Comment(
            "Vertical stretch for spaghetti cave noise.\n" +
                "Default: 0.75")
        @Config.RangeDouble(min = 0.1, max = 4)
        @Config.RequiresWorldRestart
        public float verticalStretch = .75f;

        @Config.Name("Spaghetti Cave Perturb Amp")
        @Config.Comment(
            "Coordinate warp strength for spaghetti cave noise.\n" +
                "Default: 4")
        @Config.RangeDouble(min = 0, max = 24)
        @Config.RequiresWorldRestart
        public float perturbAmp = 4f;

        @Config.Name("Spaghetti Cave Perturb Frequency")
        @Config.Comment(
            "Coordinate warp frequency for spaghetti cave noise.\n" +
                "Default: 0.025")
        @Config.RangeDouble(min = 0, max = 0.2)
        @Config.RequiresWorldRestart
        public float perturbFrequency = .025f;

        @Config.Name("Spaghetti Cave Roughness Frequency")
        @Config.Comment(
            "Frequency of thickness variation along spaghetti tunnels.\n" +
                "Default: 0.075")
        @Config.RangeDouble(min = 0.001, max = 0.3)
        @Config.RequiresWorldRestart
        public float roughnessFrequency = .075f;
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

}
