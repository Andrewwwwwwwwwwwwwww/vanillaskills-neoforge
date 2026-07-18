package io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorPiece;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorTier;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorTiers;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolKind;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTier;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTiers;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.PlayerSkillData;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillCategory;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillNode;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillTree;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * The skill GUI. With {@code category == null} it's the lane-select screen (one icon per lane);
 * with a category set it shows that lane's nodes. Edit mode lets ops manage lanes and nodes.
 */
public class SkillTreeMenu extends ChestMenu {
    private static final int POINTS_SLOT = 45;  // bottom-left corner
    private static final int STATS_SLOT = 53;   // bottom-right corner
    private static final int BACK_SLOT = 49;    // bottom-centre (lane view)

    private final ServerPlayer player;
    private final SimpleContainer container;
    private final boolean editMode;
    private final boolean layoutMode;   // lane-select: drag lane icons to rearrange them
    private final String category;   // null = lane-select view
    private String selected;         // node id picked up in edit mode
    private String selectedCategory; // lane id picked up in layout mode

    public static void open(ServerPlayer player) {
        openInternal(player, false, false, null);
    }

    public static void openEditor(ServerPlayer player) {
        openInternal(player, true, false, null);
    }

    /** Open the lane-select screen in layout mode: click a lane to pick it up, click a spot to move/swap. */
    public static void openLayout(ServerPlayer player) {
        openInternal(player, false, true, null);
    }

    public static void openCategory(ServerPlayer player, String categoryId, boolean editMode) {
        openInternal(player, editMode, false, categoryId);
    }

