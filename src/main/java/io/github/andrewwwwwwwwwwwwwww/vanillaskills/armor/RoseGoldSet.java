package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * Rose Gold set behaviour: immunity to all negative status effects, but ONLY while the full set is
 * worn (checked live, so removing any piece reverts it immediately). The worn/stored set-bonus
 * tooltip is handled generically by {@link ArmorSetTooltips}.
 */
public final class RoseGoldSet {
    private RoseGoldSet() {}

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isFullSet(player)) stripHarmfulEffects(player);
        }
    }

    /** True only while all four Rose Gold pieces are worn (checked live, so it reverts instantly). */
    public static boolean isFullSet(net.minecraft.world.entity.LivingEntity entity) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!ArmorTiers.ROSE_GOLD.isWorn(entity.getItemBySlot(slot))) return false;
        }
        return true;
    }

    /** Static description shown on a crafted / stored Rose Gold piece. */
    public static ItemLore baseLore() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(""));
        lines.add(trStyled("vanillaskills.set.rose_gold.name_plain", "Rose Gold Set", ChatFormatting.AQUA));
        lines.add(trStyled("vanillaskills.set.rose_gold.desc1", "Full set: immune to all", ChatFormatting.GRAY));
        lines.add(trStyled("vanillaskills.set.rose_gold.desc2", "negative status effects", ChatFormatting.GRAY));
        return new ItemLore(lines);
    }

    private static void stripHarmfulEffects(ServerPlayer player) {
        List<Holder<MobEffect>> harmful = null;
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                if (harmful == null) harmful = new ArrayList<>();
                harmful.add(instance.getEffect());
            }
        }
        if (harmful != null) {
            for (Holder<MobEffect> effect : harmful) player.removeEffect(effect);
        }
    }

    private static Component styled(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }

    /** Translatable, non-italic lore line — item lore is baked in, so the client resolves the key. */
    private static Component trStyled(String key, String fallback, ChatFormatting color) {
        return Component.translatableWithFallback(key, fallback)
                .withStyle(color).withStyle(s -> s.withItalic(false));
    }
}
