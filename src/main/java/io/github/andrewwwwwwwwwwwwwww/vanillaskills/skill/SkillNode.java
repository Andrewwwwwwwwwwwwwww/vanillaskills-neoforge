package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import java.util.ArrayList;
import java.util.List;

/**
 * One node in the skill tree. Deserialized directly from skilltree.json by Gson.
 */
public class SkillNode {
    public String id;
    public String title;
    public List<String> description = new ArrayList<>();
    public String icon = "minecraft:stone";
    public int cost = 1;
    public int minEarned = 0;  // requires this many lifetime-earned Skill Shards before it can be unlocked
    public String currency = "skill"; // "skill" = Skill Shards, "quest" = Quest Shards
    public List<String> requires = new ArrayList<>();

    public boolean isQuestCurrency() {
        return "quest".equals(currency);
    }
    public String category;   // which lane this node belongs to (null -> default lane)
    public int slot;          // position within its lane's view
    public List<SkillEffect> effects = new ArrayList<>();

    public SkillNode() {}

    public SkillNode(String id, String title, String category, int slot, int cost, String icon) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.slot = slot;
        this.cost = cost;
        this.icon = icon;
    }

    /** Replace any null collections left by Gson with empty lists. */
    public void normalize() {
        if (description == null) description = new ArrayList<>();
        if (requires == null) requires = new ArrayList<>();
        if (effects == null) effects = new ArrayList<>();
        if (title == null) title = id;
        if (icon == null) icon = "minecraft:stone";
        if (currency == null) currency = "skill";
    }
}
