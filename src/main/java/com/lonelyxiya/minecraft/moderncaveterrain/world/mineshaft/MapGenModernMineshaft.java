package com.lonelyxiya.minecraft.moderncaveterrain.world.mineshaft;

import com.lonelyxiya.minecraft.moderncaveterrain.config.util.ConfigHolder;
import com.lonelyxiya.minecraft.moderncaveterrain.config.io.ConfigLoader;
import com.lonelyxiya.minecraft.moderncaveterrain.util.ModernCaveTerrainUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeMesa;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.structure.*;
import net.minecraftforge.event.terraingen.InitMapGenEvent;


import java.util.Random;

/**
 * Overrides mineshaft generation to remove pieces of mineshafts within 4 blocks of the liquid altitude.
 * This prevents mineshafts from being at risk of burning from lava at the liquid altitude.
 *
 * Replaces vanilla mineshaft generation where Modern Cave Terrain is active.
 */
public class MapGenModernMineshaft extends MapGenMineshaft {
    static {
        MapGenStructureIO.registerStructure(StructureBetterMineshaftStart.class, "Mineshaft");
    }

    private MapGenBase defaultMineshaftGen;
    private int liquidAltitude;

    public MapGenModernMineshaft(InitMapGenEvent event) {
        this.defaultMineshaftGen = event.getOriginalGen();
    }

    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        Biome biome = this.world.getBiome(new BlockPos((chunkX << 4) + 8, 64, (chunkZ << 4) + 8));
        MapGenMineshaft.Type mapgenmineshaft$type = biome instanceof BiomeMesa ? MapGenMineshaft.Type.MESA : MapGenMineshaft.Type.NORMAL;
        return new StructureBetterMineshaftStart(this.world, this.rand, chunkX, chunkZ, mapgenmineshaft$type, liquidAltitude);
    }


    @Override
    public void generate(World worldIn, int x, int z, ChunkPrimer primer) {
        // Only operate on whitelisted dimensions.
        if (!ModernCaveTerrainUtils.isDimensionWhitelisted(worldIn.provider.getDimension())) {
            defaultMineshaftGen.generate(worldIn, x, z, primer);
            return;
        }

        if (world == null) { // First call - lazy initialization
            this.initialize(worldIn);
        }

        super.generate(worldIn, x, z, primer);
    }

    private void initialize(World worldIn) {
        this.world = worldIn;
        // Load config for this dimension
        ConfigHolder config = ConfigLoader.loadConfigFromFileForDimension(worldIn.provider.getDimension());
        this.liquidAltitude = config.liquidAltitude.get();
    }

    private static class StructureBetterMineshaftStart extends StructureMineshaftStart {
        private final int liquidAltitude;

        public StructureBetterMineshaftStart(World worldIn, Random rand, int chunkX, int chunkZ, MapGenMineshaft.Type type, int liquidAltitude) {
            super(worldIn, rand, chunkX, chunkZ, type);
            this.liquidAltitude = liquidAltitude;
        }

        @Override
        public void generateStructure(World worldIn, Random rand, StructureBoundingBox structurebb) {
            components.removeIf(component ->
                component.getBoundingBox().minY < liquidAltitude + 5 ||
                        (component.getBoundingBox().intersectsWith(structurebb) && !component.addComponentParts(worldIn, rand, structurebb))
            );
        }
    }
}
