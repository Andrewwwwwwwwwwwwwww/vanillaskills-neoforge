package io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/**
 * The "Fortune Upgrade" item is a vanilla ECHO SHARD (thematically fitting — it's found in Ancient
 * City chests) carrying a custom name, our own description, a hidden marker, and a model hook for a
 * resource pack. Using a vanilla item (not a new registered one) keeps the mod fully server-side.
 * (It was previously a netherite upgrade template, but that item showed its own netherite-upgrade
 * description that couldn't be selectively removed.)
 */
public final class FortuneTemplate {
    private FortuneTemplate() {}

    public static final String MARKER_KEY = "vs_fortune_template";

    private static final CompoundTag MARKER = makeMarker();

    private static CompoundTag makeMarker() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(MARKER_KEY, true);
        return tag;
    }

    /** The display name shown on the template. */
    public static Component displayName() {
        return Component.literal("Fortune Upgrade").withStyle(ChatFormatting.AQUA).withStyle(s -> s.withItalic(false));
    }

    /** A fresh marker tag identifying our template. */
    public static CompoundTag markerTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(MARKER_KEY, true);
        return tag;
    }

    /** The base vanilla item the template is built on. */
    public static final net.minecraft.world.item.Item BASE = Items.ECHO_SHARD;

    /** The resource-pack model hook that gives the template its custom texture. */
    public static CustomModelData modelData() {
        return new CustomModelData(List.of(), List.of(), List.of("vanillaskills:fortune_template"), List.of());
    }

    /** The template's description lore. */
    public static ItemLore lore() {
        return new ItemLore(List.of(
                line("Upgrades Fortune books to the", ChatFormatting.GRAY),
                line("next level (up to V).", ChatFormatting.GRAY),
                line("Consumed when used.", ChatFormatting.DARK_GRAY)));
    }

    /** Build a fresh Fortune Upgrade template stack. */
    public static ItemStack create() {
        ItemStack stack = new ItemStack(BASE);
        stack.set(DataComponents.CUSTOM_NAME, displayName());
        CustomData.set(DataComponents.CUSTOM_DATA, stack, markerTag());
        stack.set(DataComponents.CUSTOM_MODEL_DATA, modelData());
        stack.set(DataComponents.LORE, lore());
        return stack;
    }

    private static Component line(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }

    /** True if the stack is our marked Fortune Upgrade template. */
    public static boolean isTemplate(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(BASE)) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.matchedBy(MARKER);
    }
}
