package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Deletes vanilla recipes for blocks VanillaSkills has taken over.
 *
 * <p>2.0 claims {@code lodestone} as the Stable Skill Shard Block, so its vanilla recipe has to go —
 * otherwise anyone can craft the mod's endgame block out of eight chiseled stone bricks and an iron ingot.
 *
 * <p><b>A datapack cannot do this.</b> There is no "delete recipe" in the format: the closest you get is
 * overriding the file with a recipe that cannot be satisfied, and the first attempt here did exactly that
 * with a 3x3 of barriers. It worked, but the recipe book cheerfully displayed it — a barrier grid sitting
 * in the crafting list, which is worse than the problem. Dropping the recipe outright removes it from
 * crafting, from the recipe book and from recipe unlocks in one go, silently.
 *
 * <p>{@code RecipeMap#create} is the single funnel every recipe map is built through, so filtering its
 * input catches the server's map and anything else that builds one. Since 26.3 recipes are a registry and
 * {@code create} is handed the registry's lookup rather than a list, so the filter is a lookup that lists
 * everything but the taken-over recipes.
 */
@Mixin(RecipeMap.class)
public class RecipeRemovalMixin {

    /** Vanilla recipe ids to drop. Keyed by recipe id, which is the file path under {@code data/…/recipe/}. */
    private static final Set<Identifier> VANILLASKILLS$REMOVED = Set.of(
            Identifier.fromNamespaceAndPath("minecraft", "lodestone"));

    @ModifyVariable(method = "create", at = @At("HEAD"), argsOnly = true, remap = false)
    private static HolderLookup<Recipe<?>> vanillaskills$dropTakenOverRecipes(HolderLookup<Recipe<?>> in) {
        if (in == null) return null;
        return new HolderLookup<>() {
            @Override
            public Stream<Holder.Reference<Recipe<?>>> listElements() {
                return in.listElements().filter(ref -> !vanillaskills$removed(ref.key()));
            }

            @Override
            public Stream<HolderSet.Named<Recipe<?>>> listTags() {
                return in.listTags();
            }

            @Override
            public Optional<Holder.Reference<Recipe<?>>> get(ResourceKey<Recipe<?>> key) {
                return vanillaskills$removed(key) ? Optional.empty() : in.get(key);
            }

            @Override
            public Optional<HolderSet.Named<Recipe<?>>> get(TagKey<Recipe<?>> tag) {
                return in.get(tag);
            }
        };
    }

    private static boolean vanillaskills$removed(ResourceKey<Recipe<?>> key) {
        return VANILLASKILLS$REMOVED.contains(key.identifier());
    }
}
