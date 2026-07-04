# VanillaSkills — NeoForge edition

## [1.1.3+mc26.2-neoforge] - 2026-07-04
/skill nightvision toggle (parity with Fabric 1.1.3; 1.1.2 withdrawn).

## [1.1.1+mc26.2-neoforge] - 2026-07-03
7 new bounty quests + fishing quests at half weight (parity with Fabric 1.1.1).

## [1.1.0+mc26.2-neoforge] - 2026-07-03
Fortune Finder luck now boosts all natural container + vault loot (shared mixins; parity with Fabric 1.1.0).

## [1.0.9+mc26.2-neoforge] - 2026-07-03
Sweet + glow berry Cultivator bonus (shared harvest mixins; parity with Fabric 1.0.9).

## [1.0.8+mc26.2-neoforge] - 2026-07-03
Same Cultivator crop expansion as the Fabric 1.0.8 release (cocoa/melon/pumpkin-capped-2/sugar cane/cactus/chorus).

## [1.0.7+mc26.2-neoforge] - 2026-07-02
First NeoForge build. Feature-equal port of the Fabric 1.0.7 release to NeoForge 26.2.0.7-beta
(Minecraft 26.2, Java 25). Same textures, same mixins, same per-world config.

Port surface (everything else is shared, loader-neutral code):
- Fabric API events -> NeoForge bus (lifecycle, tick, join/leave/respawn, entity interact,
  block break, living death/damage, command registration)
- Registry.register -> DeferredRegister (recipe serializers, creative tab)
- Fabric loot API -> LootTableLoadEvent injection
- Fabric transitive access wideners -> META-INF/accesstransformer.cfg (Display/Interaction setters)
- fabric.mod.json -> META-INF/neoforge.mods.toml ([[mixins]] entries)
- Keybinds -> RegisterKeyMappingsEvent; client tick -> ClientTickEvent.Post
- Mod Menu integration dropped (Fabric-only); config screen class retained

Runtime-verified: dedicated server boots to Done, skill tree loads (118 nodes, P=2126),
mixins apply clean.
