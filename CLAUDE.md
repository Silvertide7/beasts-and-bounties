# Beasts & Bounties

A NeoForge mod for **Minecraft 1.21.1** (NeoForge `21.1.249`, **Java 21**). Mod id `beasts_and_bounties`.
It adds a **bestiary**: a book that fills in as you kill creatures. Every tracked creature has a
data-driven entry made of **milestones**, each reached at a number of kills, bounties claimed, or
both. Reaching one reveals more
about the creature (name, 3D model, stats, weaknesses, where it lives, notes) and can give
**permanent bonuses** (attributes, more damage against that creature, less damage taken from it).
It works for any living entity, but the shipped content is aimed at hostile mobs.

v1 is the bestiary. The bounty system itself is a later release (see [Roadmap](#roadmap)), but
players already store bounties claimed per creature and milestones can already require them.

## Design principles

- **Progress (kills + bounties claimed, per creature) is the only state that gets saved.** Everything
  else (visible knowledge, bonuses, attribute modifiers) comes from `progress + data`. Nothing derived is ever saved, so changing the
  data and running `/reload` puts every player right without a migration.
- **Merge once at load, look up at runtime.** When an entry is parsed, its milestones are folded into
  a running total for each milestone. At runtime you find the highest milestone reached and read
  that precomputed total. No merging on the damage path or in the render loop.
- **The hot path is the damage event.** `LivingIncomingDamageEvent` fires for every hit on every
  entity. Do the `instanceof ServerPlayer` check first, then one `Map<EntityType<?>, BestiaryEntry>`
  lookup, then one kills lookup. Allocate nothing.
- **No config file in v1.** Everything a server would tune lives in the datapack.

## Data format

One JSON file per entity at `data/<entity_namespace>/bestiary/<entity_path>.json`. **The file path
is the entity id**: `data/minecraft/bestiary/zombie.json` is `minecraft:zombie`, so no `entity`
field is needed.

**Files from different packs merge, like vanilla tags.** Every pack's copy of the same path is read,
lowest priority first:
- `"replace": false` (the default) adds the file to what lower packs defined. Its `classification`,
  if present, wins, and its milestones join the others.
- `"replace": true` throws away everything from lower packs first, so the file is the whole entry.
  Use this to overwrite a vanilla entry outright.

Milestones from any files that share the same `kills` and `bounties` requirement are combined into one milestone using the
merge rule below, in pack order: higher packs win on scalars, lists append, and rewards add up. So
an addon can add a reward at 25 zombie kills without copying the vanilla file.

```json
{
  "classification": "normal",
  "milestones": [
    { "kills": 1 },
    { "kills": 3,  "name": true, "model": true, "type": "Undead" },
    { "kills": 10, "description": { "translate": "bestiary.beasts_and_bounties.zombie.description" },
                   "stats": { "health": 20, "damage": "medium", "speed": "low" },
                   "habitat": ["Overworld, at night or in darkness"] },
    { "kills": 25, "weaknesses": ["Sunlight", "Smite"], "strengths": ["Calls nearby zombies for help"],
                   "rewards": [ { "type": "damage_bonus", "amount": 0.05 } ] },
    { "kills": 50, "notes": ["Villagers it kills may rise as zombie villagers"],
                   "rewards": [ { "type": "damage_resistance", "amount": 0.1 },
                                { "type": "attribute", "attribute": "minecraft:generic.max_health",
                                  "amount": 1, "operation": "add_value" } ] }
  ]
}
```

**Top-level fields.** `replace` (optional, default `false`), `milestones` (the merged entry needs at
least one) and `classification` (optional, default `normal`). Classification is not tied to a milestone; it shows as soon as the entry is
listed, because its job is to make dangerous creatures stand out.

| `classification` | Name colour | Icon sprite |
|---|---|---|
| `normal` | black | none |
| `elite` | dark aqua | `bestiary/classification_elite` |
| `minor_boss` | dark purple | `bestiary/classification_minor_boss` |
| `major_boss` | dark red | `bestiary/classification_major_boss` |

The colours are dark because they sit on parchment. The icons are 8×8 placeholders, and the Overview
page also prints the classification name for anything that isn't normal.

**Milestone fields.** All are optional except `kills`.

| Field | Type | Meaning |
|---|---|---|
| `kills` | int ≥ 0, default 0 | Kills needed. |
| `bounties` | int ≥ 0, default 0 | Bounties claimed for this creature needed. A milestone needs at least one of `kills` / `bounties` above 0. |
| `name` | bool | Reveal the entity's own name (`EntityType.getDescription()`; rename it through lang). Until then it shows `???`. |
| `model` | bool | Reveal the slowly rotating 3D model. Until then a placeholder is drawn. |
| `type` | text | Category, e.g. Undead or Arthropod. |
| `description` | text | Description paragraph. |
| `stats` | map name → number or tier | Tiers are `low`, `medium`, `high`, `extreme`. The name is a lang key `stat.beasts_and_bounties.<name>`. Shown sorted by name. |
| `attack_patterns` | list of text | How it fights: "Shoots arrows from range", "Climbs walls". Shown under the stats on the Knowledge tab. |
| `strengths`, `weaknesses`, `habitat`, `notes` | list of text | Bullet lists. |
| `rewards` | list of reward | Permanent bonuses. See below. |

**Milestones unlock in order, as a chain.** They are sorted by `kills`, then `bounties`, and the
reached milestone is the last one in an unbroken run of met requirements. So a milestone that needs
a bounty **blocks every milestone after it** until the bounty is claimed, however many kills the
player has. That makes the bounty system required past a chosen point. Two consequences:
- A milestone with only `bounties` (0 kills) sorts first and gates the whole entry. To gate late
  progress, give the bounty milestone a kill count too: `{"kills": 25, "bounties": 1}`.
- The vanilla pack has no bounty requirements yet. Bounties can't be claimed until the bounty system
  exists, so gating vanilla now would freeze progress (admins could still use commands).

"Text" means a vanilla `Component` (`ComponentSerialization.CODEC`), either a plain string or
`{"translate": ...}`. Shipped entries use translate keys. Pack authors can use plain strings.

**The entry appears in the book when the first milestone is reached.** A first milestone with
nothing else in it (as in the example) means "listed, but nothing known yet". To make a creature
take several kills before it shows up at all, give its first milestone a higher `kills`.

**Merge rule.** Reached milestones apply in kill order. `name`, `model`, `type` and `description`
are replaced by later milestones, so a later description can supersede an earlier one. Lists are
appended. `stats` merges by key, later values winning. Rewards add up.

**Rewards** use a `type`-dispatched codec (a sealed interface, one record per type):

| `type` | Fields | Effect |
|---|---|---|
| `attribute` | `attribute`, `amount`, `operation` (`add_value` / `add_multiplied_base` / `add_multiplied_total`) | A global attribute modifier on the player. |
| `damage_bonus` | `amount` (fraction) | Damage dealt *to this entity type* × (1 + Σ). |
| `damage_resistance` | `amount` (fraction) | Damage taken *from this entity type* × max(0, 1 − Σ). |
| `extra_loot` | `chance` (0–1) | Chance, min(1, Σ), that a kill by the player rolls the mob's loot table one extra time. |
| `bonus_experience` | `amount` (fraction) | Experience the mob drops × max(0, 1 + Σ), rounded. |

All four non-attribute rewards are summed per milestone at load into one `Bonuses` record
(`damageMultiplier`, `damageTakenMultiplier`, `extraLootChance`, `experienceMultiplier`), so every
runtime lookup is `entry.bonuses(progress)`: one index into a precomputed `Bonuses[]`.

- **Extra loot** runs in `LivingDropsEvent`, only when the kill counts as a player kill
  (`isRecentlyHit`). The killer is `CombatTracking.attributedPlayer(source.getEntity())`, so pets and
  summons count for their owner, and only the killer rolls (not every participant). The extra roll
  builds the same `LootParams` as vanilla `dropFromLootTable`, including luck and
  `LAST_DAMAGE_PLAYER`. Its items are added to `event.getDrops()`, so grave and drop mods see them.
- **Bonus experience** runs in `LivingExperienceDropEvent`, using the event's attacking player. The
  ender dragon's experience is awarded outside that event, so the bonus doesn't apply to it.

**Bad data is logged as a WARN and skipped, never failing the reload.** An entry is
skipped when its entity id is not in `BuiltInRegistries.ENTITY_TYPE`, or when no milestones are left
after merging. A single pack's file that fails to decode (bad JSON, an unknown attribute) is skipped
and logged with its pack id, and the other packs' files for that entity still merge. Milestones are
sorted by `kills` at load, so authors can list them in any order. The loader is a
`SimplePreparableReloadListener` over `FileToIdConverter.listMatchingResourceStacks`. It is not a
`SimpleJsonResourceReloadListener`, which only sees the top pack's file, and not a datapack registry,
which fails the whole world load on one bad file.

## Architecture

Feature-folder layout, the same convention as Realms of Reverie and SilverLib: everything that
changes together lives together. The `@Mod` class stays thin and calls `XFeature.init(modEventBus)`.

```
src/main/java/net/silvertide/beasts_and_bounties/
  BeastsAndBountiesMod.java     @Mod entry point, MODID, id(path)
  feature/bestiary/
    BestiaryFeature             init: registers attachment, item, payloads, event listeners
    BestiaryEntry               classification + sorted milestones; precomputed Knowledge[] and damage multiplier double[]s; milestoneReached(kills)
    Milestone                   authored form: kills + Knowledge (inlined MapCodec) + rewards
    Knowledge                   visible info; Knowledge.merge(later) is the merge rule
    Reward                      sealed interface + dispatch codec (attribute / damage_bonus / damage_resistance)
    Classification, Tier        StringRepresentable enums carrying their display colour
    BestiaryEntries             server-side maps by id and by EntityType + the reload listener that merges pack files (replace)
    Progress                    record(kills, bounties): stored per creature and used as a milestone's requirement; meets(requirement)
    BestiaryProgress            player attachment Map<ResourceLocation, Progress>; addKills / addBounties / set / resetAll
    BestiaryAttributes          sums attribute rewards over every entry, reapplies the transient modifiers
    BestiaryEvents              death → credit, damage → multipliers, datapack sync → payloads + attributes, respawn → attributes
    BestiaryPayloads            entries sync, full kills sync, single kill delta
    BestiaryCommand             /bestiary admin commands
    BestiaryItem                right-click opens the screen on the client
    client/
      ClientBestiary            client copy of entries + kills; shows a toast on milestone deltas
      BestiaryToast             milestone toast drawn from the mod's own toast + icon sprites
      BestiaryScreen            the book UI
src/main/resources/
  beasts_and_bounties_logo.png  mod list logo (logoFile in mods.toml, logoBlur=false for pixel art); copy of bestiary_logo.png at the repo root
  assets/beasts_and_bounties/   lang, item model + texture, GUI spread + sprites
  builtin_data_packs/vanilla_bestiary/   built-in datapack: data/minecraft/bestiary/ (54 entries: hostile mobs, bosses, and livestock/fish players normally kill), pack.png
  data/beasts_and_bounties/recipe/bestiary.json
src/gametest/                   gametests + the empty structure template; loaded by dev runs, not in the jar
```

### Shipped vanilla entries

The vanilla entries are **not part of the mod's own data**. They ship as a built-in datapack, the
same way as Artifactory and Mortal Boons: `BestiaryFeature.addVanillaEntriesPack` registers it via
`AddPackFindersEvent` with `PackSource.DEFAULT`, `alwaysActive = false` and `Pack.Position.TOP`.
It is enabled by default and can be turned off on the create-world datapack screen or with
`/datapack disable "mod/beasts_and_bounties:builtin_data_packs/vanilla_bestiary"`. Any pack can also
override a single entry by putting a file at the same path. With the pack disabled the mod tracks
nothing until another pack adds entries. The vanilla text lang keys stay in the mod's `en_us.json`,
because lang is a client resource and can't go in a datapack.

Every text field in a shipped entry is a translate key: `bestiary.beasts_and_bounties.<mob>.<field>[.<index>]`,
with the English text in `en_us.json`. Edit the JSON and the lang file directly; they are the source
of truth, and no generator is kept. Milestones follow a pattern per classification:

Information comes first; rewards then arrive in small, frequent steps. Every entry of a
classification ends on the same totals.

| Classification | Information (kills → reveals) | Final milestone | Totals at the final milestone |
|---|---|---|---|
| normal | 1 listed · 2 name, model, type · 5 description, stats, attacks · 10 habitat · 15 strengths · 25 weaknesses · 40 notes | 250 | +10% damage, 10% resistance, 100% extra loot, +50% XP, 1× attribute |
| elite | 1 name, model, type · 3 description, stats, attacks · 5 habitat · 8 strengths · 12 weaknesses · 18 notes | 150 | +15% damage, 15% resistance, 100% extra loot, +50% XP, 2× attribute |
| minor_boss | 1 identity, description, stats, attacks · 2 habitat, strengths · 3 weaknesses · 4 notes | 10 | +10% damage, 10% resistance, 100% extra loot, +50% XP, 4× attribute |
| major_boss | 1 everything but lore · 2 strengths, weaknesses · 3 notes | 5 | +20% damage, 20% resistance, 100% extra loot, +50% XP, 8× attribute |

Reward steps:
- **normal:** milestones at 1, 2, 5, 10, 15, 25, 40, 60, 80, 100, 125, 150, 175, 200, 250. Damage
  +2% at 15, 40, 80, 125, 175. Resistance 2% at 25, 60, 100, 150, 200. Extra loot +10% and XP +5% at
  10, 25, 60, 80, 100, 125, 150, 175, 200, 250.
- **elite:** 1, 3, 5, 8, 12, 18, 25, 35, 45, 60, 75, 90, 110, 150. Damage +3% at 8, 18, 35, 60, 90.
  Resistance 3% at 12, 25, 45, 75, 110. Loot +10% and XP +5% at 5, 12, 25, 35, 45, 60, 75, 90, 110, 150.
- **minor_boss:** 1–8 and 10. Loot +20% and XP +10% at 1, then +10% and +5% at every later milestone.
  Damage +2.5% at 2, 4, 6, 8. Resistance 2.5% at 3, 5, 7, 10. Attribute half at 5, half at 10.
- **major_boss:** 1–5. Loot +20% and XP +10% at every milestone. Damage +5% at 1, 3, 4, 5. Resistance
  5% at 2, 3, 4, 5. Attribute half at 3, half at 5.

**Every entry grants one themed attribute**, sized by classification. Each attribute has a normal
"unit"; elites give 2×, minor bosses 4× and major bosses 8×. Normal and elite entries grant it at
their final milestone; bosses grant half at each of two milestones (minor 5 and 10, major 3 and 5).
Normals stay tiny because there are 44 of them and attributes stack across every entry.

| Attribute | Normal unit | Given by |
|---|---|---|
| `generic.max_health` | +0.5 | cow, giant, mooshroom, pig, zombie, zombie_villager · evoker (elite) · wither (major, +4) |
| `generic.armor` | +0.5 | husk, sheep, shulker · elder_guardian (minor, +2) |
| `generic.armor_toughness` | +0.25 | pufferfish · wither_skeleton (elite) |
| `generic.knockback_resistance` | +0.025 | hoglin, zoglin · ravager (elite) · warden (minor, +0.1) |
| `generic.explosion_knockback_resistance` | +0.025 | creeper, ghast |
| `generic.movement_speed` (`add_multiplied_base`) | +0.5% | pillager, stray, vex · illusioner (elite) · ender_dragon (major, +4%) |
| `generic.attack_damage` | +0.1 | blaze, polar_bear, vindicator, zombified_piglin · piglin_brute (elite) |
| `generic.attack_speed` (`add_multiplied_base`) | +1% | cave_spider, skeleton |
| `generic.jump_strength` (`add_multiplied_base`) | +1% | goat, magma_cube, rabbit · breeze (elite) |
| `generic.safe_fall_distance` | +0.25 | bat, chicken, phantom, slime |
| `generic.luck` | +0.1 | endermite, piglin, tropical_fish, witch |
| `generic.oxygen_bonus` | +0.2 | cod, glow_squid, squid |
| `generic.water_movement_efficiency` | +0.02 | drowned, salmon |
| `generic.movement_efficiency` | +0.02 | bogged |
| `generic.step_height` | +0.05 | spider |
| `player.block_interaction_range` | +0.1 | enderman |
| `player.mining_efficiency` | +0.25 | silverfish |
| `player.submerged_mining_speed` (`add_multiplied_base`) | +2% | guardian |

Everything stacked together stays small: +8 max health in total, about +6.5% movement speed.
Keep new attributes positive: the Rewards tab colours negative modifiers red as penalties.

**No entries for mobs players shouldn't kill.** The pack leaves out pets and tameables (cat, wolf,
parrot, horses, donkey, mule, llamas, camel, fox, ocelot, skeleton and zombie horses), helpers
(allay, iron and snow golems), villagers and wandering traders, and peaceful or protected creatures
(axolotl, dolphin, turtle, sniffer, panda, frog, tadpole, armadillo, bee, strider). Don't add them
back: the rewards would encourage killing them. A gametest checks a sample of them stays absent.

Shipped stats are tiers only (`low` / `medium` / `high` / `extreme`), never numbers; numbers stay
supported for pack authors who want them. The vanilla values were bucketed by these upper bounds
(low / medium / high, anything above is extreme): health 10 / 24 / 100, damage 3 / 6 / 12,
armor 4 / 10 / 16. Speed was always judged as a tier.

Attack patterns always unlock with the stats, whichever milestone that is for the classification.
Mobs with no combat behaviour (chickens, cows, villagers and the like) have none.

Resistance is only granted against mobs that deal damage. The dev fast zombie is a copy with its 15
milestones at kills 1–15, except its final reward is +5 max health instead of the zombie's +0.5. Don't
regenerate it blindly from the vanilla file.

### Kill credit

`LivingDeathEvent` at `EventPriority.LOWEST`, so totem and revive mods cancel first (cancelled events
are not received). If the victim's type has an entry, every player in
`CombatTracking.participants(victim, KILL_CREDIT_WINDOW_TICKS)` gets one kill. That function comes
from **SilverLib**, a required dependency built partly for this mod. It covers group fights, pets and
summons, and supporting healers. Kills of untracked types are never stored, so the attachment can't
grow without limit.

All progress goes through **`BestiaryProgress.set`**, usually via `addKills` / `addBounties`. It
writes the attachment, sends the delta payload, and reapplies attributes only if the reached
milestone changed. Nothing in v1 calls `addBounties` except the command; the bounty system will.

Progress is keyed by `ResourceLocation`, not `EntityType`, so counts for a removed mod's entities are
kept and come back if the mod returns. The attachment is serialized and `copyOnDeath`.

### Bonuses

There are two kinds of bonus, and they work differently on purpose.

- **Damage against one creature type is not an attribute.** When the datapack loads, each
  `BestiaryEntry` builds two `double[]` arrays, `damageMultiplierAtMilestone` and
  `damageTakenMultiplierAtMilestone`. Entry `i` holds the final multiplier with every reward up to and
  including milestone `i` already summed, so `1 + Σbonus` and `max(0, 1 − Σresistance)`. Index `-1`
  (no milestone reached yet) is `1.0`. Finding the reached milestone from progress is a short
  loop over the sorted requirements (entries have about 3 to 8 milestones). After that, the lookup is a
  single array index.
- One `LivingIncomingDamageEvent` handler applies both:
  - If the attacker is a `ServerPlayer`, the damage is multiplied by the victim type's dealt
    multiplier. `source.getEntity()` is used, so arrows count for the shooter.
  - If the victim is a `ServerPlayer`, the damage is multiplied by the attacker type's taken
    multiplier.

  Either way the cost is one entry lookup, one kills lookup and one array read, with nothing
  allocated and nothing saved on the player.
- **Attributes**: all `attribute` rewards across all entries add up to **one modifier per
  (attribute, operation)**, with fixed ids like `beasts_and_bounties:bestiary_add_value`. To reapply,
  remove our ids from every attribute instance the player has, then add the current totals. That
  takes no memory of what was applied before. Reapply on login, respawn, milestone crossing,
  datapack reload (all online players), and command changes.
- **Modifiers are transient (`addTransientModifier`), never saved with the player.** The player
  stores only progress; bonuses are always computed from the loaded datapack, so changing reward
  data never leaves stale modifiers in player saves. Accepted cost: health is clamped to base max
  health when the player loads, before our modifiers apply, so a player above base max loses that
  extra health on relog. Do not "fix" this by saving modifiers or health.

### Sync

There is no attachment sync on NeoForge 21.1, so it is done by hand:

- **Entries**: `OnDatapackSyncEvent` covers both join and `/reload`. The payload carries the
  authored milestones via `ByteBufCodecs.fromCodecWithRegistries`. `Component` needs registries.
  NBT maps lose order, which is why stats are shown sorted. The client rebuilds the precomputed
  arrays with the same code.
- **Progress**: a full map on login (`AllProgress`), then a single `ProgressUpdate(entity id,
  progress)` per change. When a
  delta reaches a milestone, the client shows a toast ("New Creature Recorded" / "Knowledge Deepened"),
  with the creature's name only if it has been revealed.
- Server state (`BestiaryEntries`) and client state (`ClientBestiary`) are separate holders, even in
  singleplayer.

### The book

- A `bestiary` item. Right-click opens `BestiaryScreen`. Crafted shapeless from book + bone. Kills
  are tracked whether or not the player has the book.
- On the left, the paged list of discovered entries (no discovered count for now). On the right,
  the selected entry: name, model, type, stats, description, strengths, weaknesses, habitat, notes,
  bonuses earned, and the next milestone's requirement (kills, bounties, or both), or "Entry complete. Nothing left to learn." once every milestone is reached.
- **Search**: an `EditBox` above the list. It filters the list on every keystroke with a
  case-insensitive substring match on the *revealed* name only, so a `???` entry can't be found by
  name before its name is revealed. The filtered list is rebuilt when the query changes, never
  per frame.
- **Tabs are pages inside the selected entry**, shown on the right-hand page:
  - **Overview**: name, model, type, description, progress to the next milestone.
  - **Knowledge**: the stats at the top, then Attack Patterns, Strengths, Weaknesses, Habitat and
    Notes sections. There is no separate Stats tab.
  - **Rewards**: two clearly separated sections, each a bold heading plus a grey italic hint:
    - **"Against the <name>"** ("Against This Creature" while the name is hidden), hint "Only while
      fighting it": the per-creature `Bonuses` (increased damage, less damage taken, extra loot roll
      chance, bonus experience).
    - **"Permanent Boons"**, hint "Always active, wherever you roam": the attribute rewards, summed
      per attribute and operation, in vanilla attribute wording ("+0.5 Max Health").
    A section with nothing earned is left out.

  Tabs with nothing revealed are hidden. The selected tab stays the same when you switch entries,
  falling back to Overview if that tab is hidden. Each page turns with its own `page_backward` /
  `page_forward` arrows at the bottom (shown only when there is a previous / next page, with a
  "1 / 3" page number between them and the vanilla page-turn sound). The left page pages the
  creature list (10 rows a page), the right page pages the selected tab's text. There is no mouse
  wheel scrolling. Tab icons are sprites, with the tab name as a tooltip.
