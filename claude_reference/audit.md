# Audit: whole app (2026-10-13)

Scope: every source file, the gametests, lang, mixin config, mods.toml, build.gradle, both built-in
datapacks and CLAUDE.md. Three passes: my own read, an independent correctness reviewer given only
the code and the goal, and a repo-wide over-engineering audit.

Headline: **no high-severity problems.** Nothing leaks client classes onto a dedicated server, the
network setup is right, progress can't be manipulated from the client, cooldowns can't be reset,
and the damage hot path allocates nothing.

**Status: every item below is fixed** (same day). Build passes and all 20 gametests pass. Where a
fix differs from the suggested one, the note says so.

## Medium

- [x] **M1. The extra loot roll ignores vanilla's loot gating.** Now returns for babies and when
  `doMobLoot` is off. Covered by `damageHooksScaleHitsLootAndExperience`.
- [x] **M2. Pressing G during an active hunt reports a bogus cooldown.** Both the client key handler
  and `PlayerTracking.startHunt` now do nothing while a hunt is running.
- [x] **M3. Single-payload sends are unguarded.** New `core/Payloads.sendIfConnected` is the only
  send path (progress updates, the join sync, tracking sync, pings and hunt updates). The kill
  credit and command gametests that this used to block now exist.
- [x] **M4. Switching or stopping tracking leaves the old ping and wisp running.** `setState` clears
  the ping text and stops the wisp whenever the tracked creature changes.

## Low

- [x] **L1. Per-frame work in the book.** Row name lines are wrapped in `refresh()`, the header
  name line and Track button label/width in `rebuildHeader()`, tab shapes carry their sprite ids
  (`TabShape`), arrow sprites are constants, and the tracked creature is read once per frame. The
  eye-icon space is always reserved so the row width never depends on tracking state.
- [x] **L2. Per-frame text allocation in the HUD.** The ping component is built once on receive;
  timer and cooldown text are rebuilt only when the seconds change (`SecondsText`).
- [x] **L3. Hunt updates resend when glowing creatures swap order.** Glow ids are sorted.
- [x] **L4. Stale hunt arrow position.** `HuntUpdate` is resent when the target moved more than
  4 blocks since the last send (`HuntTarget.isCloseTo`).
- [x] **L5. Tracking text reveals the name early.** `BestiaryEntry.displayName` is the single
  `???` gate; the book, the toast, the HUD ping (`ClientBestiary.creatureName`) and the server's
  "haven't learned to hunt" message all use it.
- [x] **L6. Tracking state never cleaned up.** The scanner's once-a-second check stops tracking
  when the creature's unlocks are gone. Covered by `trackingNeedsAnUnlockAndCleansUpAfterItself`.
  (Done in the scanner rather than from `BestiaryProgress`, so the bestiary feature doesn't
  depend on tracking.)
- [x] **L7. Lenient JSON parsing.** `GsonHelper.parse(reader)`, strict.
- [x] **L8. Respawn health.** After reapplying modifiers on respawn (not an End exit), health is
  set to the new max. Documented in CLAUDE.md.
- [x] **L9. Summons that aren't tamed animals get no extra loot.** `isRecentlyHit` dropped in
  favour of `attributedPlayer != null`, with M1's gates.

## Standards

- [x] **S1. Duplicated helpers.** `SharedConstants.TICKS_PER_SECOND` everywhere; one
  `BestiaryProgress.bonuses(player, type | id)`; one `BestiaryScreen.refreshIfOpen()` (kept, not
  deleted: the header cache from L1 depends on a refresh when tracking state changes); one Vec3
  stream codec; one send guard (M3).
- [x] **S2. Dead and template leftovers.** Unused `type.*` lang keys removed; `maven-publish`,
  the `publishing` block, the `data` run config and the generated-resources source dir removed
  from `build.gradle`; `TEMPLATE_LICENSE.txt` deleted; `README.md` rewritten for the project.
- [x] **S3. Misleading name.** `syncAfterDatapackLoad` → `syncEntriesAndProgress`.

## Over-engineering audit (ponytail)

All applied through S1/S2/L5 above except the ones kept on purpose: `Reward.Type` enum dispatch,
the two config classes, `BestiaryToast` and `WispParticle` (exist to use the mod's own art), one
registry class per type per feature.

## Gametest gaps

- [x] Kill credit end to end (`killsAreCreditedToEveryParticipant`).
- [x] `BestiaryProgress.set` and attributes (`settingProgressReappliesAttributesAcrossMilestones`);
  admin commands (`adminCommandsClampCompleteAndReset`).
- [x] Damage multipliers, extra loot (babies, `doMobLoot`) and experience, posted as real events
  (`damageHooksScaleHitsLootAndExperience`).
- [x] `PlayerTracking.track` refusing an unlocked creature; cleanup on lost unlocks; expired cooldown
  removal (`trackingNeedsAnUnlockAndCleansUpAfterItself`).
- [x] Persistence and `copyOnDeath` for progress and tracking state
  (`progressSurvivesSavingAndDeath`, `trackingStateSurvivesSavingAndDeath`).

## CLAUDE.md corrections

- [x] All applied: `Bonuses[]`, seven reward types with max for tracking, up to 15 milestones,
  event-bus routing, 11 rows, client-code locations, `Payloads.sendIfConnected`, the test
  description, respawn health, `{"replace": true}` removal, loot gating, hunt-update resend rules.

## Not done yet

- [ ] Project Problems check: a whole-project search in the IDE's Problems tab, exported to
  `claude_reference/problems` for review.
