package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages the Mountaineer (step-up) skill's {@code minecraft:step_height} bonus dynamically, so it is
 * SUPPRESSED while the player is sneaking (hold shift → vanilla step height, so you carefully step
 * onto/around ledges near lava instead of auto-stepping) and while the player has toggled it off via
 * {@code /skill toggle stepup}.
 *
 * <p>Done as server-side attribute management (add/remove the transient modifier) rather than a
 * step-height mixin, because the mod is server-side and vanilla clients must get the fix too — the
 * step_height attribute is synced to the client, so removing the modifier makes the vanilla client
 * stop auto-stepping within a tick.
 *
 * <p>Reconciles only when a player's suppressed-state actually changes (tracked in {@link #lastSuppressed}),
 * so the per-tick cost is a boolean compare for players who aren't toggling sneak.
 */
public final class StepHeight {
    private StepHeight() {}

    private static final String STEP_ATTR = "minecraft:step_height";
    /** Node-id prefix of the step-up lane (Mountaineer). */
    private static final String STEP_LANE_PREFIX = "mountaineer";

    /** uuid -> last computed suppressed state, so we only touch the attribute on a change. */
    private static final Map<UUID, Boolean> lastSuppressed = new HashMap<>();

    /** True when the step-up bonus should be OFF right now (sneaking, or toggled off). */
    public static boolean suppressed(ServerPlayer player, PlayerSkillData data) {
        return player.isShiftKeyDown() || (data != null && data.stepUpDisabled);
    }

    /** Whether the player has any step-up node unlocked (cheap prefix scan). */
    public static boolean hasStepSkill(PlayerSkillData data) {
        if (data == null) return false;
        for (String id : data.unlocked) {
            if (id.startsWith(STEP_LANE_PREFIX)) return true;
        }
        return false;
    }

    /** Called every server tick: keeps each player's step_height matching their sneak/toggle state. */
    public static void tick(MinecraftServer server, SkillTree tree) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            PlayerSkillData data = VanillaSkills.PLAYERS.get(uuid);
            boolean sup = suppressed(player, data);
            Boolean prev = lastSuppressed.get(uuid);
            if (prev != null && prev == sup) continue; // no change → nothing to do
            if (!hasStepSkill(data)) { lastSuppressed.put(uuid, sup); continue; }
            lastSuppressed.put(uuid, sup);
            reconcile(player, data, tree, sup);
        }
    }

    /** Add or remove the step_height modifiers of the player's unlocked step nodes to match {@code sup}. */
    static void reconcile(ServerPlayer player, PlayerSkillData data, SkillTree tree, boolean sup) {
        AttributeInstance inst = stepInstance(player);
        if (inst == null) return;
        for (String id : data.unlocked) {
            SkillNode node = tree.byId(id);
            if (node == null) continue;
            for (int i = 0; i < node.effects.size(); i++) {
                SkillEffect e = node.effects.get(i);
                if (!"attribute".equals(e.type) || !STEP_ATTR.equals(e.attribute)) continue;
                Identifier modId = SkillEffects.modifierId(node.id, i);
                if (sup) {
                    inst.removeModifier(modId);
                } else {
                    inst.addOrUpdateTransientModifier(new AttributeModifier(
                            modId, e.amount, AttributeModifier.Operation.ADD_VALUE));
                }
            }
        }
    }

    /** Force a re-evaluation next tick (e.g. right after the toggle command changes the flag). */
    public static void invalidate(UUID uuid) {
        lastSuppressed.remove(uuid);
    }

    public static void onLeave(UUID uuid) {
        lastSuppressed.remove(uuid);
    }

    private static AttributeInstance stepInstance(ServerPlayer player) {
        Identifier id = Identifier.tryParse(STEP_ATTR);
        if (id == null) return null;
        Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.get(id);
        return holder.map(player::getAttribute).orElse(null);
    }
}
