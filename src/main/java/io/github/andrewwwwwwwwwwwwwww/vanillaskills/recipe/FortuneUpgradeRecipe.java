package io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

/**
 * Upgrades a pair of Fortune books one level using the Fortune Upgrade template:
 *
 *   L D L     L = lapis block
 *   B T B     D = diamond block
 *   L D L     B = enchanted book with Fortune N (both same level, N in 3..4)
 *             T = Fortune Upgrade smithing template (consumed)
 *
 * Output: a single enchanted book with Fortune (N+1). max_level stays 3, so this is the only
 * way to obtain Fortune IV/V; the book is applied to a tool in an anvil (see AnvilMenuMixin).
 */
public class FortuneUpgradeRecipe extends CustomRecipe {
    public static final FortuneUpgradeRecipe INSTANCE = new FortuneUpgradeRecipe();
    public static final RecipeSerializer<FortuneUpgradeRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(INSTANCE),
            StreamCodec.<RegistryFriendlyByteBuf, FortuneUpgradeRecipe>unit(INSTANCE));

    private static final int MAX_LEVEL = 5;

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;

        // Fixed positions (row-major): 0..8
        if (!input.getItem(0).is(Items.LAPIS_BLOCK)) return false;
        if (!input.getItem(2).is(Items.LAPIS_BLOCK)) return false;
        if (!input.getItem(6).is(Items.LAPIS_BLOCK)) return false;
        if (!input.getItem(8).is(Items.LAPIS_BLOCK)) return false;
        if (!input.getItem(1).is(Items.DIAMOND_BLOCK)) return false;
        if (!input.getItem(7).is(Items.DIAMOND_BLOCK)) return false;
        if (!FortuneTemplate.isTemplate(input.getItem(4))) return false;

        int left = bookFortuneLevel(input.getItem(3));
        int right = bookFortuneLevel(input.getItem(5));
        return left >= 3 && left < MAX_LEVEL && left == right;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack sourceBook = input.getItem(3);
        ItemEnchantments stored = sourceBook.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null) return ItemStack.EMPTY;

        Holder<Enchantment> fortune = null;
        int level = 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stored.entrySet()) {
            if (entry.getKey().is(Enchantments.FORTUNE)) {
                fortune = entry.getKey();
                level = entry.getIntValue();
                break;
            }
        }
        if (fortune == null) return ItemStack.EMPTY;

        ItemStack out = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(fortune, Math.min(MAX_LEVEL, level + 1));
        out.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        return out;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    @Override
    public RecipeSerializer<FortuneUpgradeRecipe> getSerializer() {
        return SERIALIZER;
    }

    private static int bookFortuneLevel(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.ENCHANTED_BOOK)) return 0;
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null || stored.isEmpty()) return 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stored.entrySet()) {
            if (entry.getKey().is(Enchantments.FORTUNE)) return entry.getIntValue();
        }
        return 0;
    }
}
