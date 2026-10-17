# Audit: pre-release, whole mod (2026-10-17)

Scope: every source file, the gametests, lang, both built-in datapacks, advancements, recipes,
sounds, mixin config, the mods.toml template and CLAUDE.md. Two passes: my own read and an
independent reviewer given only the repository and the goal. Build passes; 28 gametests pass on the dedicated-server dist.

Headline: **no client class leaks, every send goes through `Payloads.sendIfConnected`, every lang
key resolves, all 54 entries and 38 bounties validate, attachments and saved data are right.**
Five real defects were found and fixed the same day. Two findings were rejected with reasons.

## Fixed

- [x] **F1. Crash: Track button after a search empties the list.** `rebuildPage()` returned early
  with `trackButtonShown` still true, so a click where the button had been dereferenced a null
  selection. Now the flag is cleared before the early return.
- [x] **F2. Entry sync could exceed the 1 MiB payload cap.** The whole entry map went out as one
  payload; a large pack would kick every client at login. Entries now go out in chunks of 8
  (`Entries.chunked`), the first flagged to replace the client map and the rest merging in.
- [x] **F3. Stale hunt target after dying mid-hunt.** The transient `last_hunt_update` attachment
  wasn't carried to the respawned player, so `sendIfChanged` thought nothing had changed. A
  `PlayerEvent.Clone` handler copies it (a transient attachment can't use `copyOnDeath`).
- [x] **F4. Compass drawn without blending.** The ring, arrow and height markers relied on the
  crosshair leaving blend enabled, which it doesn't in third person. They now enable it themselves.
- [x] **F5. A refreshed board could reopen after the player closed it.** `BoardOpened` carries
  `refreshOnly`; the client only applies a refresh to an already-open board.
- [x] **F6. Tracking & Bounties tab condition didn't match the doc.** It ignored bounties claimed and
  entries that require bounties, leaving `BestiaryEntry.usesBounties` dead. Both are in the
  condition now.
- [x] **F7. CLAUDE.md drift.** Documented a deleted `awardCraftExperienceFor`, called the board
  wall-mounted, and omitted the chunked sync and the clone handler. All corrected.

## Rejected

- **R1. "Both screens pause the integrated server, so Accept and Track are dead in singleplayer."**
  Not so: the server's task queue still runs while paused (`waitUntilNextTick` drains it), so
  serverbound packets are handled and replies sent; the user has used both buttons in the
  singleplayer dev client throughout. Making the book not pause would also let mobs attack a
  reading player, which vanilla's book screens don't do. Left as is.
- **R2. Loosening the NeoForge version range.** `[${neo_version},)` requires 21.1.249 or newer.
  Deliberate: it is the version the mod is built and tested against. Revisit only if users ask.

## Release metadata (needs the user)

- [x] `mod_version` set to `1.0.0`; `authors="Silvertide7"` in the mods.toml template.
  `issueTrackerURL` and `displayURL` stay commented out.
- [x] Project Problems check (export in `claude_reference/problems`). Acted on: two
  `Math.clamp` rewrites (command counters, `saturatingAdd`), `Long::sum` for the add command, an
  inferable type argument on the entry codec, an unused pattern variable renamed `ignored`, and
  four always-constant parameters folded into `drawFooter` / `drawCenteredScaled` / `isOverCard`
  on the board screen. Left alone, with reasons: "method never used" on every `@SubscribeEvent`
  handler, mixin and gametest (reflection-driven); every "`Level` used without try-with-resources"
  (the IDE sees `AutoCloseable` on `Level`; nobody closes a level); the nonnull-annotation nags on
  overrides (the project doesn't annotate); `Optional` parameters and fields (deliberate, they
  model "not tracking"); the Gradle DSL and `${...}` TOML template "unresolved" errors (template
  expansion); `PackFile` visibility (a private record in a protected signature); the `runAsFancy`
  deprecation (vanilla's own inventory entity render still calls it); and the "may be null" data
  flow warnings, each guarded earlier in the same path.

## Verified OK (reviewer's list, condensed)

Class leaks (lambda-only client references; server-dist gametest run loads clean); payload
immutability in singleplayer; attachment serialization, `copyOnDeath` and empty-state removal;
`BoardRerolls` dirty marking; datapack reload off-thread prepare and on-thread apply; registry
lookups guarded; SilverLib 1.2.2 and PMMO 2.10.47 API shapes match the declared ranges; command
math and clamping; lang coverage both ways; all datapack JSON against the codecs; advancement,
recipe, loot, sound, particle and model formats; every sprite PNG at the size the code assumes;
config defaults against the doc; GUI depth order; gametests assert real behaviour.
