package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import java.util.List;

/** The pool of possible bounty-board quests; 3 are picked each rotation. */
public final class QuestPool {
    private QuestPool() {}

    /**
     * The FIXED starter board: all 15 always active for new players, each completable once. Finishing
     * all of them graduates the player to the rotating shared board below. A progression arc — wood,
     * food, the mining ladder, first combat, mob drops, and finally the mod's own skill tree. All
     * targets are biome-agnostic (e.g. sticks instead of a specific wood's log).
     * EDIT-IN-PLACE ONLY: starter progress is saved as indices into THIS list — never reorder,
     * insert, or GROW it. The starter GUI ({@code QuestMenu.STARTER_SLOTS}) renders exactly this
     * many slots and graduation requires claiming every index, so an appended quest would be
     * unrenderable and would make graduation impossible. To change a quest, replace its entry.
     */
    public static final List<Quest> STARTER = List.of(
            new Quest(Quest.Type.GATHER, "minecraft:stick", 32, 1, "Gather 32 Sticks"),
            new Quest(Quest.Type.GATHER, "minecraft:cobblestone", 64, 1, "Gather 64 Cobblestone"),
            new Quest(Quest.Type.GATHER, "minecraft:coal", 16, 1, "Gather 16 Coal"),
            new Quest(Quest.Type.GATHER, "minecraft:bread", 16, 1, "Bake 16 Bread"),
            new Quest(Quest.Type.GATHER, "minecraft:leather", 16, 3, "Gather 16 Leather"),
            new Quest(Quest.Type.GATHER, "minecraft:copper_ingot", 32, 4, "Smelt 32 Copper Ingots"),
            new Quest(Quest.Type.GATHER, "minecraft:iron_ingot", 16, 4, "Smelt 16 Iron Ingots"),
            new Quest(Quest.Type.GATHER, "minecraft:gold_ingot", 8, 5, "Smelt 8 Gold Ingots"),
            new Quest(Quest.Type.GATHER, "minecraft:diamond", 4, 6, "Gather 4 Diamonds"),
            new Quest(Quest.Type.KILL, "minecraft:zombie", 10, 3, "Slay 10 Zombies"),
            new Quest(Quest.Type.KILL, "minecraft:skeleton", 10, 3, "Slay 10 Skeletons"),
            new Quest(Quest.Type.KILL, "minecraft:creeper", 5, 4, "Slay 5 Creepers"),
            new Quest(Quest.Type.GATHER, "minecraft:bone", 16, 2, "Gather 16 Bones"),
            new Quest(Quest.Type.GATHER, "minecraft:string", 8, 2, "Gather 8 String"),
            new Quest(Quest.Type.SKILL, "", 10, 10, "Unlock 10 Skills")
    );

