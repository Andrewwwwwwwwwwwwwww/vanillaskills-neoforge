package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cultivator skill helper: maps a fully-grown harvestable crop to the product item that should be
 * dropped as a bonus. Returns null for immature crops or non-crops.
 */
public final class Farming {
    private Farming() {}

    public static Item matureCropProduct(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            if (!crop.isMaxAge(state)) return null;
            if (block == Blocks.WHEAT) return Items.WHEAT;
            if (block == Blocks.CARROTS) return Items.CARROT;
            if (block == Blocks.POTATOES) return Items.POTATO;
            if (block == Blocks.BEETROOTS) return Items.BEETROOT;
            return null;
        }
        if (block == Blocks.NETHER_WART && state.getValue(NetherWartBlock.AGE) >= 3) {
            return Items.NETHER_WART;
        }
        return null;
    }
}
