package com.yungnickyoung.minecraft.bettercaves.world.carver.modern;

import com.yungnickyoung.minecraft.bettercaves.noise.OpenSimplex2S;

/**
 * Small 3D noise preset runner inspired by Cave Generator's NoiseSettings.
 */
public class ProgrammableNoiseSampler {
    private final OpenSimplex2S noise;
    private final OpenSimplex2S perturbNoise;
    private final double frequency;
    private final double threshold;
    private final double stretch;
    private final int octaves;
    private final double gain;
    private final double lacunarity;
    private final double perturbAmp;
    private final double perturbFrequency;
    private final boolean invert;

    public ProgrammableNoiseSampler(long seed, double frequency, double threshold, double stretch,
                                    int octaves, double gain, double lacunarity, double perturbAmp,
                                    double perturbFrequency, boolean invert) {
        this.noise = new OpenSimplex2S(seed);
        this.perturbNoise = new OpenSimplex2S(seed + 4049L);
        this.frequency = frequency;
        this.threshold = threshold;
        this.stretch = Math.max(.0001, stretch);
        this.octaves = Math.max(1, octaves);
        this.gain = gain;
        this.lacunarity = lacunarity;
        this.perturbAmp = perturbAmp;
        this.perturbFrequency = perturbFrequency;
        this.invert = invert;
    }

    public double sample(double x, double y, double z) {
        if (perturbAmp != 0.0 && perturbFrequency != 0.0) {
            double px = perturbNoise.noise3_XZBeforeY(x * perturbFrequency, y * perturbFrequency, z * perturbFrequency) * perturbAmp;
            double py = perturbNoise.noise3_XZBeforeY((x + 67.0) * perturbFrequency, (y - 31.0) * perturbFrequency, (z + 19.0) * perturbFrequency) * perturbAmp;
            double pz = perturbNoise.noise3_XZBeforeY((x - 41.0) * perturbFrequency, (y + 17.0) * perturbFrequency, (z + 97.0) * perturbFrequency) * perturbAmp;
            x += px;
            y += py;
            z += pz;
        }

        double nx = x * frequency;
        double ny = (y / stretch) * frequency;
        double nz = z * frequency;
        double amplitude = 1.0;
        double amplitudeSum = 0.0;
        double value = 0.0;

        for (int i = 0; i < octaves; i++) {
            value += noise.noise3_XZBeforeY(nx, ny, nz) * amplitude;
            amplitudeSum += amplitude;
            nx *= lacunarity;
            ny *= lacunarity;
            nz *= lacunarity;
            amplitude *= gain;
        }

        return amplitudeSum == 0.0 ? 0.0 : value / amplitudeSum;
    }

    public boolean carve(double x, double y, double z) {
        boolean within = sample(x, y, z) <= threshold;
        return invert != within;
    }

    public boolean carveBand(double x, double y, double z, double thickness) {
        boolean within = Math.abs(sample(x, y, z)) <= thickness;
        return invert != within;
    }
}
