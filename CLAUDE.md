# Beasts & Bounties

A Forge mod for **Minecraft 1.20.1** (Forge `47.3.0`, **Java 17**, ForgeGradle 6). Mod id `beasts_and_bounties`.
This is the `1-20-1` branch, a port of the NeoForge 1.21.1 `main` branch; see **Forge 1.20.1 port** at
the end for everything that differs.
It adds a **bestiary**: a book that fills in as you kill creatures. Every tracked creature has a
data-driven entry made of **milestones**, each reached at a number of kills, bounties claimed, or
both. Reaching one reveals more
about the creature (name, 3D model, stats, weaknesses, where it lives, notes) and can give
**permanent bonuses** (attributes, more damage against that creature, less damage taken from it).
It works for any living entity, but the shipped content is aimed at hostile mobs.

The **bounty board** (see Bounties) posts per-player hunts; completing one credits that creature's
bestiary progress with a bounty and bonus kills (10 / 5 / 1 / 1 by classification), and milestones can require bounties.

## Design principles

- **Progress (kills + bounties claimed, per creature) is the only state that gets saved.** Everything
  else (visible knowledge, bonuses, attribute modifiers) comes from `progress + data`. Nothing derived is ever saved, so changing the
  data and running `/reload` puts every player right without a migration.
- **Merge once at load, look up at runtime.** When an entry is parsed, its milestones are folded into
  a running total for each milestone. At runtime you find the highest milestone reached and read
  that precomputed total. No merging on the damage path or in the render loop.
- **The hot path is the damage event.** `LivingHurtEvent` fires for every hit on every
  entity. Do the `instanceof ServerPlayer` check first, then one `Map<EntityType<?>, BestiaryEntry>`
  lookup, then one kills lookup. Allocate nothing.
- **Bestiary content is tuned in the datapack; tracking numbers in config.** There is no bestiary
  config file. Tracking's distances and times live in `beasts_and_bounties-tracking-server.toml`, and
  its wisp visuals in `beasts_and_bounties-tracking-client.toml` (see Tracking).

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
- The vanilla pack gates only the last two tracking levels on bounties (one for P3, a second for
  A3), so everything else is reachable by kills alone and the bounty board becomes required only
  for the top of each entry.