- **Every texture the mod's UI draws is its own PNG, so all of it can be repainted.** Never draw a
  vanilla texture, item render or vanilla tooltip/toast. To look like vanilla, copy the vanilla PNG
  from the 1.21.1 client jar into the mod and draw the copy. The only non-texture drawing is plain
  colour fills: the screen dim, list highlights, and the search underline.
  - `textures/gui/bestiary.png` is the two-page spread, **1080×665**, hand-painted (no longer a
    vanilla copy). It draws at 3.6 texture pixels per GUI pixel (`TEXTURE_PIXELS_PER_GUI_PIXEL`), so
    300×185 on screen. **All layout is defined in texture pixels** as `Area.fromTexture(x, y, w, h)` and
    converted once, so measuring the PNG gives the numbers directly:
    **The header** is the top row of each page: the search bar on the left, the entry title on the
    right. **The footer** is the bottom row of each page: the page-turn arrows and "n / m". Moves the
    user asks for are in screen (GUI) pixels; one GUI pixel is 3.6 texture pixels, so a smaller
    texture nudge can round away to nothing.
    - left header (search bar) `(118, 57)`, 350×30, with the clear button at its right end.
    - creature list `(82, 101)`, 408×447.
    - left footer `(103, 548)`, 380×30: back arrow at the left end, forward at the right,
      "n / m" centred. Arrows are top-aligned to the row and hang a little below it.
    - right header (entry title, the creature name) `(612, 57)`, 350×30. Its text sits on the same line as the search
      text (both vertically centred in their 30-pixel rows via `titleTextY`).
    - entry content `(597, 101)`, 410×447, level with the creature list (model box on Overview, then text).
    - right footer `(588, 548)`, 370×30.
    Tabs start at the entry content's left edge, above the book.
    A repaint that changes the PNG size only needs `TEXTURE_WIDTH` / `TEXTURE_HEIGHT` and these areas.
  - The GUI sprites are in `textures/gui/sprites/bestiary/` and are referenced as
    `beasts_and_bounties:bestiary/<name>`:
    - `tab_left`, `tab_middle`, `tab_right`, each with a `_selected` variant: 28×32 tab shapes. The
      first visible tab uses `left`, the last uses `right`, any between use `middle`; a lone tab
      uses `left`. Unselected tabs overlap the book by `TAB_OVERLAP` (5), the selected one by
      `SELECTED_TAB_OVERLAP` (3).
    - `icon_overview`, `icon_knowledge`, `icon_rewards`: 16×16 tab icons (copies of the bestiary
      item, book and quill, emerald). `icon_overview` is also the toast icon.
    - `toast`: 160×32, the milestone toast background (vanilla advancement toast), used by
      `BestiaryToast`.
    - `tooltip` + `tooltip.png.mcmeta`: a 16×16 nine-slice (border 3) recreating the vanilla tooltip
      colours. 1.21.1 draws tooltips as gradients with no texture, so this one was generated rather
      than copied. It is used for the tab-name tooltips.
    - `classification_elite`, `classification_minor_boss`, `classification_major_boss`: 8×8.
    - `clear_search` / `clear_search_highlighted`: 14×14 (vanilla `widget/cross_button`), drawn at
      7×7 at the right end of the search bar, only while there is search text. Clicking it clears the
      search.
    - `page_forward` / `page_backward`: 23×13, each with a `_highlighted` variant (hover). The
      page-turn arrows.
