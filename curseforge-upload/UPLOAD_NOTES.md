# VanillaSkills — CurseForge Upload (1.0.7)

Two separate uploads, two separate CurseForge projects.

## 1. Mod jar  →  the VanillaSkills **mod** project
**File:** `vanillaskills-1.0.7+mc26.2.jar`
- Minecraft **26.2**, **Fabric** loader (>= 0.19.3)
- **Required dependency:** Fabric API (>= 0.152.1+26.2)
- Environment: server-side, vanilla clients OK
- **Textures are bundled in this jar** — anyone who installs the mod sees the full custom
  look (armor/tools/spears, worn armor, steel shield, materials) automatically. No resource
  pack needed for modded clients.

## 2. Texture pack  →  a **separate** CurseForge resource-pack project
**File:** `VanillaSkills-TexturePack.zip`
- Resource pack format range covers **MC 26.1.2 and 26.2** (`min_format [84,0]` / `max_format 88`).
- Purpose: deliver the textures to **vanilla clients connecting to a server** that runs the mod
  (those clients don't have the jar, so they need the pack).
- Two ways to deliver it to players:
  - **Server-required (recommended):** host the zip at a direct-download URL and set in
    `server.properties`:
    ```
    resource-pack=<direct download URL to the zip>
    require-resource-pack=true
    ```
    (a GitHub release asset is an easy stable URL.) Players are prompted to download on join.
  - **Manual:** players drop the zip in `.minecraft/resourcepacks/` and enable it.

## Deploy note
When updating a live server, update the **jar and the pack together** — the worn-armor and shield
visuals depend on both the mod (which stamps the items) and the textures being present.
