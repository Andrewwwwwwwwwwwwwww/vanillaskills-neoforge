package io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Alloys;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonIngot;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonScale;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.DragonUpgradeTemplate;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.FortuneTemplate;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.shield.SteelShield;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** The custom crafting recipes shown in the in-game recipe book (one 3x3 display per page). */
public final class RecipeBook {
    private RecipeBook() {}

    /**
     * One displayable recipe: a 3x3 grid of inputs (length 9; nulls allowed), a result, and the
     * crafting "station" icon shown at the top of the page — a crafting table for normal recipes,
     * an anvil for anvil-forged recipes. The 3-arg form defaults the station to a crafting table.
     */
    public record Display(String title, String[] desc, ItemStack[] grid, ItemStack result,
                          net.minecraft.world.item.Item station) {
        /** No description, crafting-table station. */
        public Display(String title, ItemStack[] grid, ItemStack result) {
            this(title, null, grid, result, Items.CRAFTING_TABLE);
        }
        /** No description, custom station icon. */
        public Display(String title, ItemStack[] grid, ItemStack result, net.minecraft.world.item.Item station) {
            this(title, null, grid, result, station);
        }
        /** With a "what it's for" description (shown as lore under the title), crafting-table station. */
        public Display(String title, String[] desc, ItemStack[] grid, ItemStack result) {
            this(title, desc, grid, result, Items.CRAFTING_TABLE);
        }
    }

    private static final ItemStack E = ItemStack.EMPTY;

    public static List<Display> all() {
        List<Display> r = new ArrayList<>();

        // 0. Hardwood — the first recipe: the base MATERIAL. Four logs in a 2x2 make Wood blocks
        //    (the all-bark cubes), which are what Hardwood tools & armor are crafted from.
        ItemStack log = new ItemStack(Items.OAK_LOG);
        r.add(new Display("Hardwood",
                new String[]{
                        "The base material for all Hardwood gear.",
                        "Then craft any tool or armor in its normal",
                        "shape using these Wood blocks (not planks).",
                        "Hardwood swords & axes poison on hit."},
                new ItemStack[]{
                        log.copy(), log.copy(), E,
                        log.copy(), log.copy(), E,
                        E, E, E},
                count(new ItemStack(Items.OAK_WOOD), 3)));

        ItemStack scale = DragonScale.create();
        ItemStack netherite = new ItemStack(Items.NETHERITE_INGOT);
        ItemStack steel = Alloys.steelIngot();
        ItemStack chorus = new ItemStack(Items.CHORUS_FLOWER);
        ItemStack endRod = new ItemStack(Items.END_ROD);
        ItemStack berry = new ItemStack(Items.GLOW_BERRIES);
        ItemStack sculk = new ItemStack(Items.SCULK);
        ItemStack lapis = new ItemStack(Items.LAPIS_BLOCK);
        ItemStack diaBlock = new ItemStack(Items.DIAMOND_BLOCK);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT);
        ItemStack copper = new ItemStack(Items.COPPER_INGOT);
        ItemStack diamond = new ItemStack(Items.DIAMOND);

        // 1. Rose Gold Ingot
        r.add(new Display("Rose Gold Ingot (×4)", new ItemStack[]{
                gold.copy(), copper.copy(), gold.copy(),
                copper.copy(), E, copper.copy(),
                gold.copy(), copper.copy(), gold.copy()}, count(Alloys.roseGoldIngot(), 4)));

        // 2. Steel Ingot — forged in an ANVIL (one iron in each input slot), not a crafting recipe.
        //    The anvil is shown as the station icon at the top, so the grid is just iron + iron.
        r.add(new Display("Steel Ingot (Anvil: iron + iron)", new ItemStack[]{
                E, E, E,
                new ItemStack(Items.IRON_INGOT), E, new ItemStack(Items.IRON_INGOT),
                E, E, E}, Alloys.steelIngot(), Items.ANVIL));

        // 3. Crystallized Diamond
        ItemStack shard = new ItemStack(Items.AMETHYST_SHARD);
        r.add(new Display("Crystallized Diamond (×2)", new ItemStack[]{
                diamond.copy(), shard.copy(), diamond.copy(),
                shard.copy(), new ItemStack(Items.AMETHYST_BLOCK), shard.copy(),
                diamond.copy(), shard.copy(), diamond.copy()}, count(Alloys.crystallizedDiamond(), 2)));

        // 4. Steel Shield
        r.add(new Display("Steel Shield", new ItemStack[]{
                steel.copy(), new ItemStack(Items.SHIELD), steel.copy(),
                steel.copy(), steel.copy(), steel.copy(),
                E, steel.copy(), E}, SteelShield.create()));

        // 5. Fortune Upgrade Template
        r.add(new Display("Fortune Upgrade Template (×2)", new ItemStack[]{
                berry.copy(), FortuneTemplate.create(), berry.copy(),
                sculk.copy(), diaBlock.copy(), sculk.copy(),
                berry.copy(), new ItemStack(Items.EMERALD_BLOCK), berry.copy()}, count(FortuneTemplate.create(), 2)));

        // 6. Fortune IV Book
        r.add(new Display("Fortune IV Book", new ItemStack[]{
                lapis.copy(), diaBlock.copy(), lapis.copy(),
                fortuneBook(3), FortuneTemplate.create(), fortuneBook(3),
                lapis.copy(), diaBlock.copy(), lapis.copy()}, fortuneBook(4)));

        // 7. Fortune V Book
        r.add(new Display("Fortune V Book", new ItemStack[]{
                lapis.copy(), diaBlock.copy(), lapis.copy(),
                fortuneBook(4), FortuneTemplate.create(), fortuneBook(4),
                lapis.copy(), diaBlock.copy(), lapis.copy()}, fortuneBook(5)));

        // 8. Dragon Ingot
        r.add(new Display("Dragon Ingot", new ItemStack[]{
                scale.copy(), scale.copy(), scale.copy(),
                scale.copy(), netherite.copy(), scale.copy(),
                scale.copy(), scale.copy(), scale.copy()}, DragonIngot.create()));

        // 9. Dragon Upgrade Template
        r.add(new Display("Dragon Upgrade Template (×2)", new ItemStack[]{
                chorus.copy(), DragonUpgradeTemplate.create(), chorus.copy(),
                chorus.copy(), netherite.copy(), chorus.copy(),
                endRod.copy(), new ItemStack(Items.SHULKER_SHELL), endRod.copy()}, count(DragonUpgradeTemplate.create(), 2)));

        return r;
    }

    private static ItemStack count(ItemStack stack, int n) {
        stack.setCount(n);
        return stack;
    }

    /** A display-only enchanted book labelled "Fortune N" (glint, no real enchantment needed for display). */
    private static ItemStack fortuneBook(int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        String roman = switch (level) { case 3 -> "III"; case 4 -> "IV"; case 5 -> "V"; default -> String.valueOf(level); };
        book.set(DataComponents.CUSTOM_NAME, Component.literal("Fortune " + roman)
                .withStyle(ChatFormatting.AQUA).withStyle(s -> s.withItalic(false)));
        book.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return book;
    }
}
