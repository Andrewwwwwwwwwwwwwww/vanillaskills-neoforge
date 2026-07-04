package io.github.andrewwwwwwwwwwwwwww.vanillaskills;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side Wind Burst launch, working around a vanilla 26.2 bug where the mace's Wind Burst
 * enchantment never launches the attacker (the smash lands, but the explosion knockback is lost in
 * the new client explosion path — reproduced in a fresh unmodded world).
 *
 * <p>Called from the post-damage hook on each loader. Mirrors the vanilla enchantment definition:
 * requires a mace in main hand, fall distance >= 1.5 at hit time, not creative-flying, and scales
 * with the enchant level using wind_burst.json's own knockback multipliers (1.2 / 1.75 / 2.2).
 * Applies the launch as a direct velocity push (the same mechanism as the Dragon dash), which
 * bypasses the broken explosion-packet path entirely. Gated by {@code windBurstFix} in
 * gameplay.json (default on) so it can be turned off if a Minecraft update fixes the bug.
 */
public final class WindBurstFix {
    private WindBurstFix() {}

    /** Vanilla wind_burst.json knockback multipliers per level (I / II / III). */
    private static final float[] STRENGTH = {1.2f, 1.75f, 2.2f};
    /** Upward velocity per multiplier point — tuned to land near vanilla launch heights. */
    private static final double LAUNCH_SCALE = 0.9;

    /** Call after a melee hit has dealt damage. No-op unless every Wind Burst condition holds. */
    public static void onMeleeDamage(ServerPlayer attacker, float damageDealt) {
        if (!GameplayConfig.WIND_BURST_FIX || damageDealt <= 0.0f) return;
        if (!attacker.getMainHandItem().is(Items.MACE)) return;
        if (attacker.fallDistance < 1.5 || attacker.getAbilities().flying) return;

        int level = windBurstLevel(attacker);
        if (level <= 0) return;

        double strength = STRENGTH[Math.min(level, STRENGTH.length) - 1];
        Vec3 motion = attacker.getDeltaMovement();
        attacker.setDeltaMovement(motion.x, strength * LAUNCH_SCALE, motion.z);
        attacker.hurtMarked = true; // force the velocity packet to the client
        attacker.fallDistance = 0.0; // fresh chain: the next smash needs new fall height

        ServerLevel level0 = attacker.level();
        level0.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
                attacker.getX(), attacker.getY(), attacker.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        level0.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private static int windBurstLevel(LivingEntity attacker) {
        Holder<Enchantment> windBurst = attacker.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.WIND_BURST);
        return EnchantmentHelper.getEnchantmentLevel(windBurst, attacker);
    }
}
