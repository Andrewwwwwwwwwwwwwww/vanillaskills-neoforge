package io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Quest;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.QuestPool;
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

/**
 * The bounty board. Pre-graduation it shows ALL fixed starter quests (always active, complete each
 * once, no rotation); after graduating it shows the shared 3-quest rotating board.
 */
public class QuestMenu extends ChestMenu {
    // Graduated layout (9x4): 6 quests in a 3-3 block below the clock (rows 1-2, columns 2/4/6).
    private static final int[] MAIN_SLOTS = {11, 13, 15, 20, 22, 24};
    // Starter layout (9x5): a centered 5-wide x 3-row block of quests (columns 2-6).
    private static final int[] STARTER_SLOTS = {
            11, 12, 13, 14, 15,
            20, 21, 22, 23, 24,
            29, 30, 31, 32, 33};
    private static final int INFO_SLOT = 4;

    private final ServerPlayer player;
    private final SimpleContainer container;
    private final boolean starterBoard;
    private final int[] questSlots;
    private final int shopSlot;
    private final int backSlot;
    private final int closeSlot;
    private final int featsSlot;
    /** Rotation this menu was built against — clicks on a stale board reopen instead of claiming. */
    private long rotationAtBuild;

    public static void open(ServerPlayer player) {
        Quests.sync(player);
        boolean starter = !Quests.isGraduated(player);
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new QuestMenu(syncId, inv, (ServerPlayer) p, starter),
                Component.literal("Bounty Board")));
    }

    private QuestMenu(int syncId, Inventory inv, ServerPlayer player, boolean starterBoard) {
        super(starterBoard ? MenuType.GENERIC_9x5 : MenuType.GENERIC_9x4, syncId, inv,
                new SimpleContainer(starterBoard ? 45 : 36), starterBoard ? 5 : 4);
        this.player = player;
        this.container = (SimpleContainer) getContainer();
        this.starterBoard = starterBoard;
        this.questSlots = starterBoard ? STARTER_SLOTS : MAIN_SLOTS;
        int last = container.getContainerSize() - 1; // 44 or 35
        this.closeSlot = last;                       // bottom-right
        this.backSlot = last - 8;                    // bottom-left
        this.shopSlot = 0;                           // top-left
        this.featsSlot = 8;                          // top-right corner
        populate();
    }

    private void populate() {
        this.rotationAtBuild = VanillaSkills.QUESTS.rotationId();
        for (int i = 0; i < container.getContainerSize(); i++) container.setItem(i, ItemStack.EMPTY);
        container.setItem(INFO_SLOT, infoItem());
        List<Quest> active = Quests.activeFor(player);
        for (int i = 0; i < active.size() && i < questSlots.length; i++) {
            container.setItem(questSlots[i], questItem(i, active.get(i)));
        }
        container.setItem(shopSlot, shopButton());
        container.setItem(featsSlot, button(Items.WITHER_SKELETON_SKULL, "Feats", ChatFormatting.GOLD,
                "One-time achievements: discoveries, bosses & the End."));
        container.setItem(backSlot, button(Items.NETHER_STAR, "Skill Tree", ChatFormatting.AQUA,
                "Open the skill tree."));
        container.setItem(closeSlot, button(Items.BARRIER, "Close", ChatFormatting.RED, null));
    }

    private ItemStack infoItem() {
        int quest = VanillaSkills.PLAYERS.questShards(player);
        List<Component> lore = new ArrayList<>();
        if (starterBoard) {
            int total = QuestPool.STARTER.size();
            lore.add(styled("Your starter quests — all always available,", ChatFormatting.GRAY));
            lore.add(styled("no rotation, complete them in any order.", ChatFormatting.GRAY));
            lore.add(styled("Finish all " + total + " to join the main board: "
                    + Quests.graduationProgress(player) + "/" + total, ChatFormatting.AQUA));
        } else {
            long remaining = VanillaSkills.QUESTS.nextRotationMs() - System.currentTimeMillis();
            String when = remaining <= 0 ? "any moment" : (remaining / 3_600_000) + "h " + (remaining % 3_600_000 / 60_000) + "m";
            lore.add(styled("The main bounty board — shared by everyone.", ChatFormatting.GRAY));
            lore.add(styled("New bounties in " + when, ChatFormatting.YELLOW));
        }
        lore.add(Component.literal(""));
        lore.add(styled("Your Quest Shards: " + quest, ChatFormatting.LIGHT_PURPLE));
        lore.add(styled("Click a quest to claim its reward.", ChatFormatting.DARK_GRAY));

        ItemStack stack = new ItemStack(Items.CLOCK);
        stack.set(DataComponents.CUSTOM_NAME, styled(starterBoard ? "Starter Quests" : "Bounty Board", ChatFormatting.GOLD));
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
            String verb = switch (q.type()) {
                case KILL -> "Slain";
                case SKILL -> "Skills unlocked";
                case STAT -> "Progress";
                default -> "Gathered";
            };
            lore.add(styled(verb + ": " + progress + "/" + q.amount(), ChatFormatting.GRAY));
        }
        lore.add(styled("Reward: +" + q.reward() + " Quest Shard" + (q.reward() == 1 ? "" : "s"), ChatFormatting.LIGHT_PURPLE));
        lore.add(Component.literal(""));
        if (claimed) {
            lore.add(styled(starterBoard ? "Completed" : "Claimed — come back next rotation", ChatFormatting.DARK_GRAY));
        } else if (ready) {
            lore.add(styled(q.type() == Quest.Type.GATHER ? "Click to turn in & claim" : "Click to claim", ChatFormatting.GREEN));
        } else {
            String hint = switch (q.type()) {
                case GATHER -> "Bring the items here";
                case SKILL -> "Unlock skills in the skill tree (/skill)";
                case STAT -> "Counts from when this appeared — keep going";
                default -> "Keep hunting";
            };
            lore.add(styled(hint, ChatFormatting.GRAY));
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        if (claimed) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private static net.minecraft.world.item.Item iconFor(Quest q) {
        if (q.type() == Quest.Type.FREEBIE) return Items.EMERALD;
        if (q.type() == Quest.Type.SKILL) return Items.NETHER_STAR;
        if (q.type() == Quest.Type.STAT) return q.target().contains("swim") ? Items.WATER_BUCKET
                : q.target().contains("jump") ? Items.RABBIT_FOOT : Items.LEATHER_BOOTS;
        if (q.type() == Quest.Type.GATHER) return Quests.item(q.target());
        if (q.target().equals(Quest.ANY_HOSTILE)) return Items.IRON_SWORD;
        net.minecraft.world.item.Item egg = Quests.item(q.target() + "_spawn_egg");
        return egg == Items.PAPER ? Items.IRON_SWORD : egg;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (clicker instanceof ServerPlayer sp) {
            if (slotId == closeSlot) { sp.closeContainer(); return; }
            if (slotId == backSlot) { SkillTreeMenu.open(sp); return; }
            if (slotId == shopSlot) { ShopMenu.open(sp); return; }
            if (slotId == featsSlot) { FeatsMenu.open(sp); return; }
            for (int i = 0; i < questSlots.length; i++) {
                if (slotId == questSlots[i]) {
                    // Stale-board guard: if graduation status or the rotation changed while this menu
                    // was open, the rendered quests no longer match the live board — a claim here would
                    // hit the WRONG quest (and could consume the wrong items). Reopen instead.
                    boolean liveStarter = !Quests.isGraduated(sp);
                    if (liveStarter != starterBoard
                            || (!starterBoard && rotationAtBuild != VanillaSkills.QUESTS.rotationId())) {
                        sp.sendSystemMessage(Component.literal("The board changed — reopening.")
                                .withStyle(ChatFormatting.YELLOW));
                        open(sp);
                        return;
                    }
                    Quests.claim(sp, i);
                    // Graduating mid-menu changes the board layout; reopen with the right size.
                    if (starterBoard && Quests.isGraduated(sp)) {
                        open(sp);
                        return;
                    }
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
