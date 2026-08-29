# VanillaSkills — NeoForge, MC 26.2 — CurseForge Upload (2.1.13)

Full details, including the breaking-release notice and the changelog to paste, are in
`../../vanillaskills/curseforge-upload/UPLOAD_NOTES.md`. Upload as an ADDITIONAL FILE on the existing
**Vanilla-Skills** project (`1570558`) — same project as the Fabric jars, different loader tag.

## Mod jar
**File:** `vanillaskills-2.1.13+mc26.2-neoforge.jar`
- Minecraft **26.2**, loader tag **NeoForge** (NOT "Forge")
- Requires NeoForge **26.2.0.7-beta or newer**; no Fabric API dependency
- Release type: Release
- Feature-equal with the Fabric 2.1.13 file; parity is enforced by `tools/check-parity.js`.

## Texture pack
Same `VanillaSkills-TexturePack.zip` as the Fabric edition — one pack serves both loaders, and it only
needs uploading once, to the **VSTP - Vanilla-Skills** project (`1585850`). It must go up alongside the
jars: they carry its SHA-1 and clients reject a mismatch.
