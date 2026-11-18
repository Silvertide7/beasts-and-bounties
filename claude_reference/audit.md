# Audit: per-board bounty postings (2026-11-18)

Scope: the `perBoardPostings` config, `BountyOffers.forBoard` / `boardSeed`, the board position on
`BoardOpened` / `AcceptBounty` / `AbandonBounty`, the reach check in `PlayerBounties`, and the
gametests. Two passes: my own read and an independent reviewer given only the diff and the goal.
All 32 gametests pass.

The reviewer confirmed that with the config off nothing behaves differently from before. It also
confirmed the dimension comes from the server and never from the client, the seed mixing gives
unrelated postings for neighbouring boards, payload handlers run on the main thread, there are no
client class leaks, and the network version bump is right.

## Fixed

- [x] **P1. A hacked client could make the server load chunks anywhere.** `canUseBoard` read the
  block before checking reach, and reading a block loads its chunk (or generates it). Repeated
  accept packets with far-off positions could stall the server. Reach is now checked first.
- [x] **P2. The board could go stale after abandoning out of reach.** The refresh was skipped, so the
  screen kept showing the abandoned poster. The refresh is sent again every time; it only shows
  postings and grants nothing.
- [x] **P3. Misleading message.** An accept refused for reach said "That bounty isn't posted for
  you." It now says "You're too far from the board." (`bounty.beasts_and_bounties.board_out_of_reach`).
- [x] **P4. Constant in the wrong place / an extra roll.** `BOARD_REACH_LENIENCY` moved up with the
  other constants, and a refused accept no longer rolls the board's postings first.
- [x] **P5. The rule against accepting from a spot with no board was untested.**
  `perBoardAcceptsOnlyAtAReachableBoard` switches the config on, checks that an accept from a spot
  with no board is refused and one at a reachable board works, then restores the old value.
- [x] **P6. Weak and fragile tests.** The "postings differ" checks compared ordered lists (the same
  creatures in another order counted as different); they now compare sets. The accept test passed
  `BlockPos.ZERO` and failed whenever `perBoardPostings` was on, which it is in the dev
  `run/config`. It now stands at a real board, so it passes either way.

---

# Audit: wall-mounted bounty board (2026-11-18)

Scope: `BountyBoardBlock`, the `bounty_board` blockstate, the new `block/bounty_board_wall` model and
the CLAUDE.md Block section. Two passes: my own read and an independent reviewer given only the diff
and the goal. The build passes; nothing has been checked in game yet.

The reviewer found no blocking bugs and checked these against vanilla's standing and wall signs:
the wall facing and the side it hangs from, the model rotations matching the hitboxes, the support
check (`isSolid`), boards dropping as an item when they pop off (pistons included), clicking through
replaceable blocks such as grass, rotate and mirror, and existing worlds loading their old boards as
`wall=false`.

## Fixed

- [x] **W1. `WALL` was public** but only used inside the block. It is private now.
- [x] **W2. Bare numbers in the wall hitboxes.** The board's 4 and 12 pixel heights are now
  `WALL_BOARD_BOTTOM_PIXELS` / `WALL_BOARD_TOP_PIXELS`.
- [x] **W3. Duplicate standing support check.** `BlockItem.canPlace` already calls `canSurvive`,
  so `getStateForPlacement` returns the standing state without checking it again.
- [x] **W4. The back of the wall board was always drawn.** The face against the wall now has
  `"cullface": "south"`, so it is skipped when the wall hides it.
- [x] **W5. Import order** (`Blocks` came after `HorizontalDirectionalBlock`).

## Open

- [x] **W6. No gametest for placement or popping off.** `boardHangsOnAWallAndPopsOffWithoutSupport`
  checks that clicking a wall's side hangs the board facing away from it, and that both a wall and a
  standing board drop as items once their support is removed. It fails if `canSurvive` stops checking.
- [ ] **W7. Not waterloggable** (this was already true before the change). Vanilla signs keep the
  water they're placed in; the board removes it. Add only if wanted.

## Accepted

- **A1. Old standing boards on weak supports.** A board already standing on glass panes, a fence
  or similar stays put until the block below it changes, then pops off and drops itself. This is
  how vanilla signs behave too, and nothing is lost.

---

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
