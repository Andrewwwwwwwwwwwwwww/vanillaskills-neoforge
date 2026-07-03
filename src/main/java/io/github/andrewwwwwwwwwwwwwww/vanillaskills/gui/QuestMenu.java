package io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Quest;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Quests;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/** The bounty board: shows the 3 active quests, the player's progress, and lets them claim. */
public class QuestMenu extends ChestMenu {
    private static final int[] SLOTS = {11, 13, 15};
    private static final int INFO_SLOT = 4;
    private static final int SHOP_SLOT = 22;
    private static final int BACK_SLOT = 18;   // bottom-left: open the skill tree
    private static final int CLOSE_SLOT = 26;

    private final ServerPlayer player;
    private final SimpleContainer container;

    public static void open(ServerPlayer player) {
        Quests.sync(player);
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new QuestMenu(syncId, inv, (ServerPlayer) p),
                Component.literal("Bounty Board")));
    }

    private QuestMenu(int syncId, Inventory inv, ServerPlayer player) {
        super(MenuType.GENERIC_9x3, syncId, inv, new SimpleContainer(27), 3);
        this.player = player;
        this.container = (SimpleContainer) getContainer();
        populate();
    }

    private void populate() {
        for (int i = 0; i < container.getContainerSize(); i++) container.setItem(i, ItemStack.EMPTY);
        container.setItem(INFO_SLOT, infoItem());
        List<Quest> active = Quests.activeFor(player);
        for (int i = 0; i < active.size() && i < SLOTS.length; i++) {
            container.setItem(SLOTS[i], questItem(i, active.get(i)));
        }
        container.setItem(SHOP_SLOT, shopButton());
        container.setItem(BACK_SLOT, button(Items.NETHER_STAR, "Skill Tree", ChatFormatting.AQUA,
                "Open the skill tree."));
        container.setItem(CLOSE_SLOT, button(Items.BARRIER, "Close", ChatFormatting.RED, null));
    }

    private ItemStack infoItem() {
        long remaining = VanillaSkills.QUESTS.nextRotationMs() - System.currentTimeMillis();
        String when = remaining <= 0 ? "any moment" : (remaining / 3_600_000) + "h " + (remaining % 3_600_000 / 60_000) + "m";
        int quest = VanillaSkills.PLAYERS.questShards(player);
        boolean graduated = Quests.isGraduated(player);

        List<Component> lore = new ArrayList<>();
        if (graduated) {
            lore.add(styled("The main bounty board — shared by everyone.", ChatFormatting.GRAY));
        } else {
            lore.add(styled("Your starter board.", ChatFormatting.GRAY));
            lore.add(styled("Complete " + Quests.GRADUATE_AT + " quests to join the main board: "
                    + Quests.graduationProgress(player) + "/" + Quests.GRADUATE_AT, ChatFormatting.AQUA));
        }
        lore.add(styled("New bounties in " + when, ChatFormatting.YELLOW));
        lore.add(Component.literal(""));
        lore.add(styled("Your Quest Shards: " + quest, ChatFormatting.LIGHT_PURPLE));
        lore.add(styled("Click a bounty to claim its reward.", ChatFormatting.DARK_GRAY));

        ItemStack stack = new ItemStack(Items.CLOCK);
        stack.set(DataComponents.CUSTOM_NAME, styled(graduated ? "Bounty Board" : "Starter Board", ChatFormatting.GOLD));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private ItemStack shopButton() {
        ItemStack stack = new ItemStack(Items.EMERALD);
        stack.set(DataComponents.CUSTOM_NAME, styled("Quest Shop", ChatFormatting.GREEN));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                styled("Spend Quest Shards on boost items,", ChatFormatting.GRAY),
                styled("or convert them into Skill Shards.", ChatFormatting.GRAY),
                Component.literal(""),
                styled("Click to open", ChatFormatting.GREEN))));
        return stack;
    }

    private ItemStack button(net.minecraft.world.item.Item item, String name, ChatFormatting color, String desc) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, styled(name, color));
        if (desc != null) stack.set(DataComponents.LORE, new ItemLore(List.of(styled(desc, ChatFormatting.GRAY))));
        return stack;
    }

    private ItemStack questItem(int index, Quest q) {
        boolean claimed = Quests.isClaimed(player, index);
        int progress = Quests.progress(player, index);
        boolean ready = progress >= q.amount();

        ItemStack stack = new ItemStack(iconFor(q));
        ChatFormatting nameColor = claimed ? ChatFormatting.DARK_GRAY : ready ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
        stack.set(DataComponents.CUSTOM_NAME, styled(q.title() + (claimed ? " ✔" : ""), nameColor));

        List<Component> lore = new ArrayList<>();
        if (q.type() != Quest.Type.FREEBIE) {
            String verb = q.type() == Quest.Type.KILL ? "Slain" : "Gathered";
            lore.add(styled(verb + ": " + progress + "/" + q.amount(), ChatFormatting.GRAY));
        }
        lore.add(styled("Reward: +" + q.reward() + " Quest Shard" + (q.reward() == 1 ? "" : "s"), ChatFormatting.LIGHT_PURPLE));
        lore.add(Component.literal(""));
        if (claimed) {
            lore.add(styled("Claimed — come back next rotation", ChatFormatting.DARK_GRAY));
        } else if (ready) {
            lore.add(styled(q.type() == Quest.Type.GATHER ? "Click to turn in & claim" : "Click to claim", ChatFormatting.GREEN));
        } else {
            lore.add(styled(q.type() == Quest.Type.GATHER ? "Bring the items here" : "Keep hunting", ChatFormatting.GRAY));
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        if (claimed) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private static net.minecraft.world.item.Item iconFor(Quest q) {
        if (q.type() == Quest.Type.FREEBIE) return Items.EMERALD;
        if (q.type() == Quest.Type.GATHER) return Quests.item(q.target());
        if (q.target().equals(Quest.ANY_HOSTILE)) return Items.IRON_SWORD;
        net.minecraft.world.item.Item egg = Quests.item(q.target() + "_spawn_egg");
        return egg == Items.PAPER ? Items.IRON_SWORD : egg;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (clicker instanceof ServerPlayer sp) {
            if (slotId == CLOSE_SLOT) { sp.closeContainer(); return; }
            if (slotId == BACK_SLOT) { SkillTreeMenu.open(sp); return; }
            if (slotId == SHOP_SLOT) { ShopMenu.open(sp); return; }
            for (int i = 0; i < SLOTS.length; i++) {
                if (slotId == SLOTS[i]) {
                    Quests.claim(sp, i);
                    populate();
                    sendAllDataToRemote();
                    return;
                }
            }
        }
        sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private static Component styled(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }
}
