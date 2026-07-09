package io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Feat;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Feats;
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

/** The Feats tab: a read-only checklist of one-time achievements (discoveries, bosses, the End). */
public class FeatsMenu extends ChestMenu {
    // 5-5-1 centered layout (rows of 5 sit dead-centre; the last feat is centered under them).
    private static final int[] FEAT_SLOTS = {
            11, 12, 13, 14, 15,
            20, 21, 22, 23, 24,
            31};
    private static final int TITLE_SLOT = 4;
    private static final int BACK_SLOT = 36;
    private static final int CLOSE_SLOT = 44;

    private final ServerPlayer player;
    private final SimpleContainer container;

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new FeatsMenu(syncId, inv, (ServerPlayer) p),
                Component.literal("Feats")));
    }

    private FeatsMenu(int syncId, Inventory inv, ServerPlayer player) {
        super(MenuType.GENERIC_9x5, syncId, inv, new SimpleContainer(45), 5);
        this.player = player;
        this.container = (SimpleContainer) getContainer();
        populate();
    }

    private void populate() {
        for (int i = 0; i < 45; i++) container.setItem(i, ItemStack.EMPTY);
        container.setItem(TITLE_SLOT, titleItem());
        List<Feat> all = Feats.ALL;
        for (int i = 0; i < all.size() && i < FEAT_SLOTS.length; i++) {
            container.setItem(FEAT_SLOTS[i], featItem(all.get(i)));
        }
        container.setItem(BACK_SLOT, button(Items.ARROW, "Back to Bounty Board", ChatFormatting.YELLOW));
        container.setItem(CLOSE_SLOT, button(Items.BARRIER, "Close", ChatFormatting.RED));
    }

    private ItemStack titleItem() {
        int done = 0;
        for (Feat f : Feats.ALL) if (Feats.isDone(player, f.id())) done++;
        ItemStack stack = new ItemStack(Items.WITHER_SKELETON_SKULL);
        stack.set(DataComponents.CUSTOM_NAME, styled("Feats", ChatFormatting.GOLD));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                styled("One-time achievements — earned once, kept forever.", ChatFormatting.GRAY),
                styled("They award Quest Shards automatically when you do them.", ChatFormatting.GRAY),
                Component.literal(""),
                styled("Completed: " + done + "/" + Feats.ALL.size(), ChatFormatting.AQUA))));
        return stack;
    }

    private ItemStack featItem(Feat f) {
        boolean done = Feats.isDone(player, f.id());
        ItemStack stack = new ItemStack(Quests.item(f.icon()));
        stack.set(DataComponents.CUSTOM_NAME,
                styled(f.title() + (done ? " ✔" : ""), done ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        List<Component> lore = new ArrayList<>();
        lore.add(styled(f.desc(), ChatFormatting.GRAY));
        lore.add(Component.literal(""));
        lore.add(styled("Reward: +" + f.reward() + " Quest Shards", ChatFormatting.LIGHT_PURPLE));
        lore.add(Component.literal(""));
        lore.add(styled(done ? "Completed" : "Not yet earned", done ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        if (done) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private ItemStack button(net.minecraft.world.item.Item item, String name, ChatFormatting color) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, styled(name, color));
        return stack;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (clicker instanceof ServerPlayer sp) {
            if (slotId == CLOSE_SLOT) { sp.closeContainer(); return; }
            if (slotId == BACK_SLOT) { QuestMenu.open(sp); return; }
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
