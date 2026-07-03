package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The shared bounty board: 3 active quests that re-roll on a timer (default every 5 hours). Persisted
 * to world/vanillaskills/questboard.json so it survives restarts.
 */
public class QuestBoard {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int ACTIVE_COUNT = 3;
    private final Random random = new Random();

    private State state = new State();

    /** Serialized board state. */
    private static class State {
        long rotationId = 0;
        long nextRotationMs = 0;
        int[] activeIndices = new int[0];
    }

    public long rotationId() {
        return state.rotationId;
    }

    public long nextRotationMs() {
        return state.nextRotationMs;
    }

    /** The 3 currently-active quests (by their index in the active list). */
    public List<Quest> active() {
        List<Quest> out = new ArrayList<>();
        for (int idx : state.activeIndices) {
            if (idx >= 0 && idx < QuestPool.ALL.size()) out.add(QuestPool.ALL.get(idx));
        }
        return out;
    }

    public Quest active(int i) {
        List<Quest> a = active();
        return (i >= 0 && i < a.size()) ? a.get(i) : null;
    }

    public void load() {
        Path path = path();
        try {
            if (Files.exists(path)) {
                State loaded = GSON.fromJson(Files.readString(path), State.class);
                if (loaded != null) state = loaded;
            }
        } catch (Exception e) {
            VanillaSkills.LOGGER.error("Failed to load questboard.json", e);
        }
        long now = System.currentTimeMillis();
        if (state.activeIndices.length != ACTIVE_COUNT || state.nextRotationMs <= now) {
            reroll(now);
        }
    }

    public void tick(MinecraftServer server) {
        if (System.currentTimeMillis() >= state.nextRotationMs) {
            reroll(System.currentTimeMillis());
            server.getPlayerList().getPlayers().forEach(p -> p.sendSystemMessage(Component.literal(
                    "New bounties are available! Use /quests").withStyle(ChatFormatting.GOLD)));
        }
    }

    /** Force a fresh rotation right now (op/testing). */
    public void forceReroll() {
        reroll(System.currentTimeMillis());
    }

    private void reroll(long now) {
        state.rotationId++;
        state.nextRotationMs = now + GameplayConfig.BOUNTY_REFRESH_MS;
        state.activeIndices = pickDistinct(ACTIVE_COUNT);
        save();
    }

    private int[] pickDistinct(int count) {
        // The universal board draws from the full pool (early-game gating now lives on the starter board).
        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < QuestPool.ALL.size(); i++) {
            pool.add(i);
        }
        // Weighted distinct sampling (rarer quests like the freebie have lower weight).
        List<Integer> chosen = new ArrayList<>();
        int n = Math.min(count, pool.size());
        for (int k = 0; k < n; k++) {
            int total = 0;
            for (int idx : pool) total += Math.max(1, QuestPool.ALL.get(idx).weight());
            int r = random.nextInt(total);
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

    public void save() {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(state));
        } catch (Exception e) {
            VanillaSkills.LOGGER.error("Failed to save questboard.json", e);
        }
    }

    private static Path path() {
        MinecraftServer server = VanillaSkills.server;
        return server.getWorldPath(LevelResource.ROOT).resolve("vanillaskills").resolve("questboard.json");
    }
}
