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
    public Set<String> creditedAdvancements = new LinkedHashSet<>();
    public boolean initialized = false;

    // Bounty board progress for the current rotation (slot 0-2 -> kills / claimed).
    public long questRotation = -1;
    public Map<Integer, Integer> questKills = new HashMap<>();
    public Set<Integer> questClaimed = new LinkedHashSet<>();

    // Starter (personal) bounty board: new players complete 15 quests to graduate to the universal board.
    public int questsCompleted = 0;
    public boolean graduated = false;
    public int[] starterSlots = new int[0]; // this rotation's 3 personal quest indices (pre-graduation)

    public void normalize() {
        if (unlocked == null) unlocked = new LinkedHashSet<>();
        if (creditedAdvancements == null) creditedAdvancements = new LinkedHashSet<>();
        if (questKills == null) questKills = new HashMap<>();
        if (questClaimed == null) questClaimed = new LinkedHashSet<>();
        if (starterSlots == null) starterSlots = new int[0];
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
