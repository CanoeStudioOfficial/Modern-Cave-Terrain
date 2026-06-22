package com.yungnickyoung.minecraft.bettercaves.api;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BetterCavesAPI {
    private static final List<ModernCaveCarvingCallback> MODERN_CAVE_CARVING_CALLBACKS =
        new CopyOnWriteArrayList<>();

    private BetterCavesAPI() {
    }

    /**
     * Registers a callback for modern cave carving. This can be called from another mod's init code
     * with a lambda or method reference.
     */
    public static void registerModernCaveCarvingCallback(ModernCaveCarvingCallback callback) {
        MODERN_CAVE_CARVING_CALLBACKS.add(Objects.requireNonNull(callback, "callback"));
    }

    public static boolean unregisterModernCaveCarvingCallback(ModernCaveCarvingCallback callback) {
        return MODERN_CAVE_CARVING_CALLBACKS.remove(callback);
    }

    public static List<ModernCaveCarvingCallback> getModernCaveCarvingCallbacks() {
        return Collections.unmodifiableList(MODERN_CAVE_CARVING_CALLBACKS);
    }

    public static boolean hasModernCaveCarvingCallbacks() {
        return !MODERN_CAVE_CARVING_CALLBACKS.isEmpty();
    }

    public static IBlockState resolveModernCaveCarvedBlock(World world, BlockPos pos, IBlockState originalState,
                                                           IBlockState proposedState,
                                                           ModernCaveCarvingType carvingType) {
        IBlockState resolvedState = proposedState;
        for (ModernCaveCarvingCallback callback : MODERN_CAVE_CARVING_CALLBACKS) {
            IBlockState callbackState = callback.getCarvedBlock(world, pos, originalState, resolvedState, carvingType);
            if (callbackState != null) {
                resolvedState = callbackState;
            }
        }
        return resolvedState;
    }
}
