package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Per-player bounty progress. New players work a personal "starter" board of early-game quests (drawn
 * from the non-lategame pool); after completing {@link #GRADUATE_AT} quests they graduate to the shared
 * universal board (all quests). Both boards share the same 5-hour rotation timer.
 */
public final class Quests {
    private Quests() {}

    /** Quests a new player must complete on the starter board before joining the universal board.
     *  Default 15 — set live from gameplay.json by GameplayConfig. */
    public static int GRADUATE_AT = 15;

    private static final Random RANDOM = new Random();

    /** Resets per-rotation progress when the board rolls over; (re)rolls the starter board for newbies. */
    public static void sync(ServerPlayer player) {
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        long rotation = VanillaSkills.QUESTS.rotationId();
        boolean changed = false;
        if (data.questRotation != rotation) {
            data.questRotation = rotation;
            data.questKills.clear();
            data.questClaimed.clear();
            if (!data.graduated) data.starterSlots = pickEarly(3);
            changed = true;
        } else if (!data.graduated && (data.starterSlots == null || data.starterSlots.length != 3)) {
            data.starterSlots = pickEarly(3);
            changed = true;
        }
        if (changed) VanillaSkills.PLAYERS.save(player.getUUID());
    }

    /** The 3 quests on the player's current board (universal if graduated, else their starter board). */
    public static List<Quest> activeFor(ServerPlayer player) {
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        if (data.graduated) return VanillaSkills.QUESTS.active();
        List<Quest> out = new ArrayList<>();
        for (int idx : data.starterSlots) {
            if (idx >= 0 && idx < QuestPool.ALL.size()) out.add(QuestPool.ALL.get(idx));
        }
        return out;
    }

    public static Quest questFor(ServerPlayer player, int index) {
        List<Quest> a = activeFor(player);
        return (index >= 0 && index < a.size()) ? a.get(index) : null;
    }

    public static boolean isGraduated(ServerPlayer player) {
        return VanillaSkills.PLAYERS.get(player.getUUID()).graduated;
    }

    public static int graduationProgress(ServerPlayer player) {
        return VanillaSkills.PLAYERS.get(player.getUUID()).questsCompleted;
    }

    /** Called when a player kills something — advances any matching active KILL quests on their board. */
    public static void onKill(ServerPlayer killer, Entity dead) {
        sync(killer);
        PlayerSkillData data = VanillaSkills.PLAYERS.get(killer.getUUID());
        List<Quest> active = activeFor(killer);
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()).toString();
        boolean hostile = dead instanceof Enemy;
        boolean changed = false;
        for (int i = 0; i < active.size(); i++) {
            Quest q = active.get(i);
            if (q.type() != Quest.Type.KILL || data.questClaimed.contains(i)) continue;
            boolean match = q.target().equals(Quest.ANY_HOSTILE) ? hostile : q.target().equals(id);
            if (!match) continue;
            int cur = data.questKills.getOrDefault(i, 0);
            if (cur >= q.amount()) continue;
            data.questKills.put(i, cur + 1);
            changed = true;
            if (cur + 1 == q.amount()) {
                killer.sendSystemMessage(Component.literal("Bounty ready to claim: " + q.title() + " — /quests")
                        .withStyle(ChatFormatting.GREEN));
            }
        }
        if (changed) VanillaSkills.PLAYERS.save(killer.getUUID());
    }

    public static boolean isClaimed(ServerPlayer player, int index) {
        return VanillaSkills.PLAYERS.get(player.getUUID()).questClaimed.contains(index);
    }

    /** Progress toward the quest (kills done, or items currently held), capped at the amount. */
    public static int progress(ServerPlayer player, int index) {
        Quest q = questFor(player, index);
        if (q == null) return 0;
        if (q.type() == Quest.Type.FREEBIE) return q.amount(); // always ready
        if (q.type() == Quest.Type.KILL) {
            return Math.min(q.amount(), VanillaSkills.PLAYERS.get(player.getUUID()).questKills.getOrDefault(index, 0));
        }
        return Math.min(q.amount(), countItem(player, q.target()));
    }

    /** Attempt to claim a quest's reward; messages the player with the result. */
    public static void claim(ServerPlayer player, int index) {
        sync(player);
        Quest q = questFor(player, index);
        if (q == null) return;
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        if (data.questClaimed.contains(index)) {
            player.sendSystemMessage(Component.literal("You've already claimed this bounty.").withStyle(ChatFormatting.RED));
            return;
        }
        if (q.type() == Quest.Type.KILL) {
            int cur = data.questKills.getOrDefault(index, 0);
            if (cur < q.amount()) {
                player.sendSystemMessage(Component.literal("Not done yet: " + cur + "/" + q.amount()).withStyle(ChatFormatting.RED));
                return;
            }
        } else if (q.type() == Quest.Type.GATHER) {
            int have = countItem(player, q.target());
            if (have < q.amount()) {
                player.sendSystemMessage(Component.literal("You need " + (q.amount() - have) + " more.").withStyle(ChatFormatting.RED));
                return;
            }
            removeItem(player, q.target(), q.amount());
        }
        // FREEBIE: nothing to verify or consume — just claim it.
        data.questClaimed.add(index);
        VanillaSkills.PLAYERS.addQuestShards(player, q.reward());
        player.sendSystemMessage(Component.literal("Bounty complete: " + q.title() + "  +" + q.reward()
                + " Quest Shard" + (q.reward() == 1 ? "" : "s")).withStyle(ChatFormatting.GOLD));

        if (!data.graduated) {
            data.questsCompleted++;
            if (data.questsCompleted >= GRADUATE_AT) {
                graduate(player, data);
            } else {
                player.sendSystemMessage(Component.literal("Starter board: " + data.questsCompleted + "/"
                        + GRADUATE_AT + " quests done.").withStyle(ChatFormatting.GRAY));
            }
        }
        VanillaSkills.PLAYERS.save(player.getUUID());
    }

    private static void graduate(ServerPlayer player, PlayerSkillData data) {
        data.graduated = true;
        data.questKills.clear();
        data.questClaimed.clear();
        data.questRotation = VanillaSkills.QUESTS.rotationId(); // start the universal board fresh
        player.sendSystemMessage(Component.literal("★ You've graduated to the main Bounty Board! "
                + "Every quest is now available.").withStyle(ChatFormatting.GOLD));
    }

    /** Op: force a player onto the universal board. */
    public static void forceGraduate(ServerPlayer player) {
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        data.questsCompleted = Math.max(data.questsCompleted, GRADUATE_AT);
        graduate(player, data);
        VanillaSkills.PLAYERS.save(player.getUUID());
    }

    /** Op: send a player back to the starter board. */
    public static void resetToStarter(ServerPlayer player) {
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        data.graduated = false;
        data.questsCompleted = 0;
        data.questKills.clear();
        data.questClaimed.clear();
        data.starterSlots = pickEarly(3);
        data.questRotation = VanillaSkills.QUESTS.rotationId();
        VanillaSkills.PLAYERS.save(player.getUUID());
    }

    /** 3 distinct random non-lategame quest indices, weighted (rarer quests like the freebie show less). */
    private static int[] pickEarly(int count) {
        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < QuestPool.ALL.size(); i++) {
            if (!QuestPool.ALL.get(i).lategame()) pool.add(i);
        }
        List<Integer> chosen = new ArrayList<>();
        int n = Math.min(count, pool.size());
        for (int k = 0; k < n; k++) {
            int total = 0;
            for (int idx : pool) total += Math.max(1, QuestPool.ALL.get(idx).weight());
            int r = RANDOM.nextInt(total);
            int removeAt = pool.size() - 1;
            for (int j = 0; j < pool.size(); j++) {
                r -= Math.max(1, QuestPool.ALL.get(pool.get(j)).weight());
                if (r < 0) { removeAt = j; break; }
            }
            chosen.add(pool.remove(removeAt));
        }
        int[] out = new int[chosen.size()];
        for (int i = 0; i < out.length; i++) out[i] = chosen.get(i);
        return out;
    }

    public static Item item(String id) {
        Identifier loc = Identifier.tryParse(id);
        if (loc == null) return Items.PAPER;
        return BuiltInRegistries.ITEM.get(loc).map(Holder::value).orElse(Items.PAPER);
    }

    private static int countItem(ServerPlayer player, String id) {
        Item item = item(id);
        int n = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item) && !Markers.isOurs(s)) n += s.getCount(); // don't count our marked variants
        }
        return n;
    }

    private static void removeItem(ServerPlayer player, String id, int amount) {
        Item item = item(id);
        int remaining = amount;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item) && !Markers.isOurs(s)) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
    }
}