- **Model**: create one entity with `type.create(minecraft.level)` for the **selected entry only**,
  dropped when the selection changes or the screen closes. Spin it by setting body and head yaw from
  `Util.getMillis()`, one turn every 16 seconds. Scale it to fit using the larger of `getBbWidth()` and `getBbHeight()`.
  Non-living or null results fall back to the placeholder. Rendering goes through
  `InventoryScreen.renderEntityInInventory(graphics, x, y, scale, translate, pose, null, entity)`,
  inside a scissor so large mobs can't spill over the page.
- Wrap the text into lines once, when the selection changes, not every frame.

### Commands

`/bestiary` is admin-only (permission level 2, `Commands.LEVEL_GAMEMASTERS`).
- `/bestiary kills <set|add|remove> <targets> <entity> <amount>`
- `/bestiary bounties <set|add|remove> <targets> <entity> <amount>`
- `/bestiary reset <targets> [entity]` clears kills and bounties for one creature, or everything.
- `/bestiary get <target> <entity>` prints kills, bounties and milestone `n of m`.
- `/bestiary complete <targets> [entity]` raises progress to the final milestone's requirement (never
  lowers it) for one entry or every loaded entry. Use it to test a maxed-out bestiary.

Counts clamp to 0 and `Integer.MAX_VALUE`. Entity suggestions come from the loaded entries. Every
change syncs the client and reapplies attributes when the reached milestone changes.

