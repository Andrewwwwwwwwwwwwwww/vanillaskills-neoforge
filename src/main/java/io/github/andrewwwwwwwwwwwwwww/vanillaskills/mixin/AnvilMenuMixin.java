package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Alloys;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two anvil behaviours for VanillaSkills:
 *
 * <p><b>Steel forging.</b> Put a plain iron ingot in each of the anvil's two input slots and it
 * produces a Steel Ingot — the mod's only way to make steel (there is no crafting-table recipe).
 * Costs {@link #STEEL_FORGE_COST} level (the anvil won't hand over a zero-cost result) and consumes
 * exactly one iron from each slot per take, so a stack of iron can be forged one steel at a time.
 *
 * <p><b>Over-level enchantments.</b> The anvil clamps every enchantment to its max_level. Since
 * VanillaSkills keeps Fortune's max_level at 3 but mints Fortune IV/V directly, the anvil would knock
 * those back to III. After the vanilla result is computed, this un-clamps: for any enchantment already
 * present on the result, if either input carries a higher level, the result is raised to it.
 *
 * <p>Input/result slot fields live in the superclass {@code ItemCombinerMenu}, so they're read via the
 * inherited {@code getSlot}; {@code cost}/{@code repairItemCountCost} are on {@code AnvilMenu} and are
 * shadowed directly.
 */
@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {

    private static final int STEEL_FORGE_COST = 0; // free — mayPickup is overridden so a 0-cost result can still be taken

    @Shadow private int repairItemCountCost;
    @Shadow @org.spongepowered.asm.mixin.Final private DataSlot cost;

    /** A plain iron ingot — not steel or any other VanillaSkills item built on iron. */
    private static boolean vanillaskills$isPlainIron(ItemStack stack) {
        return stack.is(Items.IRON_INGOT) && !Markers.isOurs(stack);
    }

    /** Iron + iron in the two input slots forges a Steel Ingot. */
    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$forgeSteel(CallbackInfo ci) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        ItemStack left = self.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack right = self.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
        if (vanillaskills$isPlainIron(left) && vanillaskills$isPlainIron(right)) {
            self.getSlot(AnvilMenu.RESULT_SLOT).set(Alloys.steelIngot());
            this.repairItemCountCost = 1;
            this.cost.set(STEEL_FORGE_COST);
            ci.cancel();
        }
    }

    /** Let the steel result be taken even though it costs 0 levels (vanilla blocks zero-cost results). */
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$allowFreeSteel(Player player, boolean hasStack, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        ItemStack left = self.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack right = self.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
        if (vanillaskills$isPlainIron(left) && vanillaskills$isPlainIron(right)) {
            cir.setReturnValue(true);
        }
    }

    /** Forging steel consumes exactly one iron from each input (vanilla would clear the whole left slot). */
    @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$takeSteel(Player player, ItemStack stack, CallbackInfo ci) {
        // Iron + iron in the inputs means the result being taken is our forged steel (the only result
        // that combination produces). Keyed off the inputs, not the taken stack, which proved unreliable.
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        ItemStack left = self.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack right = self.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
        if (!vanillaskills$isPlainIron(left) || !vanillaskills$isPlainIron(right)) return; // not our recipe — let vanilla handle it
        if (!player.hasInfiniteMaterials() && this.cost.get() > 0) {
            player.giveExperienceLevels(-this.cost.get());
        }
        left.shrink(1);
        right.shrink(1);
        self.getSlot(AnvilMenu.INPUT_SLOT).set(left);
        self.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(right);
        this.cost.set(0);
        ((AnvilMenu) (Object) this).createResult(); // refresh — forge another if iron remains
        self.broadcastChanges();
        ci.cancel();
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void vanillaskills$preserveOverLevelEnchantments(CallbackInfo ci) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        ItemStack result = self.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        if (result.isEmpty()) return;

        DataComponentType<ItemEnchantments> resultType = enchantmentsType(result);
        ItemEnchantments resultEnch = result.get(resultType);
        if (resultEnch == null || resultEnch.isEmpty()) return;

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(resultEnch);
        boolean changed = false;
        for (int slot : new int[]{AnvilMenu.INPUT_SLOT, AnvilMenu.ADDITIONAL_SLOT}) {
            ItemStack input = self.getSlot(slot).getItem();
            if (input.isEmpty()) continue;
            ItemEnchantments inputEnch = input.get(enchantmentsType(input));
            if (inputEnch == null || inputEnch.isEmpty()) continue;
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : inputEnch.entrySet()) {
                Holder<Enchantment> key = entry.getKey();
                int inputLevel = entry.getIntValue();
                // Only lift enchantments the result already has (un-clamp, never add new).
                if (mutable.getLevel(key) > 0 && inputLevel > mutable.getLevel(key)) {
                    mutable.set(key, inputLevel);
                    changed = true;
                }
            }
        }
        if (changed) {
            result.set(resultType, mutable.toImmutable());
        }
    }

    private static DataComponentType<ItemEnchantments> enchantmentsType(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK) ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
    }
}
