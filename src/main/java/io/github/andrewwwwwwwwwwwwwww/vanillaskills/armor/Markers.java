package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Shared helpers for "marked" items: vanilla items stamped with a hidden custom_data flag so
 * we can identify our custom tiers/materials without registering new items (keeps it
 * server-side / vanilla-client safe).
 */
public final class Markers {
    private Markers() {}

    public static CompoundTag markerTag(String key) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(key, true);
        return tag;
    }

    public static void applyMarker(ItemStack stack, String key) {
        CustomData.set(DataComponents.CUSTOM_DATA, stack, markerTag(key));
    }

    public static boolean has(ItemStack stack, String key) {
        if (stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.matchedBy(markerTag(key));
    }

    /** True if the stack carries any of our markers (custom_data key starting with "vs_"). */
    public static boolean isOurs(ItemStack stack) {
        if (stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        for (String key : data.copyTag().keySet()) {
            if (key.startsWith("vs_")) return true;
        }
        return false;
    }

    /** A non-italic colored display name (rgb is 0xRRGGBB). */
    public static Component name(String text, int rgb) {
        return Component.literal(text).withStyle(s -> s.withColor(rgb).withItalic(false));
    }
}