## Build & run

```bash
./gradlew build              # compile + jar
./gradlew runClient          # dev client
./gradlew runServer          # dedicated server (needs eula=true in run/eula.txt)
./gradlew runGameTestServer  # gametests; also a server-dist load, so it catches client class leaks
```

- Versions come from `gradle.properties` only. `neoforge.mods.toml` is a **template** in
  `src/main/templates/META-INF/` and gets expanded at build time.
- SilverLib is added the same way as in Realms of Reverie:
  `compileOnly` + `localRuntime "curse.maven:silverlib-1691772:${silverlib_version}"`, with a
  `required` dependency in the mods.toml.
- No datagen in v1. Entries, the item model, the recipe and lang are plain JSON.
- **Dev-only fast zombie:** `src/gametest/resources/builtin_data_packs/dev_fast_milestones/` is a
  datapack registered by `DevFastMilestonesPack` (also in the gametest source set). It replaces the
  vanilla zombie with a copy whose 15 milestones sit at kills 1 through 15, so every zombie kill in a
  dev run unlocks the next milestone. It ranks above the vanilla pack, which a gametest checks.
  The pack also gives the bat a very long description (a plain string, merged onto the bat's
  5-kill milestone) to exercise paging on the right page. Neither the pack nor the class ships in the jar. Tests that need vanilla thresholds use the
  skeleton instead.
