package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.List;

/**
 * One-time Feats: boss kills, structure discoveries, and entering the End. All server-side. Boss kills
 * are pushed from the kill handler; discoveries and dimension entry are polled on a throttled server
 * tick (every 2s, only for feats a player hasn't earned, only in the relevant dimension).
 */
public final class Feats {
    private Feats() {}

    private static final int CHECK_INTERVAL_TICKS = 40; // poll discovery/dimension feats every ~2s

    public static final List<Feat> ALL = List.of(
            // ---- Bosses ----
            new Feat("dragonslayer", Feat.Type.KILL, "minecraft:ender_dragon", null, 30,
                    "minecraft:dragon_head", "Dragonslayer", "Defeat the Ender Dragon"),
            new Feat("wither_down", Feat.Type.KILL, "minecraft:wither", null, 25,
                    "minecraft:nether_star", "Wither Be Gone", "Defeat the Wither"),
            new Feat("warden_down", Feat.Type.KILL, "minecraft:warden", null, 25,
                    "minecraft:echo_shard", "Silence the Dark", "Defeat a Warden"),
            // ---- The End (Hungering Portal ritual complete = first entry) ----
            new Feat("the_end", Feat.Type.DIMENSION, "minecraft:the_end", null, 20,
                    "minecraft:ender_eye", "Beyond the Portal", "Enter The End — the Hungering Portal ritual complete"),
            // ---- Overworld discoveries ----
            new Feat("ancient_city", Feat.Type.DISCOVER, "minecraft:ancient_city", "minecraft:overworld", 15,
                    "minecraft:sculk_catalyst", "Deep Dark Delver", "Discover an Ancient City"),
            new Feat("mansion", Feat.Type.DISCOVER, "minecraft:mansion", "minecraft:overworld", 15,
                    "minecraft:totem_of_undying", "Woodland Wanderer", "Discover a Woodland Mansion"),
            new Feat("trial_chambers", Feat.Type.DISCOVER, "minecraft:trial_chambers", "minecraft:overworld", 12,
                    "minecraft:trial_key", "Trial by Combat", "Discover a Trial Chamber"),
            new Feat("monument", Feat.Type.DISCOVER, "minecraft:monument", "minecraft:overworld", 12,
                    "minecraft:prismarine", "Monument Raider", "Discover an Ocean Monument"),
            // ---- Nether discoveries ----
            new Feat("bastion", Feat.Type.DISCOVER, "minecraft:bastion_remnant", "minecraft:the_nether", 12,
                    "minecraft:gilded_blackstone", "Bastion Breacher", "Discover a Bastion Remnant"),
            new Feat("fortress", Feat.Type.DISCOVER, "minecraft:fortress", "minecraft:the_nether", 10,
                    "minecraft:nether_bricks", "Fortress Found", "Discover a Nether Fortress"),
            // ---- End discoveries ----
            new Feat("end_city", Feat.Type.DISCOVER, "minecraft:end_city", "minecraft:the_end", 12,
                    "minecraft:shulker_shell", "City in the Sky", "Discover an End City")
    );

    public static boolean isDone(ServerPlayer player, String id) {
        return VanillaSkills.PLAYERS.get(player.getUUID()).featsDone.contains(id);
    }

    private static void award(ServerPlayer player, Feat feat) {
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        if (!data.featsDone.add(feat.id())) return; // already earned
        VanillaSkills.PLAYERS.addQuestShards(player, feat.reward()); // also persists the data
        player.sendSystemMessage(Component.literal("★ Feat unlocked: " + feat.title() + "  +"
                + feat.reward() + " Quest Shards").withStyle(ChatFormatting.GOLD));
    }

    /** Boss-kill feats — call from the entity-kill handler with the player's victim. */
    public static void onKill(ServerPlayer killer, Entity dead) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()).toString();
        for (Feat f : ALL) {
            if (f.type() == Feat.Type.KILL && f.target().equals(id) && !isDone(killer, f.id())) {
                award(killer, f);
            }
        }
    }

    /** Throttled poll for discovery + dimension feats; also refreshes STAT-quest baselines. */
    public static void serverTick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Quests.sync(p);              // captures STAT-quest baselines at rotation start / on join
            checkLocationFeats(p);
        }
    }

    private static void checkLocationFeats(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        String dim = level.dimension().identifier().toString();
        BlockPos pos = player.blockPosition();
        for (Feat f : ALL) {
            if (data.featsDone.contains(f.id())) continue;
            switch (f.type()) {
                case DIMENSION -> { if (dim.equals(f.target())) award(player, f); }
                case DISCOVER -> {
                    if (f.dimension() != null && !f.dimension().equals(dim)) continue; // wrong dimension, skip cheaply
                    Identifier loc = Identifier.tryParse(f.target());
                    if (loc == null) continue;
                    ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, loc);
                    Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                            .get(key).map(Holder::value).orElse(null);
                    if (structure == null) continue;
                    StructureStart start = level.structureManager().getStructureAt(pos, structure);
                    if (start.isValid()) award(player, f);
                }
                default -> { /* KILL feats are pushed from onKill, not polled */ }
            }
        }
    }
}