"Text" means a vanilla `Component` (``TextCodec`, JSON text), either a plain string or
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

| `passive_tracking` / `active_tracking` | `level` (1–3) | Tracking unlocks (see Tracking). The highest level reached wins. |

Every non-attribute reward is folded per milestone at load into one `Bonuses` record
(`damageMultiplier`, `damageTakenMultiplier`, `extraLootChance`, `experienceMultiplier`,
`passiveTracking`, `activeTracking`): the first four are sums, the tracking levels are maxima. Every
runtime lookup is `entry.bonuses(progress)`, one index into a precomputed `Bonuses[]`, reached
through `BestiaryProgress.bonuses(player, entityType | entityId)`.

- **Extra loot** runs in `LivingDropsEvent`. It rolls only when the killing blow is attributed to a
  player by `CombatTracking.attributedPlayer(source.getEntity())`, so pets and summons count for
  their owner, and only the killer rolls (not every participant). It also respects vanilla's loot
  gating: **no roll for babies or when `doMobLoot` is off**, because `LivingDropsEvent` fires even
  when vanilla skipped the loot table. The extra roll builds the same `LootParams` as vanilla
  `dropFromLootTable`, including luck and `LAST_DAMAGE_PLAYER`. Its items are added to
  `event.getDrops()`, so grave and drop mods see them.
- **Bonus experience** runs in `LivingExperienceDropEvent`, using the event's attacking player. The
  ender dragon's experience is awarded outside that event, so the bonus doesn't apply to it.

**Bad data is logged as a WARN and skipped, never failing the reload.** An entry is
skipped when its entity id is not in `BuiltInRegistries.ENTITY_TYPE`, or when no milestones are left
after merging (so `{"replace": true}` with no milestones **removes** a lower pack's entry). Files
are parsed strictly (`GsonHelper.parse`): unquoted keys or trailing commas are rejected and logged. A single pack's file that fails to decode (bad JSON, an unknown attribute) is skipped
and logged with its pack id, and the other packs' files for that entity still merge. On 1.20.1 that
takes two things the code does on purpose: optional fields holding anything richer than a number
or flag go through `core/StrictCodecs.optionalField` (DFU 6's `optionalFieldOf` turns a decode
error into "field absent", silently), and the loaders reject any `DataResult` with an error even
when a partial value exists (a list with one bad element decodes partially). `badPackDataIsSkippedNotSwallowed` pins this. Milestones are
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
  core/Network                  the SimpleChannel: toClient / toServer registration, sendIfConnected (skips connections without the channel), sendToServer
  core/PlayerData, PlayerDataEvents   persisted per-player data (codec → the player's PlayerPersisted NBT, cached by tag identity, forgotten on logout)
  core/TextCodec                Codec<Component> over Component.Serializer (1.20.1 has no ComponentSerialization)
  feature/combat/               CombatTracking, CombatLog, SupportLog, CombatTrackingEvents: the kill-credit participant tracking, inlined from SilverLib
  feature/bestiary/
    BestiaryFeature             init: registers item, payloads, creative tab, the built-in datapack (addBuiltInDataPack)
    BestiaryEntry               classification + sorted milestones; precomputed Knowledge[] and Bonuses[]; milestoneReached(progress); displayName (??? until revealed)
    Milestone                   authored form: kills + Knowledge (inlined MapCodec) + rewards
    Knowledge                   visible info; Knowledge.merge(later) is the merge rule
    Reward                      sealed interface + dispatch codec (attribute, damage_bonus, damage_resistance, extra_loot, bonus_experience, passive_tracking, active_tracking)
    Classification, Tier        StringRepresentable enums carrying their display colour
    BestiaryEntries             server-side maps by id and by EntityType + the reload listener that merges pack files (replace)
    Progress                    record(kills, bounties): stored per creature and used as a milestone's requirement; meets(requirement)
    BestiaryProgress            PlayerData<Map<ResourceLocation, Progress>>; addKills / addBounties / set / resetAll / persist; bonuses(player, type)
    BestiaryAttributes          sums attribute rewards over every entry, reapplies the transient modifiers
    BestiaryEvents              death → credit, damage → multipliers, datapack sync → payloads + attributes, respawn → attributes
    BestiaryPayloads            entries sync, full progress sync, single progress delta
    BestiaryAdvancements        awards the three trophy advancements from BestiaryProgress.set
    BestiaryCommand             /bestiary admin commands
    BestiaryItem                right-click opens the screen on the client
  feature/bounty/
    BountyFeature               init: config, board block + item, sound, payloads, creative tab
    BountyConfig                refresh period, offers shown, claim/abandon cooldowns, allowAbandon
    BountyDefinition, BountyReward   one bounty per entity: kills, weight, bonus_kills, item/experience rewards
    BountyDefinitions           SimpleJsonResourceReloadListener over data/<ns>/bounty (top pack wins)
    BountyOffers, BountyBoardData  the shared board: weighted picks per (world seed, refresh period, reroll count), never repeating the previous board; saved data
    BountyState                 record(active bounty?, cooldownEndsAt) with the accept/kill/end rules
    PlayerBounties              PlayerData<BountyState>; openBoard / accept / abandon / recordKill / complete
    BountyEvents                reload listener, join sync, death → recordKill for every participant
    BountyPayloads              BoardOpened, StateSync, BountyClaimed; AcceptBounty, AbandonBounty
    BountyBoardBlock            free-standing sign on a post; right-click on the board opens it for that player
    client/ClientBounties, client/BountyBoardScreen
    client/
      ClientBestiary            client copy of entries + kills; shows a toast on milestone deltas
      BestiaryToast             milestone toast drawn from the mod's own toast + icon sprites
      BestiaryScreen            the book UI
src/main/resources/
  beasts_and_bounties_logo.png  mod list logo (logoFile in mods.toml, logoBlur=false for pixel art); copy of bestiary_logo.png at the repo root
  assets/beasts_and_bounties/   lang, item model + texture, GUI spread + sprites
  builtin_data_packs/vanilla_bestiary/   built-in datapack: data/minecraft/bestiary/ (52 entries: hostile mobs, bosses, and livestock/fish players normally kill; no breeze or bogged, they don't exist in 1.20.1) and data/minecraft/bounty/ (36 bounties: hostiles and bosses only), pack.png
  builtin_data_packs/dev_fast_milestones/   dev-only pack (excluded from the jar); see Build & run
  data/beasts_and_bounties/advancements/ root, entry_complete, bestiary_complete (impossible criteria, awarded by code; 1.20.1 folder names are plural)
  data/beasts_and_bounties/recipes/     bestiary (shaped: leather over book over bone), bounty_board (shaped: three paper over any sign over a stick); loot_tables/blocks/bounty_board
  data/beasts_and_bounties/structures/empty.nbt   the gametest template (excluded from the jar)
  net/.../gametest/             gametests, MockPlayers and DevFastMilestonesPack, in the main source set but excluded from the jar
```

### Shipped vanilla entries

The vanilla entries are **not part of the mod's own data**. They ship as a built-in datapack, the
same way as Artifactory and Mortal Boons: `BestiaryFeature.addVanillaEntriesPack` registers it via
`AddPackFindersEvent` with `PackSource.DEFAULT`, `alwaysActive = false` and `Pack.Position.TOP`.
It is enabled by default and can be turned off on the create-world datapack screen or with
`/datapack disable "beasts_and_bounties:builtin_data_packs/vanilla_bestiary"`. Any pack can also
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
| normal | 1 listed · 2 name, model, type · 5 description, stats, attacks · 10 habitat · 15 strengths · 30 weaknesses · 50 notes | 500 kills + 2 bounties | +10% damage, 10% resistance, 100% extra loot, +50% XP, 1× attribute |
| elite | 1 name, model, type · 3 description, stats, attacks · 5 habitat · 8 strengths · 12 weaknesses · 16 notes | 200 kills + 2 bounties | +15% damage, 15% resistance, 100% extra loot, +50% XP, 2× attribute |
| minor_boss | 1 identity, description, stats, attacks · 2 habitat, strengths · 3 weaknesses · 4 notes | 25 kills + 2 bounties | +10% damage, 10% resistance, 100% extra loot, +50% XP, 4× attribute |
| major_boss | 1 everything but lore · 2 strengths, weaknesses · 3 notes | 15 kills + 2 bounties | +20% damage, 20% resistance, 100% extra loot, +50% XP, 8× attribute |

The schedules are generated: each entry's text and themed attribute are kept and its milestones are
laid out by classification so that **no milestone unlocks much at once**, rewards arrive more often
the further in you are, and **the six tracking levels never share a milestone and always come in the
order P1, P2, A1, A2, P3, A3**. **P3 requires one bounty claimed and A3 a second** (`"bounties": 1`
and `2` on those milestones), which is the only bounty gating in the vanilla pack. Milestones:
- **normal (22):** 1, 2, 5, 10, 15, 20, 30, 40, 50, 65, 80, 100, 120, 150, 180, 220, 260, 300, 350,
  400 (b1), 450, 500 (b2). Damage +2% at 40, 120, 260, 400, 500. Resistance 2% at 80, 180, 300,
  350, 450. Loot +10% at 10, 20, 80, 100, 150, 220, 300, 350, 400, 450. XP +5% at 65, 100, 120,
  180, 260, 300, 350, 400, 450, 500. Tracking P1 65 · P2 150 · A1 220 · A2 300 · P3 400 · A3 500.
  Attribute at 500.
- **elite (19):** 1, 3, 5, 8, 12, 16, 20, 25, 30, 40, 50, 60, 75, 90, 110, 130, 150 (b1), 175,
  200 (b2). Damage +3% at 12, 30, 75, 130, 175. Resistance 3% at 25, 50, 90, 150, 200. Loot +10%
  at 5, 25, 40, 60, 75, 110, 130, 150, 175, 200. XP +5% at 20, 30, 40, 50, 75, 90, 110, 130, 175,
  200. Tracking P1 20 · P2 40 · A1 60 · A2 90 · P3 150 · A3 200. Attribute at 200.
- **minor_boss (15):** 1–6, 8, 10, 12, 14, 16, 18 (b1), 20, 22, 25 (b2). Loot +20% and XP +10% at
  1, then +10% / +5% steps. Damage +2.5% at 3, 6, 14, 20. Resistance 2.5% at 4, 10, 16, 22.
  Tracking P1 2 · P2 5 · A1 8 · A2 12 · P3 18 · A3 25. Attribute half at 14, half at 25.
- **major_boss (11):** 1–8, 10 (b1), 12, 15 (b2). Loot +20% at 1, 3, 5, 7, 10; XP +10% at 1, 4, 6,
  8, 12. Damage +5% at 1, 4, 7, 12. Resistance 5% at 2, 5, 8, 12. Tracking P1 2 · P2 3 · A1 4 ·
  A2 6 · P3 10 · A3 15. Attribute half at 7, half at 15.

Mobs that deal no damage get no resistance rewards and no attack patterns (the generator keeps an
entry's existing choice). To regenerate after a schedule change, rebuild from each file's merged
text and attribute the way the one-off script did; there is no checked-in generator.

**Every entry grants one themed attribute**, sized by classification. Each attribute has a normal
"unit"; elites give 2×, minor bosses 4× and major bosses 8×. Normal and elite entries grant it at
their final milestone; bosses grant half at each of two milestones (minor 14 and 25, major 7 and 15).
Normals stay tiny because there are 44 of them and attributes stack across every entry.

| Attribute | Normal unit | Given by |
|---|---|---|
| `generic.max_health` | +0.5 | cow, giant, mooshroom, pig, zombie, zombie_villager · evoker (elite) · wither (major, **+6**) |
| `generic.armor` | +0.5 | husk, sheep, shulker · elder_guardian (minor, **+4**) |
| `generic.armor_toughness` | +0.25 | pufferfish · wither_skeleton (elite) |
| `generic.knockback_resistance` | +0.025 | hoglin, zoglin · ravager (elite) · warden (minor, **+0.3**) |
| `generic.explosion_knockback_resistance` | +0.025 | creeper, ghast |
| `generic.movement_speed` (`add_multiplied_base`) | +0.5% | pillager, stray, vex · illusioner (elite) · ender_dragon (major, **+10%**) |
| `generic.attack_damage` | +0.1 | blaze, polar_bear, vindicator, zombified_piglin · piglin_brute (elite) |
| `generic.attack_speed` (`add_multiplied_base`) | +1% | cave_spider, skeleton |
| `generic.movement_speed` (`add_multiplied_base`, was `jump_strength` on 1.21) | +1% | goat, magma_cube, rabbit |
| `generic.safe_fall_distance` | +0.25 | bat, chicken, phantom, slime |
| `generic.luck` | +0.1 | endermite, piglin, tropical_fish, witch |
| `generic.oxygen_bonus` | +0.2 | cod, glow_squid, squid |
| `generic.water_movement_efficiency` | +0.02 | drowned, salmon |

| `generic.step_height` | +0.05 | spider |
| `player.block_interaction_range` | +0.1 | enderman |
| `player.mining_efficiency` | +0.25 | silverfish |
| `player.submerged_mining_speed` (`add_multiplied_base`) | +2% | guardian |

Everything stacked together stays small except the major bosses, which are meant to feel earned:
+11.5 max health in total on this branch (see the port section for the remapped attributes), about
+12.5% movement speed. The bosses break the 8× rule on purpose (wither +6 health, ender dragon +10% speed, warden +0.3
knockback resistance, elder guardian +4 armor); everything else follows it.
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

Resistance is only granted against mobs that deal damage. The dev fast zombie is a copy with its 22
milestones at kills 1–22, **no bounty gating**, and +5 max health at the end instead of the zombie's
+0.5, so a dev run can max it with kills alone.

### Kill credit

`LivingDeathEvent` at `EventPriority.LOWEST`, so totem and revive mods cancel first (cancelled events
are not received). If the victim's type has an entry, every player in
`CombatTracking.participants(victim, KILL_CREDIT_WINDOW_TICKS)` gets one kill. That function comes
from the mod's own `feature/combat` package, inlined from SilverLib (which has no 1.20.1 build). It
covers group fights, pets and summons (owners of `OwnableEntity` / `TraceableEntity`), and players who
buffed a damager with a beneficial effect. Kills of untracked types are never stored, so the saved map
can't grow without limit.

All progress goes through **`BestiaryProgress.set`**, usually via `addKills` / `addBounties`. It
writes the persisted map, sends the delta payload, and reapplies attributes only if the reached
milestone changed. `addBounties` is called by the command and by a completed bounty.

Progress is keyed by `ResourceLocation`, not `EntityType`, so counts for a removed mod's entities are
kept and come back if the mod returns. The map lives in the player's `PlayerPersisted` NBT, which Forge
copies on every respawn.

### Bonuses

There are two kinds of bonus, and they work differently on purpose.

- **Damage against one creature type is not an attribute.** When the datapack loads, each
  `BestiaryEntry` builds a `Bonuses[]`, one per milestone. Entry `i` holds the final values with
  every reward up to and including milestone `i` already folded in, so `1 + Σbonus` and
  `max(0, 1 − Σresistance)`. No milestone reached means `Bonuses.NONE`. Finding the reached milestone
  from progress is a short loop over the sorted requirements (vanilla entries have up to 22). After
  that, the lookup is a single array index.
- One `LivingHurtEvent` handler applies both:
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
  datapack reload (all online players), and command changes. On respawn (not an End exit) health is
  set to the new max afterwards, because vanilla's `restoreFrom` already set it to the base max.
- **Modifiers are transient (`addTransientModifier`), never saved with the player.** The player
  stores only progress; bonuses are always computed from the loaded datapack, so changing reward
  data never leaves stale modifiers in player saves. Accepted cost: health is clamped to base max
  health when the player loads, before our modifiers apply, so a player above base max loses that
  extra health on relog. Do not "fix" this by saving modifiers or health.

### Sync

Forge 1.20.1 has no data sync of its own, so it is done by hand over the mod's `SimpleChannel`:

- **Entries**: `OnDatapackSyncEvent` covers both join and `/reload`. The entries go out in
  **chunks of at most about 200 000 JSON characters** (`Entries.chunked` measures each entry; the
  first chunk carries `first = true` and the client replaces its map, later chunks merge in), sent as
  UTF-8 bytes (`writeByteArray`, not `writeUtf`, whose 32 767-character cap would reject big packs)
  under the 1 MiB custom-payload limit. Each payload carries the
  authored milestones as JSON via `FriendlyByteBuf.writeJsonWithCodec` (text is `TextCodec`, JSON
  components, so no registry context is needed).
  NBT maps lose order, which is why stats are shown sorted. The client rebuilds the precomputed
  arrays with the same code.
- **Progress**: a full map on login (`AllProgress`), then a single `ProgressUpdate(entity id,
  progress)` per change. When a
  delta reaches a milestone, the client shows a toast ("New Creature Recorded" / "Knowledge Deepened"),
  with the creature's name only if it has been revealed.
- Server state (`BestiaryEntries`) and client state (`ClientBestiary`) are separate holders, even in
  singleplayer.
- **Every payload goes through `core/Network.sendIfConnected`**, which skips a connection whose
  channel is missing or that hasn't negotiated the mod's channel (`SimpleChannel.isRemotePresent`);
  gametest mock players never negotiate one.

### The book

- A `bestiary` item. Right-click opens `BestiaryScreen`. Crafted shaped: leather over a book over a bone, one column. Kills
  are tracked whether or not the player has the book.
- On the left, the paged list of discovered entries (no discovered count for now). On the right,
  the selected entry: name, model, type, stats, description, strengths, weaknesses, habitat, notes,
  bonuses earned, and the next milestone's requirement (kills, bounties, or both), or "Entry complete. Nothing left to learn." once every milestone is reached.
- **List order**: the tracked creature (if any) is pinned to the top, then revealed names
  alphabetically, then unrevealed `???` entries. Opening the book while tracking selects that
  creature (it's first, and nothing else is selected yet) and opens on the Tracking tab, so Stop is
  one click away; the pin and the tab follow tracking changes live via `refreshIfOpen`.
- **Search**: an `EditBox` above the list. It filters the list on every keystroke with a
  case-insensitive substring match on the *revealed* name only, so a `???` entry can't be found by
  name before its name is revealed. The filtered list is rebuilt when the query changes, never
  per frame.
- **Tabs are pages inside the selected entry**, shown on the right-hand page:
  - **Overview**: name, model, type, description, progress to the next milestone.
  - **Knowledge**: the stats at the top, then Attack Patterns, Strengths, Weaknesses, Habitat and
    Notes sections. There is no separate Stats tab.
  - **Tracking & Bounties** (shown once the creature has tracking unlocked, bounties claimed, an
    active bounty, or an entry that requires bounties: `BestiaryEntry.usesBounties`): the Track / Stop Tracking button at the top
    (only with tracking unlocked), then "Bounty: 2 of 5 slain" when this creature is the active
    bounty, then whether it's tracked and the passive/active level descriptions. Overview shows
    "Slain: N" and "Bounties completed: N" (the two numbers `Progress` stores per creature).
    The button is vanilla-sized (20 px tall, label centred, 8 px side padding) and drawn from the
    mod's own `track_button` / `track_button_highlighted` nine-slice copies of `widget/button`.
  - **Rewards**: two clearly separated sections, each a bold heading plus a grey italic hint:
    - **"Against the <name>"** ("Against This Creature" while the name is hidden), hint "Only while
      fighting it": the per-creature `Bonuses` (increased damage, less damage taken, extra loot roll
      chance, bonus experience).
    - **"Permanent Boons"**, hint "Always active, wherever you roam": the attribute rewards, summed
      per attribute and operation, in vanilla attribute wording ("+0.5 Max Health").
    A section with nothing earned is left out.

  Tabs with nothing revealed are hidden. Clicking a different creature in the list returns to Overview (the
  model tab); refreshes and reopening keep the current tab, falling back to Overview if it is hidden. Each page turns with its own `page_backward` /
  `page_forward` arrows at the bottom (shown only when there is a previous / next page, with a
  "1 / 3" page number between them and the vanilla page-turn sound). The left page pages the
  creature list (11 rows a page), the right page pages the selected tab's text. There is no mouse
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
    - `icon_overview`, `icon_knowledge`, `icon_rewards`, `icon_tracking`: 16×16 tab icons (copies
      of the bestiary item, book and quill, emerald, spyglass). `icon_overview` is also the toast icon.
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
  Non-living or null results fall back to the placeholder. Rendering goes through the mod's
  `EntityPreview.render` (the 1.20.1 `renderEntityInInventory` has no centring vector), inside a
  scissor so large mobs can't spill over the page.
- Wrap the text into lines once, when the selection changes, not every frame. The same goes for
  everything else drawn per frame: row name lines are wrapped in `refresh()`, the header name line
  and Track button label/width in `rebuildHeader()`, and tab/arrow sprite ids are constants. The
  eye-icon space is always reserved in rows so the name width never depends on tracking state.

### Tracking (`feature/tracking/`)

You track **one** creature at a time, chosen with the **Track** / **Stop Tracking** button on the book's
**Tracking tab**. The tab appears once that creature has any tracking unlocked; it holds the
button, whether you're tracking it, and a plain-language description of the passive and active
levels you have. **No numbers there**: it says what each level does in the book's voice ("A compass
points the way…"), never ranges, seconds or percentages, so config changes never make it wrong.
The only concrete detail is the current hunt key binding (`TrackingClientEvents.huntKeyName()`). An eye icon marks the tracked creature in the list. There are two modes, each with
**3 levels** unlocked by milestone rewards `{"type": "passive_tracking" | "active_tracking", "level": 1-3}`.
Each is precomputed as the highest level reached, into `Bonuses.passiveTracking` / `activeTracking`.

- **Passive** (`PassiveTrackingLevel`) is a fading ping (every 25 / 19 / 15 s at P1 / P2 / P3), describing only the nearest one. Nothing in range means no ping and no text.
  and relative to where you faced at ping time:

  | Level | Range | The ping gives |
  |---|---|---|
  | P1 Scent | 24 | "You sense the Zombie to the left." 4-way. More than 4 blocks off, the height leads: "below you and to the left" (`TrackingHeight`). |
  | P2 Trail | 32 | "You're tracking the Zombie about 30 blocks away, ahead to the left." 8-way, height hint, distance rounded to the nearest 10 ("about", never "within": 34 rounds to 30). |
  | P3 Track | 48 | "You've caught the scent of the Zombie about 30 blocks away, below you and to the left." One sentence, plus a **wisp trail**: a mod particle that spirals (radius 0.25, 0.4 rad/tick) from your eyes toward the creature at 0.45 blocks/tick, one particle a tick. It stops after 24 blocks and always at least 8 blocks short of the creature, so it points the way without marking it; within about 10 blocks there is no trail. |

  The wisp is the registered particle `beasts_and_bounties:tracking_wisp`. It is `WispParticle`: vanilla
  end-rod motion at half the size, colour 0.8, max alpha 0.55 fading out over the second half of its
  life, with no fade-to-cream. It uses the mod's own textures
  `textures/particle/tracking_wisp_0..7` (copies of vanilla glitter). It is spawned client-side by
  `WispTrail` from the target position in the ping, so only the tracking player sees it.
  Vanilla Java has no directional hit marker, only the camera tilt toward a hit, so the wisp is how
  the creature is pointed out.
- **Active** (`ActiveTrackingLevel`) is the "Hunt Tracked Creature" key (default **G**),
  **held for 2 s** (`huntHoldSeconds`). The hold is client-side (`ClientTracking.tickHuntKey`):
  while held, "Searching for the Zombie's trail" and a thin progress bar show above the hotbar; on completion
  the client sends `StartHunt`, and the key must be released before another attempt. On the first
  tick of a press that can't start a hunt, the reason goes to the action bar and the hold doesn't
  begin: not tracking, active not learned, or "The trail is cold. Ready in Ns." (during a running
  hunt the key does nothing). The server validates again and uses the same messages. A hunt runs
  for a duration and then a cooldown; a short, quiet sniff (`tracking.hunt_start`, the mod's own
  sound event delegating to vanilla's fox sniff at volume 0.4, `assets/.../sounds.json`) plays for
  that player when it starts.
  While hunting, a faint **compass ring** (`tracking_ring`, 44×44 PNG drawn at 30×30) sits
  **halfway between the crosshair and the top of the hotbar** (`compassCenterY`), not on the
  crosshair, so it never blocks the player's view. It can overlap the action bar text. The arrow
  (16×16 PNG drawn at 11×11) rides on the ring at the target's bearing (radius 14), rotated to
  match, with the
  up/down marker beside it when the target is more than 4 blocks off. This reads as a compass, so
  an arrow at the bottom means "behind", not "below". The compass shows for the whole hunt whenever
  there is a target, glow or no glow: a glowing creature straight below you or just past a wall
  edge is easy to miss, and hiding the arrow made A2/A3 feel like a downgrade from A1. The glow is
  purely additive. The hourglass hunt timer (icon + seconds) sits 3 px above the ring, and it is
  **transient**: it appears for 5 s and fades over 1 s whenever the hunt phase changes (hunt
  starts: "30s" counting down; hunt ends: "Ready in 240s" counting the cooldown) and again whenever
  the hunt key is pressed while a hunt can't start (`ClientTracking.showTimer`, driven from the HUD's
  per-frame `noticeHuntPhaseChanges`). The compass stays up for the whole hunt regardless. Outside a hunt the hold text and bar use the status
  slot at 84. Passive ping text is hidden while hunting (the compass already points the way and the
  ping's slot at 100 overlaps the compass); the wisp still shows. The crosshair area is left clear.

  | Level | Duration / cooldown | Arrow range | Glow |
  |---|---|---|---|
  | A1 Hunt | 30 s / 240 s | 48 | none |
  | A2 Sight | 30 s / 180 s | 48 | nearest within 32 |
  | A3 Mark | 30 s / 150 s | 64 | every one within 32 |

  Glow is for that player only, via two client-only mixins in
  `beasts_and_bounties.tracking.mixins.json`: `MinecraftMixin.shouldEntityAppearGlowing` turns it
  on, and `EntityMixin.getTeamColor` tints the outline blood-red (`ClientTracking.GLOW_COLOR`,
  `0xFF5A5A`) so it reads as the bestiary's and not a spectral arrow.
- **HUD text colour.** The ping text, the hold text ("Searching for the Skeleton's trail") and the hunt timer's seconds are
  a light parchment off-white (`0xEADFC8`) with **no drop shadow**, by the user's choice (pale
  yellow vanished on bright scenes, black on dark ones). The hold bar fill stays vanilla gold
  (`0xFFAA00`).
- **Transient text.** One-line messages (hold refusals, the server's fallback messages) use the
  action bar (`Gui.setOverlayMessage`). **Pings are drawn by the HUD itself**, because the action
  bar neither wraps nor stays longer than 3 s: the ping is wrapped once on receive
  (`ClientTracking.receivePing`, width min(280, screen − 40)), drawn centred and bottom-anchored
  100 px above the bottom (so it grows upward and never collides with the status slot at 84), held
  readable for `pingTextSeconds` (client config, default 4 s) and then faded over 2 s. Switching or
  stopping tracking drops the ping and stops the wisp.
- **Config** (all tables above are the defaults):
  - `TrackingConfig` (SERVER, `beasts_and_bounties-tracking-server.toml`): per passive level the
    `range` and `pingIntervalSeconds`; per active level the `durationSeconds`, `cooldownSeconds`
    and `arrowRange`; plus `glowRange`, `huntScanIntervalTicks`, `huntHoldSeconds`,
    `heightHintBlocks` and `distanceRoundingBlocks`. Forge syncs server configs to clients, so the HUD's above/below
    marker uses the same threshold.
  - `TrackingClientConfig` (CLIENT, `beasts_and_bounties-tracking-client.toml`): the wisp's
    `blocksPerTick`, `maxLengthBlocks`, `stopShortBlocks`, `swirlRadius` and `swirlRadiansPerTick`,
    plus `pingTextSeconds` (how long a ping stays readable).
  - What each level *does* stays in code: 4-way or 8-way, whether it gives a distance or a wisp,
    and the glow scope. Those are fixed fields on `PassiveTrackingLevel` / `ActiveTrackingLevel`,
    whose numeric getters read the config.
- **State**: `PlayerData` `tracking` =
  `TrackingState(Optional<creature>, huntEndsAt, cooldownEndsAt)`. The two times are game ticks.
  It is persisted and copied on respawn. **Absent means not tracking.** Stop removes it, except
  during a cooldown, when a creature-less copy keeps `cooldownEndsAt` so Stop/Track can't reset the
  cooldown. The scanner removes that copy once the cooldown ends. Switching creatures ends the hunt
  but keeps the cooldown. **Levels are never stored**; they are read from progress so datapack
  changes apply at once. The Track / Stop / hunt rules are pure methods on `TrackingState`
  (`tracking`, `stopped`, `huntStarted`) and are gametested.
- **Performance**: `TrackingScanner` runs on `PlayerTickEvent.Post`.
  - A player with no tracking state costs one cached lookup.
  - Passive scans at the level's ping interval (500 / 380 / 300 ticks) and active every 10 ticks, staggered by
    `(gameTime + player.getId()) % interval`.
  - A scan is one `level.getEntities(type, box, filter)`, answered from entity sections, so
    tens of microseconds.
  - Passive bonuses are looked up once a second, not every tick, which is why ping intervals are
    whole seconds. If the tracked creature's unlocks vanish (reset, datapack change), that check
    stops tracking.
  - `HuntUpdate` is only sent when the target, the glow set (ids sorted) or the target's position
    (moved over 4 blocks) changes; `TrackingScanner`'s in-memory `LAST_HUNT_UPDATES` map (by player
    UUID, dropped on logout) remembers the last one, and because it is keyed by UUID it survives
    death on its own, since the hunt itself
    survives death and the client would otherwise keep a stale target. The client follows the
    live entity for the arrow when it has it loaded.
  - Pressing the hunt key during a hunt does nothing, on both sides.
- **Sync**: `StateSync` on join (`OnDatapackSyncEvent`) and on every change, through
  `Network.sendIfConnected`. `ClientTracking.clear()` runs on logout so no stale glow carries
  across worlds, and switching or stopping clears the last ping text and wisp at once.
- **Names**: tracking text uses `BestiaryEntry.displayName`, the same `???` gating as the book, so a
  pack that unlocks tracking before the name reveals nothing early.
  **Bounty text is the exception**: the poster, "Bounty accepted" and the Bounty Claimed toast use
  the entity type's own name (`EntityType.getDescription()`) whatever the player has learned,
  because a board asking you to slay "???" makes no sense; the poster's model is shown too.
- **Vanilla pack placement** (kills for P1, P2, A1, A2, P3, A3; P3 also needs 1 bounty, A3 2):
  - normal: 65, 150, 220, 300, 400, 500
  - elite: 20, 40, 60, 90, 150, 200
  - minor boss: 2, 5, 8, 12, 18, 25
  - major boss: 2, 3, 4, 6, 10, 15
  - dev zombie: kills 10, 14, 16, 18, 20, 22 (no bounties).
- **Sprites**: `tracking_ring` (generated, 44×44, faint white circle), `tracking_arrow`,
  `tracking_up` / `tracking_down` and `tracking_timer` are placeholders; `tracked_eye` is hand-drawn,
  9×9, drawn at native size centred in the 11 px list row;
  `icon_tracking` (the Tracking tab) copies the vanilla spyglass item. `track_button` (+ `_highlighted`) copies the vanilla `widget/button`
  nine-slice.

### Advancements

Three vanilla advancements in `data/beasts_and_bounties/advancements/`, each with one
`minecraft:impossible` criterion that `BestiaryAdvancements` awards from code inside
`BestiaryProgress.set`, the one place every progress change passes through (kills, bounties,
commands). They are trophies only: nothing reads them back.

| Id | Title | Awarded when | Frame |
|---|---|---|---|
| `root` | A Hunter's First Page | a creature's milestone index rises for the first time (it is now listed) | task, toast only |
| `entry_complete` | Nothing Left to Learn | any entry reaches its final milestone | goal, announced |
| `bestiary_complete` | Master of Beasts | every **loaded** entry is at its final milestone, checked after each completion | challenge, announced |

The tab's icon is the bestiary item and its background `textures/gui/advancements/bestiary.png` is
a 16×16 parchment tile cut from the book spread. `/bestiary complete` awards them like any other
progress; `/bestiary reset` does not revoke them (advancements are never un-granted). A gametest
walks a mock player through all three.

### Commands

`/bestiary` is admin-only (permission level 2, `Commands.LEVEL_GAMEMASTERS`).
- `/bestiary kills <set|add|remove> <targets> <entity> <amount>`
- `/bestiary bounties <set|add|remove> <targets> <entity> <amount>`
- `/bestiary reset <targets> [entity]` clears kills and bounties for one creature, or everything.
- `/bestiary get <target> <entity>` prints kills, bounties and milestone `n of m`.
- `/bestiary complete <targets> [entity]` raises progress to the final milestone's requirement (never
  lowers it) for one entry or every loaded entry. Use it to test a maxed-out bestiary.
- `/bestiary board reroll` gives **everyone** new postings now (the world-wide `BountyBoardData`
  reroll counter salts the offer seed, and the new board excludes the old one's creatures), and `/bestiary board clear <targets>` drops the
  targets' active bounty and cooldown. Registered by `BountyCommand`; Brigadier merges its `bestiary` literal with the
  bestiary feature's.

Counts clamp to 0 and `Integer.MAX_VALUE`. Entity suggestions come from the loaded entries. Every
change syncs the client and reapplies attributes when the reached milestone changes.

## Build & run

```bash
./gradlew build              # compile + jar
./gradlew runClient          # dev client
./gradlew runServer          # dedicated server (needs eula=true in run/eula.txt)
./gradlew runGameTestServer  # gametests; also a server-dist load, so it catches client class leaks
```

There is no datagen and no `data` run config.

- **CI**: `.github/workflows/build.yml` builds every push and pull request; `publish.yml` runs
  `./gradlew publishMods` on a published GitHub release (or by hand with a dry-run option that
  posts to the test Discord webhook instead). Publishing is CurseForge only, through the
  mod-publish-plugin block in `build.gradle`: it uploads the jar with the `CHANGELOG.md` text as
  the changelog and `project-mmo` optional. The project id
  lives in `gradle.properties` as `curseforge_project` (1693724) and
  the secrets are `CF_TOKEN`, `DISCORD_WEBHOOK_URL` and `DISCORD_WEBHOOK_TEST_URL`. Same setup
  as Mortal Boons minus Modrinth.

- Versions come from `gradle.properties` only. `src/main/resources/META-INF/mods.toml` and `pack.mcmeta`
  are expanded by `processResources`.
- No SilverLib: its combat tracking is inlined (see Kill credit). Project MMO is `compileOnly fg.deobf`
  (file 8303706 = PMMO 1.7.41) and optional in the mods.toml.
- No datagen in v1. Entries, the item model, the recipe and lang are plain JSON.
- **Dev-only fast zombie:** `src/main/resources/builtin_data_packs/dev_fast_milestones/` is a
  datapack registered by `DevFastMilestonesPack` (in the `gametest` package); both are excluded
  from the jar by `build.gradle`. It replaces the
  vanilla zombie with a copy whose 22 milestones sit at kills 1 through 22, so every zombie kill in a
  dev run unlocks the next milestone. It ranks above the vanilla pack, which a gametest checks:
  1.20.1 orders auto-added packs **by pack id** (`PackRepository` collects them in a `TreeMap`;
  `Pack.Position` only matters in the selection screen), so the dev pack's id is
  `beasts_and_bounties:dev_fast_milestones`, which sorts after
  `beasts_and_bounties:builtin_data_packs/vanilla_bestiary`.
  The pack also gives the bat a very long description (a plain string, merged onto the bat's
  5-kill milestone) to exercise paging on the right page. Neither the pack nor the class ships in the jar. Tests that need vanilla thresholds use the
  skeleton instead.
- Gametests live in the `net.silvertide.beasts_and_bounties.gametest` package of the main source set
  (excluded from the jar, as Player Abilities does), in `BestiaryGameTests`,
  `TrackingGameTests` and `BountyGameTests` (shipped bounties match entries, deterministic offers,
  accept/abandon rules, kills completing a bounty with rewards and progress, persistence). They cover data loading and merging, milestone maths, attribute reapply,
  kill credit end to end, the damage/loot/experience hooks (posted as real events), the admin
  commands, persistence and respawn copying (`ServerPlayer.restoreFrom`), tracking rules, scans and
  direction maths. **Mock players come from `MockPlayers.inLevel(helper)`**, not
  `helper.makeMockServerPlayerInLevel()`: Forge's login path calls `NetworkFilters.injectIfNecessary`,
  which dereferences the connection's netty channel, and vanilla's 1.20.1 mock has none; the helper
  wraps the `Connection` in an `EmbeddedChannel` first.
  Package-private helpers (`BestiaryEntries.mergeInPackOrder`, `TrackingScanner.passivePing`) exist
  for them. Rendering is checked in game.

## Conventions & gotchas

- Client-only code lives in `feature/*/client/` and `feature/tracking/mixin/` (the mixin json's
  `client` list) and is reached only through payload handler lambdas, `Dist.CLIENT` subscribers and
  `BestiaryItem.use` behind `isClientSide()`. Common code may call into it only from inside a lambda
  or a client-only branch, never through a field type, cast or annotation. `runGameTestServer`
  catches leaks.
- Attribute ids carry the `generic.` prefix (`minecraft:generic.max_health`); Forge adds
  `forge:swim_speed`, `forge:block_reach`, `forge:step_height_addition` and friends.
- `LivingHurtEvent` runs before armour, so bonuses scale the raw hit; combat participation is
  recorded there too (LOWEST priority), capped at the victim's health plus absorption.
- **Wording.** Bonus lines say what changes for the player, never "dealt" / "resisted": "+10%
  increased damage", "10% less damage taken", "50% chance of an extra loot roll", "+50% bonus
  experience". Each per-creature reward has a `.more` and a `.less` lang key chosen by sign, so a
  datapack penalty still reads correctly. Counts use singular keys (`requirement.kills.one` →
  "1 kill"). The book is "Hunter's Bestiary"; sections read "How It Fights", "Where It Dwells" and
  "Hunter's Notes". Tracking text names the creature with "the" ("You sense the Zombie…"),
  never "a", which breaks before vowels ("a Enderman"). No ellipses in HUD text.
- **Two text sizes** (the bounty poster's "Slay N" and reward detail lines are the exception: the user
  asked for them slightly smaller, so they use 0.9 × a snapped scale and may look a touch soft). Titles and the creature list draw at normal size. The entry body (description,
  stats, sections, rewards) draws smaller, at a target of 0.75 **snapped to whole screen pixels**:
  `round(guiScale × 0.75) / guiScale` (GUI scale 3 gives 0.67, 4 gives 0.75, 2 stays 1.0). The
  Minecraft font is a bitmap, so any other scale blurs it. Body lines wrap at `width / scale`, and
  the lines per page divide by the scale, so more of a description fits on one page. The scale is
  computed in `init()` via `GuiText.pixelSnappedBodyScale()`, shared with the bounty board.
- `GuiGraphics.blit` **does not enable blending**; vanilla widgets call
  `RenderSystem.enableBlend()` themselves. Any sprite with soft or faint edges (the bounty cards,
  the accept/abandon text, the tooltip, the hunt compass and its markers) must be drawn between `enableBlend()` + `defaultBlendFunc()`
  and `disableBlend()`, or its near-transparent edge pixels draw at full strength as stray colours.
- Text on the parchment is drawn **without a drop shadow** (`drawString(..., false)`). Vanilla widgets
  default to a shadow, which smears dark text; 1.20.1's `EditBox` has no `setTextShadow`, so the
  search text keeps its shadow.
- **Milestones are the only progression and reward mechanism.** Advancements exist only as
  trophies (see Advancements): they never gate or grant anything, and no criterion or trigger is
  authored; the mod awards them from code.
- `@Mod.EventBusSubscriber` defaults to the FORGE bus; a class holding mod-bus events
  (`RegisterKeyMappingsEvent`, `RegisterGuiOverlaysEvent`, `RegisterParticleProvidersEvent`,
  `AddPackFindersEvent`) must say `bus = Mod.EventBusSubscriber.Bus.MOD`, and such events never
  share a class with game-bus ones. Registrations that need the bus in hand (`DeferredRegister`,
  configs, creative tabs) go in `XFeature.init`; payloads register in the mod constructor.
- **Gametest mock players never tick as players.** `PlayerTickEvent` fires from
  `ServerPlayer.doTick()`, which the connection drives, and a mock player's connection is never
  ticked. A test that needs the scanner (or anything on `PlayerTickEvent`) posts
  `new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player)` itself, e.g. from `helper.onEachTick`. Mock players also
  never negotiate mod channels, which is what `Network.sendIfConnected` guards against.
- Forge serializes `SimpleChannel` packets even in singleplayer, but anything sent is still built
  from immutable data or a copy (the kills map is sent as `Map.copyOf`), never the live server object.

### Bounties (`feature/bounty/`)

A **bounty board** block (a free-standing sign on a post: `BountyBoardBlock`, a
`HorizontalDirectionalBlock` with no block entity) posts hunts. **The postings are shared by the
whole server** (one daily board, so friends simply take the same posting; there is no party or
team system), while progress, the active bounty and cooldowns are **per player**. The board block
is only where you look. One bounty is active at a time.

- **Data**: `data/<ns>/bounty/<entity_path>.json`, path-is-the-id like entries, loaded by a
  `SimpleJsonResourceReloadListener` (**top pack wins, no merging**). Fields: `kills` (default 3),
  `weight` (default 10), `bonus_kills` (default 0) and `rewards`, a `type` dispatch of `item`
  (`item`, `count`), `experience` (`amount`) and `pmmo_xp` (`xp`: `"fishing/1000"`, a Project MMO skill and
  the XP to give it, see the PMMO bridge below). A file whose entity doesn't exist is skipped with a
  WARN; one whose entity has **no bestiary entry** loads but is never offered (checked at offer
  time, because reload-listener order isn't guaranteed). Shipped in the vanilla pack for hostiles
  and bosses only, **tuned per creature** rather than per classification: kills, weight, XP and
  item counts all vary with how common the creature is, how hard it is to kill, how many the
  posting asks for and how hard it is to find. The spread: normals ask 2–8 kills for 100–300 XP
  and 12–16 iron / 8–14 gold / 8–12 emeralds / 2–4 diamonds (zombie 8 kills, 12 iron, 100 XP at
  the cheap end; endermite 2 kills, 4 diamonds, 260 XP and shulker 4 kills, 4 diamonds, 300 XP at
  the dear end); elites ask 1–4 kills for 350–700 XP and 4–8 diamonds plus 12–24 of their theme
  (iron for wither skeleton, gold for ravager and piglin brute, emeralds for evoker and
  illusioner); minor bosses 800 / 1000 XP with 2 / 3 netherite + 10 / 12 diamonds (elder guardian,
  warden); major bosses 1500 / 2000 XP with 4 / 5 netherite + 16 / 20 diamonds (wither, ender
  dragon). Weights follow commonness (12 for zombie, creeper, skeleton, spider; 3 for zoglin,
  endermite, ravager, evoker; 1 for the bosses and the illusioner, which never spawns naturally).
  Bonus kills are per classification: **+10 for normals, +5 for elites, +1 for bosses**, on top of the real kill and the +1 bounty. Items are ingots and gems only, never
  blocks, at most two kinds per bounty so the poster's icon row stays short. From level 20, 100 XP
  is about 1.5 levels and 2000 about 22 (vanilla's per-level cost is 2L+7 up to 15, 5L−38 to 30,
  9L−158 after). The numbers live in the JSON files; `everyShippedBountyMatchesAnEntry` pins the
  zombie's.
- **Offers** (`BountyOffers.pick`): `offersShown` distinct weighted picks from a `RandomSource`
  seeded by the overworld seed, `gameTime / refreshTicks` and the world's reroll count, so the same
  period gives every player the same postings. **A new board never repeats the previous one**:
  `BountyBoardData` (a `SavedData` on the overworld) remembers the reroll count plus the last
  board's period, reroll count and creature ids; `current()` returns the remembered ids while
  they are current and otherwise picks with those ids excluded (unless that would leave fewer
  candidates than `offersShown`) and records the result. Weights (`weight` in each bounty file)
  make common creatures show up more and rare ones less. The server sends them when the board opens
  (`BoardOpened(offers, periodEndsAt, state)`, each offer carrying the kills, item stacks, XP and
  habitat text the card shows) and recomputes them to validate `AcceptBounty`.
- **State**: `PlayerData` `bounty` = `BountyState(Optional<ActiveBounty(entityId, killsRequired,
  killsSoFar)>, cooldownEndsAt)`, persisted and copied on respawn; absent = `NONE`. (An old save's
  `rerolls` field is ignored by the codec.) Rules are pure:
  `canAccept(now)`, `accepted`, `killRecorded` (only for the matching creature), `ended(now,
  cooldown)`. Accept needs no active bounty and an elapsed cooldown. Abandon (`allowAbandon`) and
  completion both end the bounty with their own cooldown (`abandonCooldownMinutes`,
  `claimCooldownMinutes`, real minutes). An active bounty never expires on its own.
- **Kills**: `BountyEvents` on `LivingDeathEvent` (LOWEST) gives every `CombatTracking.participants`
  member with a matching active bounty one kill ("Bounty: 2 of 5 slain" on the action bar). On
  completion: item rewards via `ItemHandlerHelper.giveItemToPlayer` (overflow drops), XP via
  `giveExperiencePoints`, then `BestiaryProgress.addBounties(1)` and `addKills(bonus_kills)`, the
  claim cooldown, the mod's `bounty.claimed` sound (delegates to vanilla's villager "yes", volume
  0.6) and a `BountyClaimed` payload for the client toast. **Accepting changes nothing in the
  book**; the kill and the bonus kills do.
- **PMMO bridge** (`PmmoBridge`): Project MMO is an **optional** dependency (`compileOnly fg.deobf`
  `curse.maven:pmmo-353935:8303706` = PMMO 1.7.41, `mandatory=false` in the mods.toml, not on
  the dev runtime). A `pmmo_xp` reward (`"xp": "skill/amount"`) is given on completion through `Core.get(level).getData().setXpDiff(uuid,
  skill, amount)` only when PMMO is loaded (`ModList.isLoaded`, read once into a static final on first use); without PMMO the reward parses and is
  silently skipped. `PmmoBridge` is the only class that imports `harmonised.*`, and it is reached
  only behind `PmmoBridge.isLoaded()` so the class never loads without the mod. With PMMO
  present the server sends each skill reward as a ready-made "1000 Fishing" line (the skill name
  via PMMO's own `pmmo.<skill>` lang key). The poster draws them on the experience line after "+60 XP". The shipped pack grants none; it is for
  packs that run PMMO.
- **Config** (`beasts_and_bounties-bounty-server.toml`): `refreshMinecraftDays` (1.0),
  `offersShown` (3, 1–3), `claimCooldownMinutes` (10), `abandonCooldownMinutes` (20),
  `allowAbandon` (true), `showLastSeen` (false: when off the server sends no habitat, so the
  posters have no "Last seen:" tooltip). Per-creature numbers are data, never config.
- **Board screen** (`BountyBoardScreen`, opened by the `BoardOpened` handler): background
  `textures/gui/bounty_board.png` (352×188, hand-painted); its usable area is `(19, 21)` 314×145,
  and the screen draws nothing outside it (no drawn title: the texture owns the frame). Postings
  are **"WANTED" posters** on the hand-painted `bounty_card` (88×101, edge to edge): three fit
  across with 13 px margins and 12 px gaps, which is why `offersShown` is capped at 3. Each holds,
  top to bottom at fixed offsets, the hand-painted `wanted_text` heading (57×16, drawn 43×12, 6 px from the top), the creature's
  model (28 px box at 20, scissored to that box in screen space, transformed by the hover scale,
  so tall mobs never reach the heading), then the name (bold, upper case, one line; body-text scale when it fits the paper, else
  shrunk to the largest pixel-snapped scale that fits, down to the small scale, and past that cut
  with "..." and the full name put at the top of the poster's hover tooltip; shorter text is
  vertically centred on the body-scale line)
  and "Slay N" (0.9 × body scale). Below that, laid out line by line from "Slay N" down, the
  rewards: the hand-painted `reward_text` heading (58×16, drawn 44×12, centred), then two parts: an
  **experience line** (small scale, centred) with "+60 XP" and, with PMMO, each skill reward as
  "+1000 Fishing XP" beside it; then the **item rows**: 8 px icons (`renderItem` +
  `renderItemDecorations`, so the count sits on the icon like a stack), centred, up to 9 a row
  and at most two rows. Past that (`itemsThatFit`, computed once per poster) the last row ends
  with "+N" in detail text (0.9 × small scale) and the poster's hover tooltip lists every reward
  ("Rewards:", the XP lines, then "3× Emerald" lines) above the "Last seen:" habitat. N is experience **points** (`giveExperiencePoints`), not levels, because a
  level's cost depends on the player's current level. A line with nothing to show is left out. The bottom of the paper is left clear. Hovering a poster swaps in `bounty_card_highlighted`, scales it
  up 10% about its centre and pushes its drop shadow from 2 px to 4 px (the shadow is the paper's own silhouette, drawn black and offset down-right, so the ragged edge casts a matching shadow) and only
  then **fades the reward details to 25%** (everything under the Rewards heading: the XP line and item rows; the paper's own region is blitted back over them at 75% alpha, translated to z 300 so it passes the depth test against the items drawn at z 150 and their stack counts at z 200) and shows its Accept action centred in that faded area: the hand-painted `accept_text` sprite (88×16, swapped for `accept_text_highlighted` under the mouse), drawn at 3/4 size (66×12) inside the hover-scaled pose; the click area is that rectangle mapped through the hover scale (`isOverButton`). The model stays untouched. The active poster uses `abandon_text` (92×16, drawn 69×12) the same way. The bottom of the paper below the reward line is free.
  Hovering a poster shows the creature's **habitat lines from its entry's full knowledge** (a
  poster says where it was seen, whatever the player has unlocked) in a `Tooltips` tooltip headed
  "Last seen:"; there is no room on the paper. The model faces the viewer (yaw 180 + 25: in this
  projection 180° is "toward the camera", 0° is away) and is **sepia-tinted by
  `SepiaBufferSource`**, a `VertexConsumer` wrapper that multiplies every vertex colour, because
  `RenderSystem.setShaderColor` does not reach entity rendering here; the render is a copy of
  `renderEntityInInventory` with that buffer source. With a bounty active the postings are replaced
  by one poster for the target with "2 of 5 slain", its reward line and an Abandon button (with a bounty active the server sends that bounty's own definition as the only offer, since it is usually not in the current period's postings), footer "Finish this hunt
  before taking another." On cooldown (after completing or abandoning) the board is **empty** and
  the footer reads "More bounties should arrive in 20 minutes."; otherwise "New postings in 14
  minutes." Durations use whole words with singular keys ("1 minute", "45 seconds"). Accept/Abandon send their payload and **the board stays open**: the server
  answers a successful accept or abandon with a fresh `BoardOpened` flagged `refreshOnly`, and the
  client's handler updates an already-open `BountyBoardScreen` in place (`show`, which re-runs
  `init`); a refresh that arrives after the player closed the board is dropped rather than
  reopening it. A refused accept (cooldown, not offered) only shows its action-bar message.
- **Tooltips** (`bestiary/client/Tooltips`): the one tooltip renderer (mod `tooltip` nine-slice,
  multi-line, kept on screen), used by the book's tab names and the board's habitat.
- **Book**: list order is bounty creature first (warm tint, `bounty_mark`: a 9×9 black crosshair),
  then the tracked creature (cool tint, `tracked_eye`), then the rest; a creature that is both
  shows `tracked_bounty_mark` (the same crosshair in red) with the bounty tint. The list jumps back to page 1 whenever the pinned pair changes (`refresh()` compares
  it with the last sort), not on ordinary progress refreshes. The Tracking & Bounties tab shows
  "Bounty: 2 of 5 slain" for the active bounty's creature.
- **Block**: model `block/bounty_board` is two authored elements, a 2×8 post and a 16×8 board on
  top of it, textured with four PNGs, each sized so the block atlas keeps its full mipmap level (a 24×12 or
  2×14 texture cuts mipmapping for **every** block in the game, and the log warns "limits mip level"):
  - `block/bounty_board.png` (front, north face of the unrotated model, facing the placing player):
    a 32×16 canvas with the 24×12 art in the top-left corner and the rest transparent; the face uses
    uv `[0, 0, 12, 12]` (2 texels per u unit on a 32-wide texture). Repaint only that corner.
  - `block/bounty_board_back.png` (back, south face): the user's hand-painted 24×12 file, mapped
    whole with uv `[0, 0, 16, 16]`. **Don't edit or pad it**; it is the user's art. Its size limits
    the block atlas mip level to 2 (a logged warning), which was accepted. If a face's uv and its
    PNG's size disagree, the face shows a stretched crop: check both when a face looks off.
  - `block/bounty_board_edge.png` (16×16, frame wood): the board's sides, top and bottom, and the
    break particles.
  - `block/bounty_board_post.png`: a 16×16 canvas with the 2×14 post art top-left, uv `[0, 0, 2, 14]`.
  The item texture copies `item/oak_sign`. It is placed
  on the ground facing the player (four facings). Like vanilla's standing sign it is drawn at
  sign scale so the whole thing fits in one block: post 2×8, board 16×8 on top (y 8–16). The
  selection shape is post ∪ board, and **only a click on the board opens the menu**
  (`use` checks the hit's y against the board bottom and passes otherwise); a block's
  shape can't extend into the block above, which is why the sign had to fit in one. Drops itself; crafted shaped from three paper across
  the top, any sign (`#minecraft:signs`) in the middle and a stick below; Functional Blocks tab.

## Roadmap

### Later

- Bounty rewards from loot tables; empowered bounty targets.

## Forge 1.20.1 port

What this branch does differently from `main` (NeoForge 1.21.1). Keep both in step feature-wise;
port fixes across by hand.

- **Build**: ForgeGradle `[6.0,6.2)` on Gradle 8.8, Java 17 toolchain, `official` mappings,
  `org.spongepowered.mixin` for the refmap (`beasts_and_bounties.refmap.json`, `MixinConfigs` in the
  jar manifest), `mod-publish-plugin` publishing to CurseForge with `modLoaders=forge` and the
  publish tasks depending on `reobfJar`. `runGameTestServer` is the server-dist check; the dev client
  needs a display (`runClient` fails headless with "Can't find a primary monitor").
- **No attachments.** `core/PlayerData<T>` stores a codec'd value under
  `beasts_and_bounties:<name>` in the player's `PlayerPersisted` NBT (so Forge copies it on death
  and End exit) and caches the decoded value per player, keyed by the identity of the persisted
  compound: when `Entity.load` or `restoreFrom` replaces that tag the cache misses and re-reads, so a
  stale cache can never survive a reload. `getOrCache` hands out a mutable default without writing
  NBT; `BestiaryProgress.persist` writes the map back after in-place edits. Combat logs of
  non-player entities are decode-per-hit in `entity.getPersistentData()` (small, hit-driven).
- **Networking**: one `SimpleChannel` (`core/Network`), packets are records with
  `encode(FriendlyByteBuf)` / `static decode`. Client handlers are registered as **lambdas, never
  method references** (`payload -> ClientX.handle(payload)`): a method reference links the handle at
  registration and classloads the client class, which crashed the dedicated server at mod
  construction (`Attempted to load class ... Screen for invalid dist DEDICATED_SERVER`). The
  `DistExecutor.unsafeRunWhenOn` double lambda in `Network.toClient` keeps the call itself client-only.
- **Events**: `@Mod.EventBusSubscriber` with `bus = MOD` spelled out for registration events
  (`TrackingClientSetup`, `DevFastMilestonesPack`); every handler is `public static` (Forge's bus
  skips package-private ones silently). `LivingHurtEvent` replaces `LivingIncomingDamageEvent` (both
  pre-armour); combat tracking records `LivingDamageEvent` (post-armour, the real damage).
  `TickEvent.PlayerTickEvent` / `ClientTickEvent` with a `Phase.END` check.
- **Vanilla deltas**: `TextCodec` for `Component`s in codecs; `Codec.dispatch` takes `Codec`s, so the
  reward `Type.codec()` methods return `MapCodec.codec()`; `Milestone.CODEC` validates with
  `flatXmap` (no `Codec.validate`); `Codec.listOf()` has no bounds (an empty milestone list is
  caught at merge time anyway); `AttributeModifier` is UUID-keyed (`BestiaryAttributes` derives one
  UUID per operation); `Operation` names are mapped by `Reward.OPERATION_CODEC` so datapacks keep
  the 1.21 spellings (`add_value`…); `Attribute` is used raw, not as a `Holder`; `SavedData` has no
  factory/registry parameters; loot uses `KILLER_ENTITY` / `DIRECT_KILLER_ENTITY` and
  `server.getLootData()`; advancements use `Advancement` + `getAdvancement`; the block has no codec and
  overrides `use`; `WeightedEntry.Wrapper.getData()`; `DataResult.resultOrPartial` replaces
  `ifError`/`ifSuccess`. Java 17: no pattern switches, `List.get(0)` / `get(size - 1)`, hand-rolled
  `ceilDiv` and clamps.
- **Attributes that don't exist on 1.20.1** were remapped in the vanilla pack (the themed reward
  stays positive and small): explosion knockback resistance → knockback resistance; jump strength,
  movement efficiency → movement speed; safe fall distance → +0.5 max health; oxygen bonus, water
  movement efficiency, submerged mining speed → `forge:swim_speed`; step height →
  `forge:step_height_addition`; block interaction range → `forge:block_reach`; mining efficiency →
  attack speed. The maxed-bestiary total is therefore +11.5 max health here (with the wither's +6).
- **GUI**: 1.20.1 has no GUI sprite atlas, so `bestiary/client/Sprite` draws each PNG by its own
  path and size (`textures/gui/sprites/bestiary/<name>.png`), with `drawNineSliced` for the tooltip
  and track button (the `.mcmeta` nine-slice files are ignored on 1.20.1) and `drawRegion` for the
  poster fade. Never use the short `GuiGraphics.blit(texture, x, y, u, v, w, h)`: it assumes a
  256×256 texture. `EntityPreview` replaces `renderEntityInInventory` (whose 1.20.1 form has no
  centring vector) for both screens; `SepiaBufferSource` wraps the 1.20.1 `VertexConsumer`
  (`vertex/color/uv/overlayCoords/uv2/normal/endVertex`). `Screen.renderBackground(GuiGraphics)` is
  called from `render`. `EditBox` has no `setTextShadow` here, so the search text keeps vanilla's
  shadow. The HUD is a `RegisterGuiOverlaysEvent` overlay (`IGuiOverlay`), the particle a
  `registerSpriteSet`.
- **Datapack**: `pack_format` 15; folders `advancements/`, `recipes/`, `loot_tables/`,
  `structures/`; advancement icons use `"item"`, recipe results `"item"`. Breeze and bogged are
  gone (52 entries, 36 bounties). The built-in packs are registered through
  `Pack.readMetaAndCreate` + `PathPackResources` on the mod file's `findResource` path and are
  auto-enabled on new worlds (packs not explicitly disabled are added in **id order**).
- **PMMO**: 1.20.1's `IDataStorage` has `setXpDiff(uuid, skill, long)` instead of `addXp`.

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