- Gametests live in the `gametest` source set (same setup as SilverLib) and cover four things:
  every shipped entry loads, the zombie's milestones reveal and stack correctly, boss attribute
  rewards are applied and removed, and pack files merge unless one sets `replace`
  (`BestiaryEntries.mergeInPackOrder` is package-private for that test). Rendering is checked in game.

## Conventions & gotchas

- Client-only code lives in `feature/bestiary/client/` and is reached only through
  payload handler lambdas and `BestiaryItem.use` behind `isClientSide()`. Common code may call into
  it only from inside a lambda or a client-only branch. `runGameTestServer` catches leaks.
- Attribute ids in 1.21.1 still carry the `generic.` prefix (`minecraft:generic.max_health`). It was
  dropped in 1.21.2.
- `LivingIncomingDamageEvent` runs before armour, so bonuses scale the raw hit.
- **Wording.** Bonus lines say what changes for the player, never "dealt" / "resisted": "+10%
  increased damage", "10% less damage taken", "50% chance of an extra loot roll", "+50% bonus
  experience". Each per-creature reward has a `.more` and a `.less` lang key chosen by sign, so a
  datapack penalty still reads correctly. Counts use singular keys (`requirement.kills.one` →
  "1 kill"). The book is "Hunter's Bestiary"; sections read "How It Fights", "Where It Dwells" and
  "Hunter's Notes".
