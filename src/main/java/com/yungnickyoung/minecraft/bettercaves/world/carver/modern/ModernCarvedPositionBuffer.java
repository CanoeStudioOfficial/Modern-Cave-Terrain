package com.yungnickyoung.minecraft.bettercaves.world.carver.modern;

class ModernCarvedPositionBuffer {
    private static final int LOCAL_MASK = 15;
    private static final int Y_MASK = 255;
    private static final int BUFFER_GROWTH = 256;

    private int[] positions = new int[4096];
    private int size;

    void add(int localX, int y, int localZ, int carverIndex) {
        if (size >= positions.length) {
            grow();
        }
        positions[size++] = carverIndex << 16 | localX << 12 | localZ << 8 | y;
    }

    void forEach(PositionConsumer consumer) {
        for (int i = 0; i < size; i++) {
            int packed = positions[i];
            consumer.accept(packed >> 12 & LOCAL_MASK, packed & Y_MASK,
                packed >> 8 & LOCAL_MASK, packed >> 16);
        }
    }

    void reset() {
        size = 0;
    }

    private void grow() {
        int[] grown = new int[positions.length + Math.max(BUFFER_GROWTH, positions.length >> 1)];
        System.arraycopy(positions, 0, grown, 0, positions.length);
        positions = grown;
    }

    interface PositionConsumer {
        void accept(int localX, int y, int localZ, int carverIndex);
    }
}
