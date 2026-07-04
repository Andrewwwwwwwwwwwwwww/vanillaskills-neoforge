package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
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
        if (block instanceof CocoaBlock && state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE) {
            return Items.COCOA_BEANS;
        }
        if (block == Blocks.MELON) return Items.MELON_SLICE;   // bonus slices, not whole melons
        if (block == Blocks.PUMPKIN) return Items.PUMPKIN;     // bonus capped via bonusCap()
        if (block == Blocks.SUGAR_CANE) return Items.SUGAR_CANE;
        if (block == Blocks.CACTUS) return Items.CACTUS;
        if (block == Blocks.CHORUS_PLANT) return Items.CHORUS_FRUIT;
        return null;
    }

    /**
     * Max bonus items a single break may grant for this crop. Whole pumpkins are chunky, so they're
     * capped at +2 regardless of Cultivator level; everything else scales freely with level.
     */
    public static int bonusCap(BlockState state) {
        return state.getBlock() == Blocks.PUMPKIN ? 2 : Integer.MAX_VALUE;
    }
}
