package com.lonelyxiya.minecraft.moderncaveterrain.event;

import com.lonelyxiya.minecraft.moderncaveterrain.api.ModernCaveTerrainAPI;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Replaces vanilla cave generation with Modern Cave Terrain generation.
 * Should be registered to the {@code TERRAIN_GEN_BUS}.
 */
public class EventModernCaveTerrainGen {
    /**
     * Replaces cave gen and mineshaft gen
     *
     * @param event Map generation event
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onInitMapGenEvent(InitMapGenEvent event) {
        // Replace cave gen with Modern Cave Terrain
        if (
            (event.getType() == InitMapGenEvent.EventType.CAVE || event.getType() == InitMapGenEvent.EventType.NETHER_CAVE)
                && !ModernCaveTerrainAPI.isCaveGenerator(event.getOriginalGen())
        ) {
            event.setNewGen(ModernCaveTerrainAPI.createCaveGenerator(event));
        }
        // Replace mineshaft gen with Modern Cave Terrain
        else if (
            event.getType() == InitMapGenEvent.EventType.MINESHAFT
                && event.getOriginalGen() == event.getNewGen() // only modify vanilla gen to allow other mods to modify mineshafts
        ) {
            event.setNewGen(ModernCaveTerrainAPI.createMineshaftGenerator(event));
        }
        // Replace ravine gen with Modern Cave Terrain
        else if (
            event.getType() == InitMapGenEvent.EventType.RAVINE
                && event.getOriginalGen() == event.getNewGen() // only modify vanilla gen to allow other mods to modify ravines
        ) {
            event.setNewGen(ModernCaveTerrainAPI.createRavineGenerator(event));
        }
    }
}
