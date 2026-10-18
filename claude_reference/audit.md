# Audit: Forge 1.20.1 port (2026-10-17, branch `1-20-1`)

Scope: the whole ported tree: every source file, the gametests, lang, both built-in datapacks,
advancements, recipes, mods.toml, build.gradle and CLAUDE.md. Two passes: my own read and an
independent reviewer given only the repository, the decompiled Forge 47.3.0 sources and the goal.
Build passes; **29 gametests pass** on the dedicated-server dist (`runGameTestServer`).

Headline: the port is dist-safe, the buses are right, packets are symmetric, persisted data survives
death, End exit and reload, and every datapack attribute exists on 1.20.1. One high-severity platform
difference was found and fixed: DFU 6 swallows decode errors.

## Fixed

- [x] **F1. (high) Bad pack data was silently dropped instead of logged and skipped.** 1.20.1's DFU
  6.0.8 `optionalFieldOf` returns "absent" when the field is present but fails to decode, and lists
  decode partially. An unknown attribute, a bad classification or a malformed reward would load an
  entry with the bad part missing and no log line, and a `replace: true` file with one bad milestone
  would silently delete the vanilla entry. Fixed with `core/StrictCodecs.optionalField` on every
  non-trivial optional field (classification, milestones, rewards, stats, every text field) and by
  rejecting any `DataResult` that carries an error in both loaders, even when a partial value exists.
  New gametest `badPackDataIsSkippedNotSwallowed` pins it.
- [x] **F2. (medium) Entry sync capped at 32 767 characters, not 1 MiB.** `writeJsonWithCodec` goes
  through `writeUtf`; eight big entries already used 77% of that. Chunks are now cut by measured JSON
  size (about 200 000 characters) and sent as UTF-8 bytes.
- [x] **F3. A JSON `null` text would NPE inside DFU.** `TextCodec` now errors on a null component.
  (A `null` field value is treated as absent by `JsonOps`, which is fine.)
- [x] **F4. Fake players leaked cache entries.** The damage multiplier handler now ignores
  `FakePlayer` attackers, as the loot and XP paths already did through `attributedPlayer`.
- [x] **F5. Combat participation recorded post-absorption.** Forge's `LivingDamageEvent` fires after
  absorption is subtracted, so a hit fully soaked by absorption hearts never counted. Participation is
  recorded in `LivingHurtEvent` (pre-armour, like the original), capped at health plus absorption.
- [x] **F6. `.DS_Store` files shipped in the jar.** Deleted, excluded by the jar task, ignored by git.
- [x] **F7. CLAUDE.md drift.** The datapack disable command, the sync cap, singleplayer
  serialization, the damage event names, `EditBox.setTextShadow`, the bus rules, the tick event in
  tests, `use`, `EntityPreview`, the dev zombie's 22 milestones, the boss attribute split, the health
  total, the PMMO file id and API.

## Verified OK (reviewer's list, condensed)

Dedicated-server dist safety (client classes reached only from payload lambdas, `Dist.CLIENT`
subscribers, the mixin client list and `isClientSide()` branches; no client type in any common
signature); every mod-bus subscriber says `bus = MOD`, every handler is `public static`;
`PlayerData` across `restoreFrom` (same tag object, both death and End exit), `Entity.load` (new tag,
cache miss), logout and respawn ordering; `SimpleChannel` id order, `consumerMainThread`, every
encode/decode pair; pack registration and id ordering (`vanilla_bestiary` < `dev_fast_milestones` <
`file/*`); `Sprite` UV maths on the 11-argument blit, `EntityPreview` vs 1.20.1
`renderEntityInInventory`, `SepiaBufferSource` over the full `VertexConsumer`, render order and z
layering, overlay and particle registration, mixin targets; all 13 attribute ids and both operations;
datapack formats and folder names; lang both ways; `MockPlayers`; jar excludes, refmap and manifest;
mods.toml ranges; PMMO `setXpDiff`; loot and XP hooks.

## Not verified here

- [ ] The dev client. This machine is headless, so `runClient` cannot open a window. First in-game
  run should cover the book, the board (sprites, nine-slice, entity previews, sepia), the HUD overlay,
  the hunt key, the wisp particle and both mixins.
