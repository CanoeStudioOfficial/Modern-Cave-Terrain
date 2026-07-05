package com.lonelyxiya.minecraft.moderncaveterrain.noise;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Java 8-friendly subset of Mojang's NormalNoise stack used by modern cave density functions.
 *
 * <p>This intentionally avoids the registry and Codec layers from newer Minecraft versions and only keeps the
 * deterministic Xoroshiro positional random, NormalNoise, PerlinNoise and ImprovedNoise pieces needed by the
 * 1.18-style cave sampler.</p>
 */
public class MojangNormalNoise {
    private static final double INPUT_FACTOR = 1.0181268882175227D;

    private final double valueFactor;
    private final PerlinNoise first;
    private final PerlinNoise second;

    public static MojangNormalNoise create(long worldSeed, String noiseName, int firstOctave, double... amplitudes) {
        PositionalRandomFactory factory = new XoroshiroRandomSource(worldSeed).forkPositional();
        RandomSource noiseRandom = factory.fromHashOf(prefixMinecraftNamespace(noiseName));

        return new MojangNormalNoise(noiseRandom, firstOctave, amplitudes);
    }

    private MojangNormalNoise(RandomSource random, int firstOctave, double[] amplitudes) {
        this.first = new PerlinNoise(random, firstOctave, amplitudes);
        this.second = new PerlinNoise(random, firstOctave, amplitudes);

        int minOctave = Integer.MAX_VALUE;
        int maxOctave = Integer.MIN_VALUE;
        for (int i = 0; i < amplitudes.length; i++) {
            if (amplitudes[i] != 0.0D) {
                minOctave = Math.min(minOctave, i);
                maxOctave = Math.max(maxOctave, i);
            }
        }

        this.valueFactor = 0.16666666666666666D / expectedDeviation(maxOctave - minOctave);
    }

    public double getValue(double x, double y, double z) {
        double x2 = x * INPUT_FACTOR;
        double y2 = y * INPUT_FACTOR;
        double z2 = z * INPUT_FACTOR;

        return (this.first.getValue(x, y, z) + this.second.getValue(x2, y2, z2)) * this.valueFactor;
    }

    private static double expectedDeviation(int octaveSpan) {
        return 0.1D * (1.0D + 1.0D / (double) (octaveSpan + 1));
    }

    private static String prefixMinecraftNamespace(String name) {
        return name.indexOf(':') >= 0 ? name : "minecraft:" + name;
    }

    private static class PerlinNoise {
        private static final double WRAP_RANGE = 3.3554432E7D;

        private final ImprovedNoise[] noiseLevels;
        private final double[] amplitudes;
        private final double lowestFreqValueFactor;
        private final double lowestFreqInputFactor;

        private PerlinNoise(RandomSource random, int firstOctave, double[] amplitudes) {
            this.amplitudes = amplitudes.clone();
            this.noiseLevels = new ImprovedNoise[amplitudes.length];

            PositionalRandomFactory positional = random.forkPositional();
            for (int i = 0; i < amplitudes.length; i++) {
                if (amplitudes[i] != 0.0D) {
                    int octave = firstOctave + i;
                    this.noiseLevels[i] = new ImprovedNoise(positional.fromHashOf("octave_" + octave));
                }
            }

            int zeroOctaveIndex = -firstOctave;
            this.lowestFreqInputFactor = Math.pow(2.0D, (double) (-zeroOctaveIndex));
            this.lowestFreqValueFactor = Math.pow(2.0D, (double) (amplitudes.length - 1))
                    / (Math.pow(2.0D, (double) amplitudes.length) - 1.0D);
        }

        private double getValue(double x, double y, double z) {
            double value = 0.0D;
            double inputFactor = this.lowestFreqInputFactor;
            double valueFactor = this.lowestFreqValueFactor;

            for (int i = 0; i < this.noiseLevels.length; i++) {
                ImprovedNoise noise = this.noiseLevels[i];
                if (noise != null) {
                    value += this.amplitudes[i] * noise.noise(
                            wrap(x * inputFactor),
                            wrap(y * inputFactor),
                            wrap(z * inputFactor)
                    ) * valueFactor;
                }

                inputFactor *= 2.0D;
                valueFactor /= 2.0D;
            }

            return value;
        }

        private static double wrap(double value) {
            return value - Math.floor(value / WRAP_RANGE + 0.5D) * WRAP_RANGE;
        }
    }

