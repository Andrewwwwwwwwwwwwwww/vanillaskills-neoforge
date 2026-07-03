package io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.PointsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/** Shows the ways players earn skill points (read from points.json). */
public final class PointsScreen {
    private PointsScreen() {}

    public static void open(ServerPlayer player) {
        PointsConfig cfg = VanillaSkills.PLAYERS.pointsConfig();
        int total = VanillaSkills.PLAYERS.totalEarnable();
        List<ItemStack> items = new ArrayList<>();

        items.add(item(Items.KNOWLEDGE_BOOK, "Earning Skill Shards", ChatFormatting.GOLD, List.of(
                "Skill Shards come from completing advancements.",
                "Each advancement counts once — they can't be farmed.",
                "An advancement's value depends on its difficulty.")));

        items.add(item(Items.PAPER, "Tasks (common)", ChatFormatting.WHITE, List.of(
                "Ordinary square-icon advancements.",
                "+" + cfg.valueTask + " Skill Shard" + (cfg.valueTask == 1 ? "" : "s") + " each")));

        items.add(item(Items.GOLD_INGOT, "Goals", ChatFormatting.YELLOW, List.of(
                "Rounded-icon goals — a bigger ask.",
                "+" + cfg.valueGoal + " Skill Shards each")));

        items.add(item(Items.NETHER_STAR, "Challenges (purple)", ChatFormatting.LIGHT_PURPLE, List.of(
                "The hardest, purple-framed advancements.",
                "+" + cfg.valueChallenge + " Skill Shards each")));

        int custom = VanillaSkills.PLAYERS.customAdvancementTotal();
        items.add(item(Items.DIAMOND_CHESTPLATE, "VanillaSkills Goals", ChatFormatting.AQUA, List.of(
                "Craft full armor sets, discover the upgrade",
                "templates, forge a Dragon Ingot, and finish",
                "skill paths for bonus Skill Shards.",
                "Worth " + custom + " Skill Shards in total.")));

        if (cfg.startingPoints > 0) {
            items.add(item(Items.EXPERIENCE_BOTTLE, "Starting Bonus", ChatFormatting.GREEN, List.of(
                    "+" + cfg.startingPoints + " Skill Shards when you first join")));
        }

        items.add(item(Items.BEACON, "The Grand Total", ChatFormatting.GOLD, List.of(
                "There are about " + total + " Skill Shards to earn.",
                "Doing every advancement unlocks the WHOLE",
                "skill tree — it's priced to match.")));

        items.add(item(Items.WRITABLE_BOOK, "Daily Bounties", ChatFormatting.LIGHT_PURPLE, List.of(
                "The bounty board gives Quest Shards,",
                "which you can convert to Skill Shards",
                "(3:1) at the shop — extra progress every day.")));

        InfoMenu.open(player, styled("Earning Skill Shards", ChatFormatting.AQUA), 6, items);
    }

    private static ItemStack item(net.minecraft.world.item.Item icon, String name, ChatFormatting color, List<String> lore) {
        ItemStack stack = new ItemStack(icon);
        Guis.hideStats(stack);
        stack.set(DataComponents.CUSTOM_NAME, styled(name, color));
        List<Component> lines = new ArrayList<>();
        for (String line : lore) lines.add(styled(line, ChatFormatting.GRAY));
        stack.set(DataComponents.LORE, new ItemLore(lines));
        return stack;
    }

    private static Component styled(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }
}
