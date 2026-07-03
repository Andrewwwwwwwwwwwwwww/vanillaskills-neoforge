package io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool;

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
 * One special recipe for every (tier x tool kind). Matches a tool shape where the material cells
 * are all the same tier's crafting material and the stick cells are sticks, then outputs that
 * tier's tool.
 */
public class ToolCraftingRecipe extends CustomRecipe {
    public static final ToolCraftingRecipe INSTANCE = new ToolCraftingRecipe();
    public static final RecipeSerializer<ToolCraftingRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(INSTANCE),
            StreamCodec.<RegistryFriendlyByteBuf, ToolCraftingRecipe>unit(INSTANCE));

    private record Match(ToolTier tier, ToolKind kind) {}

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Match match = find(input);
        return match == null ? ItemStack.EMPTY : match.tier().create(match.kind());
    }

    private static Match find(CraftingInput input) {
        for (ToolKind kind : ToolKind.values()) {
            for (ToolKind.Shape shape : kind.shapes) {
                if (input.width() != shape.width() || input.height() != shape.height()) continue;
                ToolTier tier = matchShape(input, shape);
                if (tier != null) return new Match(tier, kind);
            }
        }
        return null;
    }

    private static ToolTier matchShape(CraftingInput input, ToolKind.Shape shape) {
        ToolTier tier = null;
        int cells = shape.width() * shape.height();
        for (int i = 0; i < cells; i++) {
            ItemStack cell = input.getItem(i);
            if (shape.isMat(i)) {
                if (cell.isEmpty()) return null;
                ToolTier cellTier = ToolTiers.tierForMaterial(cell);
                if (cellTier == null) return null;
                if (tier == null) tier = cellTier;
                else if (tier != cellTier) return null;
            } else if (shape.isStick(i)) {
                if (!cell.is(Items.STICK)) return null;
            } else if (!cell.isEmpty()) {
                return null;
            }
        }
        return tier;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    @Override
    public RecipeSerializer<ToolCraftingRecipe> getSerializer() {
        return SERIALIZER;
    }
}
