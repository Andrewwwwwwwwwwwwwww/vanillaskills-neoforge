package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-player progression, stored at world/vanillaskills/players/&lt;uuid&gt;.json.
 */
public class PlayerSkillData {
    public int version = 1;
    public Set<String> unlocked = new LinkedHashSet<>();
    public int pointsAvailable = 0; // Skill Shards available to spend
    public int pointsEarned = 0;    // Skill Shards earned lifetime
    public int questShardsAvailable = 0; // Quest Shards available to spend in the shop
    public int questShardsEarned = 0;    // Quest Shards earned lifetime
    public float lastHealth = -1f; // health at last logout, restored after max-health modifiers reapply
    public boolean completionRewarded = false; // got the full-tree Dragon Ingot reward
    public boolean nightVisionDisabled = false; // /skill toggle nightvision (only relevant once unlocked)
    public boolean stepUpDisabled = false;      // /skill toggle stepup — also auto-suppressed while sneaking
    public Set<String> creditedAdvancements = new LinkedHashSet<>();
    public boolean initialized = false;

    // Bounty board progress for the current rotation (slot 0-2 -> kills / claimed).
    public long questRotation = -1;
    public Map<Integer, Integer> questKills = new HashMap<>();
    public Set<Integer> questClaimed = new LinkedHashSet<>();
    // Repeatable STAT quests: baseline stat value snapshotted when the quest appears (keyed by the
    // active slot 0-2), so progress only counts what you do DURING the current rotation. Reset each roll.
    public Map<Integer, Long> questStatBase = new HashMap<>();

    // One-time Feats (structure discoveries, boss kills, entering the End). Permanent; never rotation-reset.
    public Set<String> featsDone = new LinkedHashSet<>();

    // Starter board: new players complete ALL fixed starter quests (QuestPool.STARTER) to graduate
    // to the universal rotating board. Starter progress never rotation-resets.
    public int questsCompleted = 0;
    public boolean graduated = false;
    public int[] starterSlots = new int[0]; // LEGACY (pre-1.2.0 random starter board); unused, kept for old saves
    public Set<Integer> starterDone = new LinkedHashSet<>();      // claimed starter indices (one-time)
    public Map<Integer, Integer> starterKills = new HashMap<>();  // kill progress per starter index
    public int starterVersion = 0; // 2 = fixed-starter system (1.2.0 migration marker)

    public void normalize() {
        if (unlocked == null) unlocked = new LinkedHashSet<>();
        if (creditedAdvancements == null) creditedAdvancements = new LinkedHashSet<>();
        if (questKills == null) questKills = new HashMap<>();
        if (questClaimed == null) questClaimed = new LinkedHashSet<>();
        if (questStatBase == null) questStatBase = new HashMap<>();
        if (featsDone == null) featsDone = new LinkedHashSet<>();
        if (starterSlots == null) starterSlots = new int[0];
        if (starterDone == null) starterDone = new LinkedHashSet<>();
        if (starterKills == null) starterKills = new HashMap<>();
    }

    public boolean hasUnlocked(String id) {
        return unlocked.contains(id);
    }

    public void grantPoints(int amount) {
        pointsAvailable += amount;
        pointsEarned += amount;
    }

    public void grantQuestShards(int amount) {
        questShardsAvailable += amount;
        questShardsEarned += amount;
    }
}