    private static class ImprovedNoise {
        private static final int[][] GRADIENT = new int[][] {
                {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
                {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
                {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1},
                {1, 1, 0}, {0, -1, 1}, {-1, 1, 0}, {0, -1, -1}
        };

        private final byte[] permutation = new byte[256];
        private final double xo;
        private final double yo;
        private final double zo;

        private ImprovedNoise(RandomSource random) {
            this.xo = random.nextDouble() * 256.0D;
            this.yo = random.nextDouble() * 256.0D;
            this.zo = random.nextDouble() * 256.0D;

            for (int i = 0; i < 256; i++) {
                this.permutation[i] = (byte) i;
            }

            for (int i = 0; i < 256; i++) {
                int offset = random.nextInt(256 - i);
                byte tmp = this.permutation[i];
                this.permutation[i] = this.permutation[i + offset];
                this.permutation[i + offset] = tmp;
            }
        }

        private double noise(double x, double y, double z) {
            double shiftedX = x + this.xo;
            double shiftedY = y + this.yo;
            double shiftedZ = z + this.zo;
            int floorX = floor(shiftedX);
            int floorY = floor(shiftedY);
            int floorZ = floor(shiftedZ);
            double localX = shiftedX - (double) floorX;
            double localY = shiftedY - (double) floorY;
            double localZ = shiftedZ - (double) floorZ;

            return this.sampleAndLerp(floorX, floorY, floorZ, localX, localY, localZ);
        }

        private double sampleAndLerp(int x, int y, int z, double localX, double localY, double localZ) {
            int px0 = this.permutation(x);
            int px1 = this.permutation(x + 1);
            int pxy00 = this.permutation(px0 + y);
            int pxy01 = this.permutation(px0 + y + 1);
            int pxy10 = this.permutation(px1 + y);
            int pxy11 = this.permutation(px1 + y + 1);
            double d000 = gradDot(this.permutation(pxy00 + z), localX, localY, localZ);
            double d100 = gradDot(this.permutation(pxy10 + z), localX - 1.0D, localY, localZ);
            double d010 = gradDot(this.permutation(pxy01 + z), localX, localY - 1.0D, localZ);
            double d110 = gradDot(this.permutation(pxy11 + z), localX - 1.0D, localY - 1.0D, localZ);
            double d001 = gradDot(this.permutation(pxy00 + z + 1), localX, localY, localZ - 1.0D);
            double d101 = gradDot(this.permutation(pxy10 + z + 1), localX - 1.0D, localY, localZ - 1.0D);
            double d011 = gradDot(this.permutation(pxy01 + z + 1), localX, localY - 1.0D, localZ - 1.0D);
            double d111 = gradDot(this.permutation(pxy11 + z + 1), localX - 1.0D, localY - 1.0D, localZ - 1.0D);
            double xAlpha = smoothstep(localX);
            double yAlpha = smoothstep(localY);
            double zAlpha = smoothstep(localZ);

            return lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
        }

        private int permutation(int value) {
            return this.permutation[value & 255] & 255;
        }

        private static double gradDot(int hash, double x, double y, double z) {
            int[] gradient = GRADIENT[hash & 15];
            return (double) gradient[0] * x + (double) gradient[1] * y + (double) gradient[2] * z;
        }
    }

    private interface RandomSource {
        int nextInt();

        int nextInt(int bound);

        long nextLong();

        double nextDouble();

        PositionalRandomFactory forkPositional();
    }

    private interface PositionalRandomFactory {
        RandomSource fromHashOf(String name);
    }

    private static class XoroshiroRandomSource implements RandomSource {
        private static final double DOUBLE_UNIT = 1.1102230246251565E-16D;

        private Xoroshiro128PlusPlus random;

        private XoroshiroRandomSource(long seed) {
            this.random = new Xoroshiro128PlusPlus(upgradeSeedTo128bit(seed));
        }

        private XoroshiroRandomSource(Seed128bit seed) {
            this.random = new Xoroshiro128PlusPlus(seed);
        }

        private XoroshiroRandomSource(long seedLo, long seedHi) {
            this.random = new Xoroshiro128PlusPlus(seedLo, seedHi);
        }

        public int nextInt() {
            return (int) this.random.nextLong();
        }

        public int nextInt(int bound) {
            if (bound <= 0) {
                throw new IllegalArgumentException("Bound must be positive");
            }

            long randomBits = Integer.toUnsignedLong(this.nextInt());
            long multipliedRandomBits = randomBits * (long) bound;
            long fractionalPart = multipliedRandomBits & 4294967295L;

            if (fractionalPart < (long) bound) {
                int unbiasedBucketsStartIndex = Integer.remainderUnsigned(~bound + 1, bound);
                while (fractionalPart < (long) unbiasedBucketsStartIndex) {
                    randomBits = Integer.toUnsignedLong(this.nextInt());
                    multipliedRandomBits = randomBits * (long) bound;
                    fractionalPart = multipliedRandomBits & 4294967295L;
                }
            }

            return (int) (multipliedRandomBits >> 32);
        }

        public long nextLong() {
            return this.random.nextLong();
        }

        public double nextDouble() {
            return (double) this.nextBits(53) * DOUBLE_UNIT;
        }

        public PositionalRandomFactory forkPositional() {
            return new XoroshiroPositionalRandomFactory(this.random.nextLong(), this.random.nextLong());
        }

        private long nextBits(int bits) {
            return this.random.nextLong() >>> (64 - bits);
        }
    }

    private static class XoroshiroPositionalRandomFactory implements PositionalRandomFactory {
        private final long seedLo;
        private final long seedHi;

        private XoroshiroPositionalRandomFactory(long seedLo, long seedHi) {
            this.seedLo = seedLo;
            this.seedHi = seedHi;
        }

        public RandomSource fromHashOf(String name) {
            Seed128bit seed = seedFromHashOf(name);
            return new XoroshiroRandomSource(seed.seedLo ^ this.seedLo, seed.seedHi ^ this.seedHi);
        }
    }

    private static class Xoroshiro128PlusPlus {
        private long seedLo;
        private long seedHi;

        private Xoroshiro128PlusPlus(Seed128bit seed) {
            this(seed.seedLo, seed.seedHi);
        }

        private Xoroshiro128PlusPlus(long seedLo, long seedHi) {
            this.seedLo = seedLo;
            this.seedHi = seedHi;
            if ((this.seedLo | this.seedHi) == 0L) {
                this.seedLo = -7046029254386353131L;
                this.seedHi = 7640891576956012809L;
            }
        }

        private long nextLong() {
            long s0 = this.seedLo;
            long s1 = this.seedHi;
            long result = Long.rotateLeft(s0 + s1, 17) + s0;

            s1 ^= s0;
            this.seedLo = Long.rotateLeft(s0, 49) ^ s1 ^ (s1 << 21);
            this.seedHi = Long.rotateLeft(s1, 28);

            return result;
        }
    }

    private static class Seed128bit {
        private final long seedLo;
        private final long seedHi;

        private Seed128bit(long seedLo, long seedHi) {
            this.seedLo = seedLo;
            this.seedHi = seedHi;
        }

        private Seed128bit mixed() {
            return new Seed128bit(mixStafford13(this.seedLo), mixStafford13(this.seedHi));
        }
    }

    private static Seed128bit upgradeSeedTo128bit(long seed) {
        long lowBits = seed ^ 7640891576956012809L;
        long highBits = lowBits + -7046029254386353131L;

        return new Seed128bit(lowBits, highBits).mixed();
    }

    private static Seed128bit seedFromHashOf(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            return new Seed128bit(longFromBytes(hash, 0), longFromBytes(hash, 8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 digest is required for Mojang noise initialization", e);
        }
    }

    private static long longFromBytes(byte[] bytes, int offset) {
        return ((long) bytes[offset] & 255L) << 56
                | ((long) bytes[offset + 1] & 255L) << 48
                | ((long) bytes[offset + 2] & 255L) << 40
                | ((long) bytes[offset + 3] & 255L) << 32
                | ((long) bytes[offset + 4] & 255L) << 24
                | ((long) bytes[offset + 5] & 255L) << 16
                | ((long) bytes[offset + 6] & 255L) << 8
                | ((long) bytes[offset + 7] & 255L);
    }

    private static long mixStafford13(long value) {
        value = (value ^ value >>> 30) * -4658895280553007687L;
        value = (value ^ value >>> 27) * -7723592293110705685L;
        return value ^ value >>> 31;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static double smoothstep(double value) {
        return value * value * value * (value * (value * 6.0D - 15.0D) + 10.0D);
    }

    private static double lerp(double alpha, double from, double to) {
        return from + alpha * (to - from);
    }

    private static double lerp2(double alpha1, double alpha2, double x00, double x10, double x01, double x11) {
        return lerp(alpha2, lerp(alpha1, x00, x10), lerp(alpha1, x01, x11));
    }

    private static double lerp3(double alpha1, double alpha2, double alpha3,
                                double x000, double x100, double x010, double x110,
                                double x001, double x101, double x011, double x111) {
        return lerp(alpha3,
                lerp2(alpha1, alpha2, x000, x100, x010, x110),
                lerp2(alpha1, alpha2, x001, x101, x011, x111));
    }
}