    // Rewards are in Quest Shards, tiered by difficulty (easy 4-5, medium 6-7, hard 8-9, elite 10-16
    // for rare/grindy targets) so a full board of 3 quests yields ~14-26 Quest Shards.
    // weight 10 = normal (diamond is rarer at 4); lategame=true quests are hidden in the early-game window.
    public static final List<Quest> ALL = List.of(
            // ---- Gather (turned in at the board) ----
            new Quest(Quest.Type.GATHER, "minecraft:iron_ingot", 32, 5, "Gather 32 Iron Ingots"),
            new Quest(Quest.Type.GATHER, "minecraft:gold_ingot", 16, 5, "Gather 16 Gold Ingots"),
            new Quest(Quest.Type.GATHER, "minecraft:diamond", 10, 15, "Gather 10 Diamonds", 4, false),
            new Quest(Quest.Type.GATHER, "minecraft:copper_ingot", 64, 6, "Gather 64 Copper Ingots"),
            new Quest(Quest.Type.GATHER, "minecraft:wheat", 64, 4, "Gather 64 Wheat"),
            new Quest(Quest.Type.GATHER, "minecraft:leather", 24, 8, "Gather 24 Leather"),
            new Quest(Quest.Type.GATHER, "minecraft:gunpowder", 16, 6, "Gather 16 Gunpowder"),
            new Quest(Quest.Type.GATHER, "minecraft:redstone", 32, 5, "Gather 32 Redstone"),
            new Quest(Quest.Type.GATHER, "minecraft:emerald", 32, 10, "Gather 32 Emeralds", 10, true),
            new Quest(Quest.Type.GATHER, "minecraft:coal", 64, 4, "Gather 64 Coal"),
            new Quest(Quest.Type.GATHER, "minecraft:ender_pearl", 8, 8, "Gather 8 Ender Pearls", 10, true),
            new Quest(Quest.Type.GATHER, "minecraft:bone", 32, 4, "Gather 32 Bones"),

            // ---- Fishing (catch & turn in) ----
            // Half weight (5): fishing is one activity with four entries, so at full weight it
            // dominated boards (~half of starter rotations had a fish quest).
            new Quest(Quest.Type.GATHER, "minecraft:cod", 16, 5, "Catch 16 Cod", 5, false),
            new Quest(Quest.Type.GATHER, "minecraft:salmon", 16, 5, "Catch 16 Salmon", 5, false),
            new Quest(Quest.Type.GATHER, "minecraft:tropical_fish", 6, 8, "Catch 6 Tropical Fish", 5, false),
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
            new Quest(Quest.Type.KILL, "minecraft:witch", 8, 12, "Slay 8 Witches", 10, true),
            new Quest(Quest.Type.KILL, Quest.ANY_HOSTILE, 50, 6, "Slay 50 hostile mobs"),

            // ---- Freebie (instant, rare) ----
            new Quest(Quest.Type.FREEBIE, "", 1, 3, "Daily Bonus — free Quest Shards", 3, false),

            // ---- 1.1.1 additions ----
            // APPEND ONLY: boards persist quests as indices into this list, so new entries must go
            // at the end — inserting above would silently remap players' saved active quests.
            new Quest(Quest.Type.GATHER, "minecraft:carrot", 64, 4, "Gather 64 Carrots"),
            new Quest(Quest.Type.GATHER, "minecraft:honey_bottle", 4, 7, "Gather 4 Honey Bottles"),
            new Quest(Quest.Type.GATHER, "minecraft:amethyst_shard", 24, 4, "Gather 24 Amethyst Shards"),
            new Quest(Quest.Type.GATHER, "minecraft:string", 32, 4, "Gather 32 String"),
            new Quest(Quest.Type.KILL, "minecraft:slime", 15, 5, "Slay 15 Slimes"),
            new Quest(Quest.Type.KILL, "minecraft:pillager", 10, 6, "Slay 10 Pillagers"),
            new Quest(Quest.Type.KILL, "minecraft:guardian", 8, 8, "Slay 8 Guardians", 10, true),

            // ---- 1.2.8 additions ---- (APPEND ONLY — see note above)
            // Cultivator crops: give the expanded farming skill something to turn in.
            new Quest(Quest.Type.GATHER, "minecraft:pumpkin", 64, 8, "Gather 64 Pumpkins"),
            new Quest(Quest.Type.GATHER, "minecraft:melon_slice", 64, 5, "Gather 64 Melon Slices"),
            new Quest(Quest.Type.GATHER, "minecraft:sugar_cane", 32, 4, "Gather 32 Sugar Cane"),
            new Quest(Quest.Type.GATHER, "minecraft:sweet_berries", 16, 5, "Gather 16 Sweet Berries"),
            new Quest(Quest.Type.GATHER, "minecraft:cocoa_beans", 16, 6, "Gather 16 Cocoa Beans"),
            new Quest(Quest.Type.GATHER, "minecraft:nether_wart", 32, 7, "Gather 32 Nether Wart", 10, true),
            new Quest(Quest.Type.GATHER, "minecraft:chorus_fruit", 8, 8, "Gather 8 Chorus Fruit", 10, true),

            // Nether identity (all lategame — hidden during the early-game window).
            new Quest(Quest.Type.GATHER, "minecraft:blaze_rod", 16, 8, "Gather 16 Blaze Rods", 10, true),
            new Quest(Quest.Type.GATHER, "minecraft:quartz", 32, 4, "Gather 32 Nether Quartz", 10, true),
            new Quest(Quest.Type.GATHER, "minecraft:ghast_tear", 4, 16, "Gather 4 Ghast Tears", 10, true),
            new Quest(Quest.Type.GATHER, "minecraft:magma_cream", 8, 7, "Gather 8 Magma Cream", 10, true),
            new Quest(Quest.Type.GATHER, "minecraft:ancient_debris", 1, 9, "Gather 1 Ancient Debris", 10, true),

            // Ocean identity.
            new Quest(Quest.Type.GATHER, "minecraft:prismarine_shard", 24, 6, "Gather 24 Prismarine Shards"),
            new Quest(Quest.Type.GATHER, "minecraft:nautilus_shell", 3, 8, "Gather 3 Nautilus Shells"),
            new Quest(Quest.Type.GATHER, "minecraft:kelp", 32, 4, "Gather 32 Kelp"),
            new Quest(Quest.Type.GATHER, "minecraft:ink_sac", 8, 4, "Gather 8 Ink Sacs"),

            // Mining — raw drops (rewards the dig, ties to Prospector).
            new Quest(Quest.Type.GATHER, "minecraft:raw_iron", 32, 6, "Gather 32 Raw Iron"),
            new Quest(Quest.Type.GATHER, "minecraft:deepslate", 48, 4, "Gather 48 Deepslate"),
            new Quest(Quest.Type.GATHER, "minecraft:obsidian", 8, 4, "Gather 8 Obsidian"),
            new Quest(Quest.Type.GATHER, "minecraft:lapis_lazuli", 24, 4, "Gather 24 Lapis Lazuli"),

            // ---- 1.3.0 additions: repeatable STAT quests ---- (APPEND ONLY)
            // Progress counts from the moment the quest is dealt (baseline snapshot per rotation), so
            // veterans don't auto-complete and you must do it within the ~5h window. Distance stats are
            // in cm (walk_one_cm etc.); the amount is in BLOCKS and the reader divides by 100.
            new Quest(Quest.Type.STAT, "minecraft:walk_one_cm,minecraft:sprint_one_cm", 5000, 6, "Travel 5,000 blocks on foot"),
            new Quest(Quest.Type.STAT, "minecraft:swim_one_cm", 1500, 7, "Swim 1,500 blocks"),
            new Quest(Quest.Type.STAT, "minecraft:jump", 800, 3, "Jump 800 times")
    );
}
