package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Forges a Dragon Ingot from eight Dragon Scales + one (plain) Netherite Ingot, anywhere in the
 * 3x3 grid. Matched shapelessly by item count — the same robust style the other custom alloy
 * recipes use — rather than by exact grid positions.
 *
 *   D D D     D = Dragon Scale
 *   D N D     N = Netherite Ingot
 *   D D D
 */
public class DragonIngotRecipe extends CustomRecipe {
    public static final DragonIngotRecipe INSTANCE = new DragonIngotRecipe();
    public static final RecipeSerializer<DragonIngotRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(INSTANCE),
            StreamCodec.<RegistryFriendlyByteBuf, DragonIngotRecipe>unit(INSTANCE));

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int scales = 0, netherite = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (DragonScale.isDragonScale(s)) {
                scales++;
            } else if (s.is(Items.NETHERITE_INGOT) && !DragonIngot.isDragonIngot(s)) {
                netherite++;  // plain netherite only, not an existing Dragon Ingot
            } else {
                return false;
            }
        }
        return scales == 8 && netherite == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return DragonIngot.create();
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public RecipeSerializer<DragonIngotRecipe> getSerializer() {
        return SERIALIZER;
    }
}
