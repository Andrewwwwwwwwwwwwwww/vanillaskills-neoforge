# Translating VanillaSkills

VanillaSkills is **server-side** — its menus are drawn by the server, so translations are applied
by the **server**, per player, based on each player's Minecraft language setting. (A client resource
pack cannot translate this mod.)

## For translators

1. Start from the English template: [`src/main/resources/assets/vanillaskills/lang/en_us.json`](src/main/resources/assets/vanillaskills/lang/en_us.json).
2. Translate only the **values** (the text after each `:`). **Do not change the keys** (the text before `:`).
3. **Keep the placeholders** exactly as they are:
   - `%s` and `%d` are filled in with names/numbers — leave them, and keep their order sensible.
   - `\n` is a line break — keep them where they make sense for your language.
   - `{GRAD}`, `{CONVERT}`, `{MENDING}` (guide pages only) are filled from server settings — leave them.
   - Symbols like `→ ◀ ▶ ✦ ✔ 🔒 ×` can stay as-is.
4. Save the file as `<locale>.json` using your Minecraft language code, e.g. `es_es.json`, `de_de.json`,
   `fr_fr.json`, `ru_ru.json`, `pt_br.json`, `zh_cn.json`. Save as **UTF-8**.
5. Anything you don't translate falls back to English automatically — partial translations are fine.

## For the server owner (installing a translation)

- **Instant, one server:** drop the file at `<world>/vanillaskills/lang/<locale>.json`, then run
  `/skill reload` (or restart). No rebuild, no re-upload. World overrides always win over bundled files.
- **Bundle it for everyone:** put it next to `en_us.json` in the jar's `assets/vanillaskills/lang/` and
  ship a new release.

## What's covered

Everything players see: the Bounty Board, Feats tab, Quest Shop, Skill Tree (lane names, node titles,
descriptions), the Recipes book, the Guide book, and the in-game messages. Node titles auto-translate
from the lane name, so you don't need to translate each tier individually. Op/edit-mode admin text is
intentionally left in English.
