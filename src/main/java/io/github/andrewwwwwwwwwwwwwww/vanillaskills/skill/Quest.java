package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

/**
 * One bounty-board quest: gather a quantity of an item (turned in at the board), kill a number of a
 * mob type, a FREEBIE (instant free Quest Shards), SKILL (have unlocked N skill-tree nodes — used by
 * the starter board to teach the mod), or STAT (accumulate a vanilla statistic — e.g. distance walked
 * or jumps — counted from when the quest appeared, so it is repeatable each rotation). Rewards Quest Shards.
 *
 * @param target   item id (GATHER), entity-type id (KILL) or the literal "any_hostile", or a
 *                 comma-separated list of custom-stat ids (STAT, e.g. "minecraft:walk_one_cm");
 *                 unused for FREEBIE and SKILL.
 * @param weight   relative likelihood of being picked for the board (higher = more common).
 * @param lategame LEGACY (pre-1.2.0 starter filtering) — no longer read; kept as documentation of
 *                 which quests are late-game themed.
 */
public record Quest(Type type, String target, int amount, int reward, String title, int weight, boolean lategame) {
    public enum Type { GATHER, KILL, FREEBIE, SKILL, STAT }

    public static final String ANY_HOSTILE = "any_hostile";

    /** Convenience: a normal-weight (10), non-lategame quest. */
    public Quest(Type type, String target, int amount, int reward, String title) {
        this(type, target, amount, reward, title, 10, false);
    }
}
