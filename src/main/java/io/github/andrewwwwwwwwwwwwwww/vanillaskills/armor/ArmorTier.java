package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Repairable;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * One armor tier. Pieces are built by stamping a vanilla armor item with overriding components:
 * attribute modifiers (armor / toughness / knockback / movement speed), durability, a name, a
 * marker (for effects + identification), a repair list, and a model hook for resource packs.
 */
public class ArmorTier {
    public final String id;
    public final String displayName;
    public final int nameColor;
    public final String markerKey;
    private final Item[] baseItems;       // indexed by ArmorPiece.ordinal()
    private final int[] armor;            // per piece
    private final double toughness;       // per piece
    private final double knockback;       // per piece
    private final double perPieceSpeed;   // ADD_MULTIPLIED_BASE per piece (sums across set)
    private final int[] durability;       // per piece
    private final HolderSet<Item> repairItems;
    public final Predicate<ItemStack> material;
    private final Supplier<ItemLore> staticLore; // optional description (e.g. set bonus), nullable

    public ArmorTier(String id, String displayName, int nameColor, String markerKey,
                     Item[] baseItems, int[] armor, double toughness, double knockback,
                     double perPieceSpeed, int[] durability, HolderSet<Item> repairItems,
                     Predicate<ItemStack> material, Supplier<ItemLore> staticLore) {
        this.id = id;
        this.displayName = displayName;
        this.nameColor = nameColor;
        this.markerKey = markerKey;
        this.baseItems = baseItems;
        this.armor = armor;
        this.toughness = toughness;
        this.knockback = knockback;
        this.perPieceSpeed = perPieceSpeed;
        this.durability = durability;
        this.repairItems = repairItems;
        this.material = material;
        this.staticLore = staticLore;
    }

    public boolean isWorn(ItemStack stack) {
        return Markers.has(stack, markerKey);
    }

    public ItemStack create(ArmorPiece piece) {
        int i = piece.ordinal();
        ItemStack stack = new ItemStack(baseItems[i]);
        stack.set(DataComponents.CUSTOM_NAME, Markers.name(displayName + " " + pieceWord(piece), nameColor));
        Markers.applyMarker(stack, markerKey);
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(), List.of("vanillaskills:" + id + "_" + piece.lower()), List.of()));
        stack.set(DataComponents.MAX_DAMAGE, durability[i]);
        stack.set(DataComponents.REPAIRABLE, new Repairable(repairItems));

        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
        b.add(Attributes.ARMOR, modifier(piece, "armor", armor[i], AttributeModifier.Operation.ADD_VALUE), piece.group);
        if (toughness > 0) {
            b.add(Attributes.ARMOR_TOUGHNESS, modifier(piece, "toughness", toughness, AttributeModifier.Operation.ADD_VALUE), piece.group);
        }
        if (knockback > 0) {
            b.add(Attributes.KNOCKBACK_RESISTANCE, modifier(piece, "knockback", knockback, AttributeModifier.Operation.ADD_VALUE), piece.group);
        }
        if (perPieceSpeed != 0) {
            b.add(Attributes.MOVEMENT_SPEED, modifier(piece, "speed", perPieceSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), piece.group);
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, b.build());
        if (staticLore != null) {
            stack.set(DataComponents.LORE, staticLore.get());
        }

        // Give each tier its own worn-armor equipment asset (vanillaskills:<id>) so resource packs
        // can retexture the worn armour per tier. Keeps the base item's slot + equip sound.
        net.minecraft.world.item.equipment.Equippable baseEquippable = stack.get(DataComponents.EQUIPPABLE);
        if (baseEquippable != null) {
            stack.set(DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable
                    .builder(baseEquippable.slot())
                    .setEquipSound(baseEquippable.equipSound())
                    .setAsset(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,
                            Identifier.fromNamespaceAndPath("vanillaskills", id)))
                    .build());
        }
        return stack;
    }

    private AttributeModifier modifier(ArmorPiece piece, String attr, double amount, AttributeModifier.Operation op) {
        Identifier id = Identifier.fromNamespaceAndPath("vanillaskills", this.id + "." + piece.lower() + "." + attr);
        return new AttributeModifier(id, amount, op);
    }

    private static String pieceWord(ArmorPiece piece) {
        return switch (piece) {
            case HELMET -> "Helmet";
            case CHESTPLATE -> "Chestplate";
            case LEGGINGS -> "Leggings";
            case BOOTS -> "Boots";
        };
    }
}