    private static void openInternal(ServerPlayer player, boolean editMode, boolean layoutMode, String category) {
        SkillTree tree = VanillaSkills.TREE.tree();
        String base;
        if (category != null) {
            SkillCategory cat = tree.category(category);
            base = cat != null ? cat.title : "Skills";
        } else {
            base = tree.title == null ? "Skills" : tree.title;
        }
        Component title = Component.literal(layoutMode ? base + " (Layout)" : editMode ? base + " (Edit Mode)" : base);
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new SkillTreeMenu(syncId, inv, (ServerPlayer) p, editMode, layoutMode, category), title));
    }

    public SkillTreeMenu(int syncId, Inventory inv, ServerPlayer player, boolean editMode, boolean layoutMode, String category) {
        super(menuTypeFor(VanillaSkills.TREE.tree().rows), syncId, inv,
                new SimpleContainer(VanillaSkills.TREE.tree().slotCount()), clampRows(VanillaSkills.TREE.tree().rows));
        this.player = player;
        this.editMode = editMode;
        this.layoutMode = layoutMode;
        this.category = category;
        this.container = (SimpleContainer) getContainer();
        populate();
    }

    private static int clampRows(int rows) {
        return Math.max(1, Math.min(6, rows));
    }

    private static MenuType<ChestMenu> menuTypeFor(int rows) {
        return switch (clampRows(rows)) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    private void populate() {
        SkillTree tree = VanillaSkills.TREE.tree();
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        int size = container.getContainerSize();
        for (int i = 0; i < size; i++) container.setItem(i, ItemStack.EMPTY);

        if (category == null) {
            for (SkillCategory cat : tree.categories()) {
                // Config-disabled crafting lanes are hidden from players (ops still see them in edit mode).
                if (!editMode && io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.CraftingGate.laneDisabled(cat.id)) continue;
                container.setItem(cat.slot, buildCategoryItem(cat, data));
            }
            if (layoutMode) {
                container.setItem(size - 1, layoutHelp());
            } else if (editMode) {
                container.setItem(size - 1, buildEditInfo(true));
            } else {
                container.setItem(4, skillsHeader());
                container.setItem(POINTS_SLOT, buildCounter(data));
                container.setItem(STATS_SLOT, buildStatsHead());
            }
        } else {
            for (SkillNode node : tree.nodesIn(category)) {
                container.setItem(node.slot, editMode ? buildEditItem(node) : buildNodeItem(node, data));
            }
            container.setItem(BACK_SLOT, backButton());
            if (editMode) {
                container.setItem(size - 1, buildEditInfo(false));
            } else {
                SkillCategory cat = tree.category(category);
                if (cat != null) {
                    int hs = headerSlot();               // top-center, else nearest free top-row slot
                    if (hs >= 0) container.setItem(hs, laneHeader(cat, data));
                }
                container.setItem(POINTS_SLOT, buildCounter(data));
                container.setItem(STATS_SLOT, buildStatsHead());
            }
        }
    }

    // ---- lane select ----

    private ItemStack buildCategoryItem(SkillCategory cat, PlayerSkillData data) {
        SkillTree tree = VanillaSkills.TREE.tree();
        // Recipes is a pseudo-lane that opens the custom-recipe book.
        if ("recipes".equals(cat.id)) {
            ItemStack r = new ItemStack(resolveItem(cat.icon));
            Guis.hideStats(r);
            r.set(DataComponents.CUSTOM_NAME, styled("Recipes", ChatFormatting.GOLD));
            r.set(DataComponents.LORE, new ItemLore(List.of(
                    styled("All custom crafting recipes", ChatFormatting.GRAY),
                    styled(layoutMode ? "Click to pick up & move" : "Click to view", ChatFormatting.YELLOW))));
            return r;
        }
        if ("guide".equals(cat.id)) {
            ItemStack r = new ItemStack(resolveItem(cat.icon));
            Guis.hideStats(r);
            r.set(DataComponents.CUSTOM_NAME, styled("Guide", ChatFormatting.GOLD));
            r.set(DataComponents.LORE, new ItemLore(List.of(
                    styled("How VanillaSkills works", ChatFormatting.GRAY),
                    styled(layoutMode ? "Click to pick up & move" : "Click to open", ChatFormatting.YELLOW))));
            return r;
        }
        if ("quests".equals(cat.id)) {
            ItemStack r = new ItemStack(resolveItem(cat.icon));
            Guis.hideStats(r);
            r.set(DataComponents.CUSTOM_NAME, styled("Bounty Board", ChatFormatting.GOLD));
            r.set(DataComponents.LORE, new ItemLore(List.of(
                    styled("Quests & the Quest Shop", ChatFormatting.GRAY),
                    styled(layoutMode ? "Click to pick up & move" : "Click to open", ChatFormatting.YELLOW))));
            return r;
        }
        int total = 0, unlocked = 0;
        boolean quest = false;
        for (SkillNode node : tree.nodesIn(cat.id)) {
            total++;
            if (data.hasUnlocked(node.id)) unlocked++;
            if (node.isQuestCurrency()) quest = true;
        }
        ItemStack stack = new ItemStack(resolveItem(cat.icon));
        Guis.hideStats(stack);
        // Layout mode: every lane is draggable; show the picked-up one as "(moving)".
        if (layoutMode) {
            boolean moving = cat.id.equals(selectedCategory);
            stack.set(DataComponents.CUSTOM_NAME, styled(cat.title + (moving ? " (moving)" : ""),
                    moving ? ChatFormatting.GOLD : (quest ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA)));
            stack.set(DataComponents.LORE, new ItemLore(List.of(
                    styled(quest ? "Crafting (Quest Shards)" : "Skill (Skill Shards)", ChatFormatting.GRAY),
                    styled(moving ? "Click a spot to place it" : "Click to pick up & move", ChatFormatting.YELLOW))));
            if (moving) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            return stack;
        }
        // A locked lane (e.g. Night Vision) stays sealed until its earned-Shard gate is met — and we
        // deliberately don't reveal the requirement, so players can't bee-line to it.
        if (!editMode && isLaneLocked(cat, data)) {
            stack.set(DataComponents.CUSTOM_NAME, styled(cat.title, ChatFormatting.DARK_GRAY));
            stack.set(DataComponents.LORE, new ItemLore(List.of(styled("🔒 Locked", ChatFormatting.RED))));
            return stack;
        }
        // Crafting lanes are tinted purple (Quest Shards); skill lanes stay aqua (Skill Shards).
        ChatFormatting nameColor = quest ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA;
        stack.set(DataComponents.CUSTOM_NAME, styled(cat.title, nameColor));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                styled(unlocked + "/" + total + " unlocked", ChatFormatting.GRAY),
                styled(quest ? "Quest Shards" : "Skill Shards", quest ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA),
                styled(editMode ? "Click to edit this lane" : "Click to open", ChatFormatting.YELLOW))));
        if (total > 0 && unlocked == total) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    /** True if any node in this lane is still gated behind an earned-Shard requirement the player hasn't met. */
    private static boolean isLaneLocked(SkillCategory cat, PlayerSkillData data) {
        for (SkillNode node : VanillaSkills.TREE.tree().nodesIn(cat.id)) {
            if (node.minEarned > 0 && data.pointsEarned < node.minEarned) return true;
        }
        return false;
    }

    // ---- lane view (nodes) ----

    /** Short blurb per lane explaining what its skills do — shown on the lane header at the top. */
    private static final java.util.Map<String, String[]> LANE_DESCRIPTIONS = java.util.Map.ofEntries(
            java.util.Map.entry("health", new String[]{"Raises your maximum health.", "+2 hearts per tier, up to +20 hearts."}),
            java.util.Map.entry("speed", new String[]{"Move faster on foot.", "+2% walk speed per tier, up to +30%."}),
            java.util.Map.entry("mining", new String[]{"Mine blocks faster.", "Stacking mining efficiency — max it out to",
                    "instamine stone with an Efficiency V pickaxe."}),
            java.util.Map.entry("luck", new String[]{"Raises your Luck (+0.5 per tier, up to +5).",
                    "Better loot from chests, vaults & fishing."}),
            java.util.Map.entry("damage", new String[]{"Hit harder in melee.", "+1 attack damage per tier, up to +10."}),
            java.util.Map.entry("guardian", new String[]{"Take less damage.", "+1 armor per tier, up to +10."}),
            java.util.Map.entry("reach", new String[]{"Place and hit from further away.",
                    "+0.5 block & entity reach per tier (max +2.5)."}),
            java.util.Map.entry("mountaineer", new String[]{"Auto-step up ledges up to 1.1 blocks tall.",
                    "Sneak to walk normally. Toggle: /skill toggle stepup."}),
            java.util.Map.entry("aquatic", new String[]{"Underwater mastery: longer breath,",
                    "faster swimming, and quicker underwater mining."}),
            java.util.Map.entry("armorsmith", new String[]{"Unlocks crafting each armor tier,",
                    "Hardwood up to Dragon. Paid in Quest Shards."}),
            java.util.Map.entry("toolsmith", new String[]{"Unlocks crafting each tool tier,",
                    "Hardwood up to Dragon. Paid in Quest Shards."}),
            java.util.Map.entry("brewmaster", new String[]{"Beneficial potions last longer —",
                    "+10% duration per tier, up to +50%."}),
            java.util.Map.entry("evasion", new String[]{"Chance to fully dodge incoming arrows.",
                    "+2% per tier, up to 20%."}),
            java.util.Map.entry("cultivator", new String[]{"Bonus crops when harvesting mature crops.",
                    "+20% chance per tier, up to a guaranteed extra."}),
            java.util.Map.entry("nightvision", new String[]{"A capstone granting permanent Night Vision.",
                    "Toggle with /skill toggle nightvision."}));

    /** The lane-view header: the branch's icon, name, what it does, and unlock progress. */
    private ItemStack laneHeader(SkillCategory cat, PlayerSkillData data) {
        SkillTree tree = VanillaSkills.TREE.tree();
        int total = 0, unlocked = 0;
        boolean quest = false;
        for (SkillNode node : tree.nodesIn(cat.id)) {
            total++;
            if (data.hasUnlocked(node.id)) unlocked++;
            if (node.isQuestCurrency()) quest = true;
        }
        ChatFormatting color = quest ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA;
        ItemStack stack = new ItemStack(resolveItem(cat.icon));
        Guis.hideStats(stack);
        stack.set(DataComponents.CUSTOM_NAME, styled(cat.title, color));
        List<Component> lore = new ArrayList<>();
        for (String line : LANE_DESCRIPTIONS.getOrDefault(cat.id,
                new String[]{"Spend Shards to unlock this branch's skills."})) {
            lore.add(styled(line, ChatFormatting.GRAY));
        }
        lore.add(Component.literal(""));
        lore.add(styled(unlocked + "/" + total + " unlocked", color));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    /** Top-center (slot 4) for the lane header, or the nearest free slot in the top row. */
    private int headerSlot() {
        if (container.getItem(4).isEmpty()) return 4;
        for (int i = 0; i < 9; i++) if (container.getItem(i).isEmpty()) return i;
        return -1;
    }

    private ItemStack buildNodeItem(SkillNode node, PlayerSkillData data) {
        boolean unlocked = data.hasUnlocked(node.id);
        boolean prereqMet = node.requires.stream().allMatch(data::hasUnlocked);

        boolean quest = node.isQuestCurrency();
        String curName = quest ? "Quest Shards" : "Skill Shards";
        ChatFormatting curColor = quest ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.YELLOW;
        int balance = quest ? data.questShardsAvailable : data.pointsAvailable;

        boolean gated = node.minEarned > 0 && data.pointsEarned < node.minEarned;
        int chain = VanillaSkills.PLAYERS.chainCost(player, node.id); // total a left-click would charge
        boolean affordableChain = balance >= chain;

        ItemStack stack = iconStackFor(node);
        Guis.hideStats(stack);
        ChatFormatting nameColor = unlocked ? ChatFormatting.GREEN
                : gated ? ChatFormatting.DARK_GRAY
                : !prereqMet ? ChatFormatting.GRAY
                : affordableChain ? curColor : ChatFormatting.RED;
        stack.set(DataComponents.CUSTOM_NAME, styled(node.title + (unlocked ? " ✔" : ""), nameColor));

        List<Component> lore = new ArrayList<>();
        for (String line : node.description) lore.add(styled(line, ChatFormatting.GRAY));
        lore.add(Component.literal(""));
        if (unlocked) {
            lore.add(styled("Unlocked", ChatFormatting.GREEN));
        } else if (gated) {
            lore.add(styled("🔒 Locked", ChatFormatting.RED)); // requirement intentionally hidden
        } else {
            lore.add(styled("Cost: " + chain + " " + curName, affordableChain ? curColor : ChatFormatting.RED));
            if (!prereqMet) lore.add(styled("Buys this + the skills below it", ChatFormatting.DARK_GRAY));
            else lore.add(styled("Left-click to unlock", ChatFormatting.DARK_GRAY));
            if (!affordableChain) lore.add(styled("Not enough " + curName, ChatFormatting.RED));
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        if (unlocked) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private ItemStack buildEditItem(SkillNode node) {
        boolean isSelected = node.id.equals(selected);
        ItemStack stack = iconStackFor(node);
        Guis.hideStats(stack);
        stack.set(DataComponents.CUSTOM_NAME,
                styled(node.title + (isSelected ? " (moving)" : ""), isSelected ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        List<Component> lore = new ArrayList<>();
        lore.add(styled("id: " + node.id, ChatFormatting.DARK_GRAY));
        lore.add(styled("slot " + node.slot + "   cost " + node.cost, ChatFormatting.GRAY));
        lore.add(styled("effects: " + node.effects.size() + "   requires: " + node.requires, ChatFormatting.GRAY));
        lore.add(Component.literal(""));
        lore.add(styled("Left-click: pick up / place / swap", ChatFormatting.YELLOW));
        lore.add(styled("Right-click: delete node", ChatFormatting.RED));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        if (isSelected) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private ItemStack buildCounter(PlayerSkillData data) {
        ItemStack stack = new ItemStack(Items.EXPERIENCE_BOTTLE);
        stack.set(DataComponents.CUSTOM_NAME, styled("Your Shards", ChatFormatting.GOLD));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                styled("Skill Shards: " + data.pointsAvailable, ChatFormatting.AQUA),
                styled("Quest Shards: " + data.questShardsAvailable, ChatFormatting.LIGHT_PURPLE),
                Component.literal(""),
                styled("Click to see how to earn Skill Shards", ChatFormatting.GRAY))));
        return stack;
    }

    private ItemStack buildStatsHead() {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponents.CUSTOM_NAME, styled("Your Stats", ChatFormatting.AQUA));
        stack.set(DataComponents.LORE, new ItemLore(List.of(styled("Click to view your current stats", ChatFormatting.GRAY))));
        return stack;
    }

    private ItemStack backButton() {
        ItemStack stack = new ItemStack(Items.ARROW);
        stack.set(DataComponents.CUSTOM_NAME, styled("Back to Lanes", ChatFormatting.YELLOW));
        return stack;
    }

    private ItemStack buildEditInfo(boolean laneSelect) {
        ItemStack stack = new ItemStack(Items.WRITABLE_BOOK);
        stack.set(DataComponents.CUSTOM_NAME, styled("Edit Mode", ChatFormatting.GOLD));
        List<Component> lore = new ArrayList<>();
        if (laneSelect) {
            lore.add(styled("Left-click a lane to edit its skills.", ChatFormatting.GRAY));
            lore.add(styled("Left-click a blank slot to add a lane.", ChatFormatting.GRAY));
            lore.add(styled("Manage lanes: /skill edit category ...", ChatFormatting.DARK_GRAY));
        } else {
            lore.add(styled("Left-click a node to pick up / move / swap.", ChatFormatting.GRAY));
            lore.add(styled("Right-click a node to delete it.", ChatFormatting.GRAY));
            lore.add(styled("Left-click a blank slot to add a skill here.", ChatFormatting.GRAY));
            lore.add(styled("Set cost/effects: /skill edit ...", ChatFormatting.DARK_GRAY));
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    // ---- edit handling (within a lane) ----

    private void handleNodeEdit(int slotId, int button) {
        SkillTree tree = VanillaSkills.TREE.tree();
        SkillNode atSlot = tree.nodeInCategoryAtSlot(category, slotId);

        if (button == 1) {
            if (atSlot != null && !atSlot.id.equals(SkillTree.ROOT_ID)) {
                tree.nodes.remove(atSlot);
                if (atSlot.id.equals(selected)) selected = null;
                VanillaSkills.TREE.touchAndSave();
            }
            return;
        }
        if (selected == null) {
            if (atSlot != null) selected = atSlot.id;
            else promptAddSkill(slotId);
            return;
        }
        SkillNode sel = tree.byId(selected);
        if (sel == null) {
            selected = null;
            return;
        }
        if (atSlot == null) {
            sel.slot = slotId;
            sel.category = category;
            VanillaSkills.TREE.touchAndSave();
            selected = null;
        } else if (atSlot.id.equals(selected)) {
            selected = null;
        } else {
            int tmp = sel.slot;
            sel.slot = atSlot.slot;
            atSlot.slot = tmp;
            VanillaSkills.TREE.touchAndSave();
            selected = null;
        }
    }

    private void promptAddSkill(int slotId) {
        String command = "/skill edit add newskill " + category + " " + slotId + " 1 minecraft:stone";
        suggest("Click to add a skill to this lane at slot " + slotId, command);
    }

    private void promptAddCategory(int slotId) {
        String command = "/skill edit category add newlane " + slotId + " minecraft:book";
        suggest("Click to add a lane at slot " + slotId, command);
    }

    private void suggest(String text, String command) {
        player.sendSystemMessage(Component.literal(text + " (then edit the id/icon)")
                .withStyle(s -> s.withColor(0x55FF55).withItalic(false).withClickEvent(new ClickEvent.SuggestCommand(command))));
    }

    // ---- shared ----

    private static MutableComponent styled(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }

    private static Item resolveItem(String iconId) {
        if (iconId == null) return Items.STONE;
        Identifier id = Identifier.tryParse(iconId);
        if (id == null) return Items.STONE;
        return BuiltInRegistries.ITEM.get(id).map(Holder::value).orElse(Items.STONE);
    }

    /**
     * Icon for a node. Armorsmith/Toolsmith ladder nodes on a VanillaSkills tier (Hardwood, Rose Gold,
     * Steel, Crystalline, Dragon) use the real marked tier item so the resource pack retextures it —
     * otherwise they'd show the vanilla base texture. Vanilla-tier and all other nodes use their icon id.
     */
    private static ItemStack iconStackFor(SkillNode node) {
        ItemStack custom = customTierIcon(node);
        return custom != null ? custom : new ItemStack(resolveItem(node.icon));
    }

    private static ItemStack customTierIcon(SkillNode node) {
        boolean armor = "armorsmith".equals(node.category);
        boolean tool = "toolsmith".equals(node.category);
        if (!armor && !tool) return null;
        int idx;
        try {
            idx = Integer.parseInt(node.id.substring(node.category.length() + 1)) - 1;
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return null;
        }
        // Ladder order: Hardwood Copper Gold RoseGold Iron Steel Diamond Crystalline Netherite Dragon.
        // Only the VanillaSkills tiers (0,3,5,7,9) have custom textures; the rest stay vanilla.
        if (armor) {
            ArmorTier t = switch (idx) {
                case 0 -> ArmorTiers.HARDWOOD; case 3 -> ArmorTiers.ROSE_GOLD; case 5 -> ArmorTiers.STEEL;
                case 7 -> ArmorTiers.CRYSTAL; case 9 -> ArmorTiers.DRAGON; default -> null;
            };
            return t == null ? null : t.create(ArmorPiece.CHESTPLATE);
        }
        ToolTier t = switch (idx) {
            case 0 -> ToolTiers.HARDWOOD; case 3 -> ToolTiers.ROSE_GOLD; case 5 -> ToolTiers.STEEL;
            case 7 -> ToolTiers.CRYSTAL; case 9 -> ToolTiers.DRAGON; default -> null;
        };
        return t == null ? null : t.create(ToolKind.PICKAXE);
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (slotId >= 0 && slotId < container.getContainerSize() && clicker instanceof ServerPlayer sp) {
            if (category == null) {
                if (layoutMode) handleLayoutClick(sp, slotId);
                else if (handleLaneSelectClick(sp, slotId)) return;
            } else {
                if (handleLaneViewClick(sp, slotId, button)) return;
            }
        }
        populate();
        sendAllDataToRemote();
    }

    /** Layout mode: pick up a lane, then click an empty spot to move it or another lane to swap. */
    private void handleLayoutClick(ServerPlayer sp, int slotId) {
        SkillTree tree = VanillaSkills.TREE.tree();
        SkillCategory cat = tree.categoryAtSlot(slotId);
        if (selectedCategory == null) {
            if (cat != null) selectedCategory = cat.id; // pick up
            return;
        }
        SkillCategory sel = tree.category(selectedCategory);
        if (sel == null) { selectedCategory = null; return; }
        if (cat != null && cat.id.equals(selectedCategory)) { selectedCategory = null; return; } // deselect
        if (slotId == POINTS_SLOT || slotId == STATS_SLOT || slotId == 4 || slotId == container.getContainerSize() - 1) {
            sp.sendSystemMessage(Component.literal("That spot is reserved (header / Points / Stats).")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (cat == null) {
            sel.slot = slotId;                              // move to an empty spot
        } else {
            int tmp = sel.slot; sel.slot = cat.slot; cat.slot = tmp; // swap two lanes
        }
        VanillaSkills.TREE.touchAndSave();
        selectedCategory = null;
    }

    private ItemStack skillsHeader() {
        ItemStack stack = new ItemStack(Items.NETHER_STAR);
        Guis.hideStats(stack);
        stack.set(DataComponents.CUSTOM_NAME, styled("✦ Skills ✦", ChatFormatting.GOLD));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                styled("Spend Skill Shards & Quest Shards here", ChatFormatting.GRAY))));
        return stack;
    }

    private ItemStack layoutHelp() {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.CUSTOM_NAME, styled("Layout Mode", ChatFormatting.GOLD));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                styled("Click a lane to pick it up,", ChatFormatting.GRAY),
                styled("then click an empty spot to move it", ChatFormatting.GRAY),
                styled("or another lane to swap them.", ChatFormatting.GRAY),
                styled("Changes save automatically. Close when done.", ChatFormatting.DARK_GRAY))));
        return stack;
    }

    /** @return true if a sub-screen was opened (this menu is being replaced). */
    private boolean handleLaneSelectClick(ServerPlayer sp, int slotId) {
        if (!editMode && slotId == POINTS_SLOT) {
            PointsScreen.open(sp);
            return true;
        }
        if (!editMode && slotId == STATS_SLOT) {
            StatsScreen.open(sp);
            return true;
        }
        SkillCategory cat = VanillaSkills.TREE.tree().categoryAtSlot(slotId);
        if (cat != null) {
            if (!editMode && io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.CraftingGate.laneDisabled(cat.id)) {
                return false; // lane hidden by config — its slot is empty for players
            }
            if (!editMode && "recipes".equals(cat.id)) {
                RecipeBookMenu.open(sp, 0);
                return true;
            }
            if (!editMode && "guide".equals(cat.id)) {
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.book.GuideBook.open(sp);
                return true;
            }
            if (!editMode && "quests".equals(cat.id)) {
                QuestMenu.open(sp);
                return true;
            }
            if (!editMode && isLaneLocked(cat, VanillaSkills.PLAYERS.get(sp.getUUID()))) {
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("🔒 That path is still locked.")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
            openCategory(sp, cat.id, editMode);
            return true;
        }
        if (editMode) promptAddCategory(slotId);
        return false;
    }

    private boolean handleLaneViewClick(ServerPlayer sp, int slotId, int button) {
        if (slotId == BACK_SLOT) {
            openInternal(sp, editMode, false, null);
            return true;
        }
        if (!editMode && slotId == POINTS_SLOT) {
            PointsScreen.open(sp);
            return true;
        }
        if (!editMode && slotId == STATS_SLOT) {
            StatsScreen.open(sp);
            return true;
        }
        if (editMode) {
            handleNodeEdit(slotId, button);
        } else {
            SkillNode node = VanillaSkills.TREE.tree().nodeInCategoryAtSlot(category, slotId);
            if (node != null) {
                VanillaSkills.PLAYERS.unlockChain(sp, node.id);   // buy this node + everything below it
            }
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