- **Two text sizes.** Titles and the creature list draw at normal size. The entry body (description,
  stats, sections, rewards) draws smaller, at a target of 0.75 **snapped to whole screen pixels**:
  `round(guiScale × 0.75) / guiScale` (GUI scale 3 gives 0.67, 4 gives 0.75, 2 stays 1.0). The
  Minecraft font is a bitmap, so any other scale blurs it. Body lines wrap at `width / scale`, and
  the lines per page divide by the scale, so more of a description fits on one page. The scale is
  computed in `init()`, so it follows GUI-scale changes.
- Text on the parchment is drawn **without a drop shadow** (`drawString(..., false)`). Vanilla widgets
  default to a shadow, which smears dark text; `EditBox` needs `setTextShadow(false)`.
- **Never use Minecraft's advancement system** (no advancements, criteria or triggers). Milestones
  are the only progression and reward mechanism.
- `@EventBusSubscriber` leaves `bus` unset; it defaults to the game bus, and the parameter is
  deprecated for removal. Mod-bus listeners are added in `BestiaryFeature.init`.
- In singleplayer, payloads are passed in memory rather than serialized. Anything sent must be
  immutable or a copy (the kills map is sent as `Map.copyOf`), never the live server object.

## Roadmap

### TODO (future release): Tracking

