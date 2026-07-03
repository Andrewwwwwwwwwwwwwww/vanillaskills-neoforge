package io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/** The player's current attribute totals, shown as icons whose tooltip is the value. */
public final class StatsScreen {
    private StatsScreen() {}

    public static void open(ServerPlayer player) {
        List<ItemStack> items = new ArrayList<>();

        // Currencies & progression first.
        var data = VanillaSkills.PLAYERS.get(player.getUUID());
        int total = VanillaSkills.PLAYERS.totalEarnable();
        items.add(info(Items.EXPERIENCE_BOTTLE, "Skill Shards", ChatFormatting.AQUA, List.of(
                data.pointsAvailable + " available",
                "earned " + data.pointsEarned + (total > 0 ? " of ~" + total + " possible" : ""))));
        items.add(info(Items.AMETHYST_SHARD, "Quest Shards", ChatFormatting.LIGHT_PURPLE, List.of(
                data.questShardsAvailable + " available",
                "spend them at the bounty board shop")));

        // Aquatic lane summary (what the player has unlocked in that lane).
        items.add(aquaticSummary(player));

        // Attribute totals.
        add(items, player, Attributes.MAX_HEALTH, Items.APPLE, "Max Health");
        add(items, player, Attributes.ARMOR, Items.IRON_CHESTPLATE, "Armor");
        add(items, player, Attributes.ARMOR_TOUGHNESS, Items.DIAMOND_CHESTPLATE, "Armor Toughness");
        add(items, player, Attributes.KNOCKBACK_RESISTANCE, Items.SHIELD, "Knockback Resistance");
        add(items, player, Attributes.ATTACK_DAMAGE, Items.IRON_SWORD, "Attack Damage");
        add(items, player, Attributes.ATTACK_SPEED, Items.CLOCK, "Attack Speed");
        add(items, player, Attributes.MOVEMENT_SPEED, Items.FEATHER, "Movement Speed");
        add(items, player, Attributes.MINING_EFFICIENCY, Items.IRON_PICKAXE, "Mining Efficiency");
        add(items, player, Attributes.LUCK, Items.RABBIT_FOOT, "Luck");
        InfoMenu.open(player, styled("Your Stats", ChatFormatting.AQUA), 4, items);
    }

    /** Summarises how much of the Aquatic lane the player has unlocked: breaths, swim speed, mine speed. */
    private static ItemStack aquaticSummary(ServerPlayer player) {
        var tree = VanillaSkills.TREE.tree();
        var data = VanillaSkills.PLAYERS.get(player.getUUID());
        double breaths = 0, swim = 0, mine = 0;
        for (io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillNode node : tree.nodesIn("aquatic")) {
            if (!data.hasUnlocked(node.id)) continue;
            for (io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillEffect e : node.effects) {
                if (!"attribute".equals(e.type) || e.attribute == null) continue;
                switch (e.attribute) {
                    case "minecraft:oxygen_bonus" -> breaths += e.amount;
                    case "minecraft:water_movement_efficiency" -> swim += e.amount;
                    case "minecraft:submerged_mining_speed" -> mine += e.amount;
                }
            }
        }
        return info(Items.HEART_OF_THE_SEA, "Aquatic", ChatFormatting.AQUA, List.of(
                "Breaths: +" + (int) Math.round(breaths),
                "Swim Speed: +" + Math.round(swim * 100) + "%",
                "Mine Speed: +" + Math.round(mine * 100) + "%"));
    }

    private static ItemStack info(Item icon, String label, ChatFormatting color, List<String> lore) {
        ItemStack stack = new ItemStack(icon);
        Guis.hideStats(stack);
        stack.set(DataComponents.CUSTOM_NAME, styled(label, color));
        List<Component> lines = new ArrayList<>();
        for (String l : lore) lines.add(styled(l, ChatFormatting.GRAY));
        stack.set(DataComponents.LORE, new ItemLore(lines));
        return stack;
    }

    private static void add(List<ItemStack> items, ServerPlayer player, Holder<Attribute> attribute, Item icon, String label) {
        ItemStack stack = new ItemStack(icon);
        Guis.hideStats(stack);
        stack.set(DataComponents.CUSTOM_NAME, styled(label, ChatFormatting.AQUA));
        double value = player.getAttributeValue(attribute);
        stack.set(DataComponents.LORE, new ItemLore(List.of(styled(format(value), ChatFormatting.WHITE))));
        items.add(stack);
    }

    private static String format(double value) {
        return Math.abs(value - Math.rint(value)) < 1e-6 ? String.valueOf((long) value) : String.format("%.2f", value);
    }

    private static Component styled(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }
}
