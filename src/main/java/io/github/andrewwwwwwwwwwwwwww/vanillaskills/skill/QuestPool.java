package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import java.util.List;

/** The pool of possible bounty-board quests; 3 are picked each rotation. */
public final class QuestPool {
    private QuestPool() {}

    // Rewards are in Quest Shards, tiered by difficulty (easy 4-5, medium 6-7, hard 8-9) so a full
    // board of 3 quests yields ~16-22 Quest Shards — roughly half the daily shop.
    // weight 10 = normal; lategame=true quests are hidden during the early-game "noob" window.
    public static final List<Quest> ALL = List.of(
            // ---- Gather (turned in at the board) ----
            new Quest(Quest.Type.GATHER, "minecraft:iron_ingot", 32, 5, "Gather 32 Iron Ingots"),
            new Quest(Quest.Type.GATHER, "minecraft:gold_ingot", 16, 5, "Gather 16 Gold Ingots"),
            new Quest(Quest.Type.GATHER, "minecraft:diamond", 10, 9, "Gather 10 Diamonds"),
            new Quest(Quest.Type.GATHER, "minecraft:copper_ingot", 64, 4, "Gather 64 Copper Ingots"),
            new Quest(Quest.Type.GATHER, "minecraft:wheat", 64, 4, "Gather 64 Wheat"),
            new Quest(Quest.Type.GATHER, "minecraft:leather", 24, 5, "Gather 24 Leather"),
            new Quest(Quest.Type.GATHER, "minecraft:gunpowder", 16, 6, "Gather 16 Gunpowder"),
            new Quest(Quest.Type.GATHER, "minecraft:redstone", 32, 5, "Gather 32 Redstone"),
            new Quest(Quest.Type.GATHER, "minecraft:emerald", 8, 7, "Gather 8 Emeralds", 10, true),
            new Quest(Quest.Type.GATHER, "minecraft:coal", 64, 4, "Gather 64 Coal"),
            new Quest(Quest.Type.GATHER, "minecraft:ender_pearl", 8, 8, "Gather 8 Ender Pearls", 10, true),
            new Quest(Quest.Type.GATHER, "minecraft:bone", 32, 4, "Gather 32 Bones"),

            // ---- Fishing (catch & turn in) ----
            // Half weight (5): fishing is one activity with four entries, so at full weight it
            // dominated boards (~half of starter rotations had a fish quest).
            new Quest(Quest.Type.GATHER, "minecraft:cod", 16, 5, "Catch 16 Cod", 5, false),
            new Quest(Quest.Type.GATHER, "minecraft:salmon", 16, 5, "Catch 16 Salmon", 5, false),
            new Quest(Quest.Type.GATHER, "minecraft:tropical_fish", 6, 7, "Catch 6 Tropical Fish", 5, false),
            new Quest(Quest.Type.GATHER, "minecraft:pufferfish", 4, 6, "Catch 4 Pufferfish", 5, false),

            // ---- Kill ----
            new Quest(Quest.Type.KILL, "minecraft:zombie", 25, 5, "Slay 25 Zombies"),
            new Quest(Quest.Type.KILL, "minecraft:skeleton", 25, 5, "Slay 25 Skeletons"),
            new Quest(Quest.Type.KILL, "minecraft:creeper", 15, 6, "Slay 15 Creepers"),
            new Quest(Quest.Type.KILL, "minecraft:spider", 20, 5, "Slay 20 Spiders"),
            new Quest(Quest.Type.KILL, "minecraft:enderman", 10, 8, "Slay 10 Endermen", 10, true),
            new Quest(Quest.Type.KILL, "minecraft:blaze", 8, 8, "Slay 8 Blazes", 10, true),
            new Quest(Quest.Type.KILL, "minecraft:piglin", 15, 6, "Slay 15 Piglins", 10, true),
            new Quest(Quest.Type.KILL, "minecraft:drowned", 15, 6, "Slay 15 Drowned", 10, true),
            new Quest(Quest.Type.KILL, "minecraft:witch", 8, 6, "Slay 8 Witches", 10, true),
            new Quest(Quest.Type.KILL, Quest.ANY_HOSTILE, 50, 7, "Slay 50 hostile mobs"),

            // ---- Freebie (instant, rare) ----
            new Quest(Quest.Type.FREEBIE, "", 1, 3, "Daily Bonus — free Quest Shards", 3, false),

            // ---- 1.1.1 additions ----
            // APPEND ONLY: boards persist quests as indices into this list, so new entries must go
            // at the end — inserting above would silently remap players' saved active quests.
            new Quest(Quest.Type.GATHER, "minecraft:carrot", 64, 4, "Gather 64 Carrots"),
            new Quest(Quest.Type.GATHER, "minecraft:honey_bottle", 4, 6, "Gather 4 Honey Bottles"),
            new Quest(Quest.Type.GATHER, "minecraft:amethyst_shard", 24, 5, "Gather 24 Amethyst Shards"),
            new Quest(Quest.Type.GATHER, "minecraft:string", 32, 4, "Gather 32 String"),
            new Quest(Quest.Type.KILL, "minecraft:slime", 15, 5, "Slay 15 Slimes"),
            new Quest(Quest.Type.KILL, "minecraft:pillager", 10, 6, "Slay 10 Pillagers"),
            new Quest(Quest.Type.KILL, "minecraft:guardian", 8, 8, "Slay 8 Guardians", 10, true)
    );
}
