package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

/**
 * One bounty-board quest: gather a quantity of an item (turned in at the board), kill a number of a
 * mob type, or a FREEBIE (instant free Quest Shards). Rewards Quest Shards.
 *
 * @param target   item id (GATHER) or entity-type id (KILL), or the literal "any_hostile".
 * @param weight   relative likelihood of being picked for the board (higher = more common).
 * @param lategame if true, this quest is hidden while the server is in its early-game "noob" window.
 */
public record Quest(Type type, String target, int amount, int reward, String title, int weight, boolean lategame) {
    public enum Type { GATHER, KILL, FREEBIE }

    public static final String ANY_HOSTILE = "any_hostile";

    /** Convenience: a normal-weight (10), non-lategame quest. */
    public Quest(Type type, String target, int amount, int reward, String title) {
        this(type, target, amount, reward, title, 10, false);
    }
}