Pick one creature in the book to track. When one is within range, the player is told it is nearby.
To start with, that can just be text on the action bar. Not in v1.

- **Choosing a target**: one tracked entity type per player, picked from the book. A
  serverbound payload sets it, and it is stored in its own small player attachment.
- **Scanning**: the server checks every `N` ticks, never every tick. Only players with a target are
  checked, and each player's scan is offset by `player.getId() % N` so the work is spread across ticks.
  One `level.getEntities(type, player.getBoundingBox().inflate(range), LivingEntity::isAlive)` call
  per scan, keeping only the nearest result.
- **Upgrade levels unlocked by milestones**: add a `tracking` reward type with a `level`; the highest
  level reached wins. For example, level 1 says "nearby", level 2 adds distance, and level 3 adds
  direction (an 8-way compass point from the player's facing). Range could also grow with level. It
  can be added to the `Reward` dispatch codec without touching existing data.
- **Bounties**: they could reuse this scan to point players toward a bounty target.

### TODO (next release): Bounties

Track down one specific creature and kill it for a reward. Not in v1. Do not build any of it early.
v1 already has these hooks so it can be added on top:

- **Claiming**: a claimed bounty calls `BestiaryProgress.addBounties(player, entityId, entry, 1)`.
  Storage, sync, milestone gating, the book's "Bounties claimed" line and admin commands already
  exist. Extra kill progress can be granted with `addKills(..., n)`.
- **Rewards**: extend the `Reward` dispatch codec (items, XP) and reuse it in bounty definitions.
- **Credit**: the same `CombatTracking.participants` call decides who completed the bounty.
- **Definitions**: their own folder, `data/<ns>/bounty/*.json`, referring to bestiary entries by
  entity id.
- **Empowered targets** (maybe): an entity attachment marks the bounty mob, plus attribute modifiers
  at spawn. The kill handler checks for the marker.
- **Time locks**: a separate player attachment holding the game time of the last bounty taken. Keep
  it out of `BestiaryProgress`, which only holds per-creature counts.

Open questions: how a bounty is taken (board block? the book?), how the target is located or
spawned, and whether a failed or abandoned bounty triggers the cooldown.

---

# Reusable Engineering Standards

The sections below are project-agnostic. Copy this block (everything below the `---` separator) into any other project's `CLAUDE.md` unchanged to apply the same standards there.

## Code Style

**Never write comments.** No inline `//` comments, no `/* */` blocks, no javadoc, no leading explanatory headers on methods or fields. Code must be self-documenting through naming alone.

- Variable names describe what the value *is* (e.g. `armorCoveragePercent`, not `acp` with a comment).
- Method names describe what they *do* and under what conditions (e.g. `applyMultiplierIfAttackerIsPlayer`, not `applyBonus` with a comment explaining the player check).
- Extract a well-named helper method instead of writing a comment to explain a block.
- Constants get descriptive names that encode their meaning and unit (e.g. `KNIGHTMETAL_BONUS_DAMAGE_AT_FULL_ARMOR`, not `MAX` with a `// 2.0 vs fully-armored target` comment).
- If a name would need a comment to explain it, rename it until it doesn't.

Existing files may still contain comments and javadoc — leave them in place when editing unrelated code, but do not add new ones and prefer to delete obsolete ones when touching the surrounding code.

**Never leave dead code.** No unused methods, fields, classes, parameters, or imports. No "escape hatch" or "just in case" code. No commented-out blocks. If it's not called, delete it — the git history is the archive.

## Code Review

When asked to review code, do a "pass", check for issues, or otherwise audit a recent change, do **two** passes in order:

1. **Self-audit first.** Read the diff yourself. Fix the obvious — dead code, comments, naming, anything that violates the Code Style rules above. Report findings.
2. **Then spawn an independent reviewer** via the `/code-review` skill or a fresh agent. Give it only the diff and the goal, no context about why you made the choices you did. That catches the bugs you would otherwise rationalize away.
3. **Write to audit.md** Write the findings from the audit to audit.md in the claude_reference folder so we can checkmark them as we complete them.
4. **Project Problems check** by running a whole project search in the problems / project tab. Have the user download and put this file into the claude_reference/problems to check. Ask before deploys if we should do this.
Don't skip step 2 because step 1 looked clean — the value of the independent reviewer is exactly that it doesn't share your blind spots.

## Version Control

**The user handles commits in git.** Never run `git add`, `git commit`, or `git push` — and don't suggest doing so — unless the user explicitly asks. Wrap up work by reporting what changed; staging and pushing are the user's job.
