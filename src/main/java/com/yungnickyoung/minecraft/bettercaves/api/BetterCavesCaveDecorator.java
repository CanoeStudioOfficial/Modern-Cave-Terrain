package com.yungnickyoung.minecraft.bettercaves.api;

/**
 * Chunk-level decoration hook invoked after Better Caves finishes carving.
 */
public interface BetterCavesCaveDecorator {
    void decorate(BetterCavesCaveDecorationContext context);
}
