package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

/**
 * Dragon Ingot: forged by surrounding a Netherite Ingot with eight Dragon Scales. Used (with the
 * Dragon Upgrade Template) to upgrade netherite armor into Dragon armor. A marked netherite ingot,
 * so it stays server-side / vanilla-client safe.
 */
public final class DragonIngot {
    private DragonIngot() {}

    public static final String MARKER = "vs_dragon_ingot";
    private static final int COLOR = 0xC23BD6;

    public static ItemStack create() {
        ItemStack stack = new ItemStack(Items.NETHERITE_INGOT);
        stack.set(DataComponents.CUSTOM_NAME, Markers.name("Dragon Ingot", COLOR));
        Markers.applyMarker(stack, MARKER);
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(), List.of("vanillaskills:dragon_ingot"), List.of()));
        return stack;
    }

    public static boolean isDragonIngot(ItemStack stack) {
        return stack.is(Items.NETHERITE_INGOT) && Markers.has(stack, MARKER);
    }
}
