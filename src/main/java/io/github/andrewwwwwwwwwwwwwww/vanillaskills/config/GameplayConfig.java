package io.github.andrewwwwwwwwwwwwwww.vanillaskills.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.QuestShop;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Quests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Optional gameplay/pacing config, stored PER-WORLD at &lt;world&gt;/vanillaskills/gameplay.json so each
 * world (and each server) can have its own settings. Edit the file (or use the Mod Menu screen in a
 * loaded singleplayer world) and it applies on load / {@code /skill reload}, no cheats required.
 */
public class GameplayConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // --- Live values published to the rest of the mod on load() ---

    /** Read by {@code ItemEnchantmentsMutableMixin}. false (default) = Mending is stripped everywhere. */
    public static volatile boolean MENDING_ENABLED = false;

    private static final String DEFAULT_RP_URL =
            "https://github.com/Andrewwwwwwwwwwwwwww/vanillaskills/releases/download/v1.0.5/VanillaSkills-TexturePack.zip";
    private static final String DEFAULT_RP_SHA1 = "88e8d95b46367a7b4d6c17860ffe493b60e88d2a";

    /** When true, the server force-pushes the VanillaSkills texture pack to every joining client
     *  (so vanilla clients see the custom gear with no server.properties setup). Read on player join. */
    public static volatile boolean PUSH_RESOURCE_PACK = true;
    public static volatile String RESOURCE_PACK_URL = DEFAULT_RP_URL;
    public static volatile String RESOURCE_PACK_SHA1 = DEFAULT_RP_SHA1;
    /** Read by {@code QuestBoard} when re-rolling: ms between bounty rotations. */
    public static volatile long BOUNTY_REFRESH_MS = 5L * 3_600_000L;
    /** Read by {@code QuestShop}: ms between shop rotations. */
    public static volatile long SHOP_REFRESH_MS = 24L * 3_600_000L;
    // QuestShop.CONVERT_RATIO and Quests.GRADUATE_AT are pushed directly on load().

    // --- Persisted fields (gameplay.json) ---

    /** When true, Mending is available as normal; when false, the mod removes it everywhere. */
    public boolean mendingEnabled = false;
    /** Hours between bounty-board rotations (default 5). */
    public int bountyRefreshHours = 5;
    /** Hours between Quest Shop rotations (default 24). */
    public int shopRefreshHours = 24;
    /** Quest Shards needed per 1 Skill Shard at the converter (default 3). */
    public int convertRatio = 3;
    /** LEGACY (pre-1.2.0): graduation is now "complete every fixed starter quest" — this value is
     *  ignored; kept so old gameplay.json files still parse. */
    public int graduateAt = 15;
    /** Auto-push the VanillaSkills texture pack to joining clients (required). Default on. */
    public boolean serverResourcePack = true;
    /** Texture-pack download URL the server pushes (default = the GitHub release asset). */
    public String resourcePackUrl = DEFAULT_RP_URL;
    /** SHA-1 of that pack (lets clients cache it; update alongside the URL if you change the pack). */
    public String resourcePackSha1 = DEFAULT_RP_SHA1;

    private static Path path() {
        Path dir = VanillaSkills.worldDir();
        return dir == null ? null : dir.resolve("gameplay.json");
    }

    /** Load gameplay.json from the current world (writing a default file if absent) and publish its values. */
    public static GameplayConfig load() {
        Path path = path();
        GameplayConfig cfg = new GameplayConfig();
        if (path != null) {
            try {
                if (Files.exists(path)) {
                    GameplayConfig loaded = GSON.fromJson(Files.readString(path), GameplayConfig.class);
                    if (loaded != null) cfg = loaded;
                } else {
                    cfg.save();
                }
            } catch (Exception e) {
                VanillaSkills.LOGGER.error("Failed to load gameplay.json, using defaults", e);
                cfg = new GameplayConfig();
            }
        }
        cfg.apply();
        return cfg;
    }

    /** Publish this config's values to the live flags / consumers (clamped to sane minimums). */
    public void apply() {
        MENDING_ENABLED = mendingEnabled;
        BOUNTY_REFRESH_MS = Math.max(1, bountyRefreshHours) * 3_600_000L;
        SHOP_REFRESH_MS = Math.max(1, shopRefreshHours) * 3_600_000L;
        QuestShop.CONVERT_RATIO = Math.max(1, convertRatio);
        Quests.GRADUATE_AT = Math.max(1, graduateAt);
        PUSH_RESOURCE_PACK = serverResourcePack;
        RESOURCE_PACK_URL = resourcePackUrl == null ? "" : resourcePackUrl;
        RESOURCE_PACK_SHA1 = resourcePackSha1 == null ? "" : resourcePackSha1;
    }

    public void save() {
        Path path = path();
        if (path == null) return; // no world loaded
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this));
        } catch (IOException e) {
            VanillaSkills.LOGGER.error("Failed to save gameplay.json", e);
        }
    }
}
