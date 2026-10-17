# Beasts & Bounties

Beasts & Bounties does not add a single new creature to Minecraft. It gives you better reasons to hunt the ones already out there. Every kill is written into a Hunter's Bestiary that reveals a creature's secrets and sharpens you against it. Hunt one long enough and you learn to track it. A Bounty Board posts fresh hunts with real rewards. The hundredth zombie you slay should mean more than the first, and now it does.

Everything is data-driven and works with any creature, vanilla or modded. The vanilla content is included, so you can install it and play. It is built to be light on the server and should sit comfortably alongside other mods.

**Website:** https://mods.silvertide.net/beasts-and-bounties

**Requires [SilverLib](https://www.curseforge.com/minecraft/mc-mods/silverlib).**

---

## The Bestiary

<insert small bestiary item texture here>

A book that fills itself in as you hunt. Every creature has an entry made of milestones, and each kill brings you closer to the next one. A new creature starts as a nameless ??? and, milestone by milestone, gives up its name, its 3D model, its stats, how it fights, its strengths and weaknesses, where it dwells, and a hunter's notes on it. Every player keeps their own book.

Knowledge is only half of it. Milestones also grant permanent bonuses: more damage against that creature, less damage taken from it, extra loot rolls, bonus experience, and a small attribute boon themed to the beast. Slay enough creepers and explosions push you around a little less. Finish the spider and you step a little higher. It all stacks, so a seasoned hunter is a tougher one.

<insert picture of the Hunter's Bestiary open on a fully unlocked entry here>

## Tracking

<insert picture of the hunt compass pointing at a glowing creature here>

Know a creature well enough and you can sense it. Tracking unlocks through milestones and comes in two kinds, each with three levels.

**Passive Sense** whispers to you now and then when your tracked creature is near: "You sense the Zombie to the left." Higher levels sharpen the direction, add a distance, and at the top send a wisp of light spiralling away from you toward it.

**Active Hunt** is the chase. Hold the hunt key and a compass rises above your hotbar, pointing the way to the nearest one. At higher levels the creature glows through walls, for your eyes only. Then the trail goes cold, and you wait before hunting again.

## Bounties

<insert picture of a WANTED bounty card here>

A Bounty Board posts WANTED posters: slay so many of a creature and claim the reward in items and experience. You hold one bounty at a time. Postings refresh every Minecraft day and are the same for everyone on the server, so friends can take the same hunt together. Completing a bounty also feeds your bestiary with a claimed bounty and bonus kills, and entries can require bounties to reach their later milestones.

---

## Getting Started

### 1. Craft the Hunter's Bestiary

Leather over a book over a bone, stacked in a column. Right-click to open it. You do not need to carry it for kills to count; the book is where you read what you have learned. It also lives in the Tools & Utilities creative tab.

### 2. Go hunting

Kill something. Anything with an entry appears in your book after the first kill, with a "New Creature Recorded" toast. Each milestone after that shows "Knowledge Deepened." Everyone who took part in a fight gets credit, including the owner of a pet or summon and anyone who healed the group, so a party hunt moves everyone's book forward.

### 3. Read the book

The left page lists every creature you have found, with a search bar at the top. Elites and bosses stand out with a coloured name and an icon. The right page shows the selected creature across tabs:

- **Overview**: name, model, type, description, "Slain" and "Bounties completed" counts, and the next milestone's requirement. When there is nothing left it reads "Entry complete. Nothing left to learn."
- **Knowledge**: the stats, then How It Fights, Strengths, Weaknesses, Where It Dwells, and Hunter's Notes.
- **Rewards**: what you have earned, in two sections. "Against the Zombie" holds the bonuses that apply only while fighting it. "Permanent Boons" holds the attribute bonuses that are always active.
- **Tracking & Bounties**: the Track button, your active bounty's progress, and the tracking levels you have learned.

Tabs with nothing to show stay hidden until you unlock something for them.

### 4. Track a creature

Once an entry has unlocked tracking, press **Track** on its Tracking & Bounties tab. You track one creature at a time, an eye icon marks it in the list, and **Stop Tracking** is in the same place. The book describes each level you have learned in plain words.

| Passive Sense | What you sense |
|---|---|
| **Scent** | Whether it is close and roughly which way. |
| **Trail** | A finer direction and about how far. |
| **Track** | The same, from further off, plus the wisp that spirals toward it. |

For the hunt, hold the **Hunt Tracked Creature** key (G by default, rebindable) for two seconds. The compass shows a timer counting the hunt down, with an up or down marker when the creature is above or below you. When the hunt ends the timer shows how long until the next one.

| Active Hunt | What you get |
|---|---|
| **Hunt** | The compass. |
| **Sight** | The compass, and once the nearest creature is close it glows through walls. A shorter cooldown. |
| **Mark** | The compass reaches further and every creature of that type nearby glows. The shortest cooldown. |

### 5. Take a bounty

Craft a Bounty Board from three paper across the top, any sign in the middle, and a stick below. Place it anywhere and click the board. Each poster shows the creature, "Slay 5", and the rewards. The poster always names the creature, even if your book still shows ???. Click Accept and the hunt is on.

Every kill reports in on the action bar: "Bounty: 2 of 5 slain." Finish it and the rewards are yours, with a "Bounty Claimed" toast. Then the board goes quiet for a while before it posts again, and the footer tells you how long. Abandoning a bounty is allowed by default, with a longer wait after.

### Advancements

Three advancements mark the big moments: "A Hunter's First Page" when you record your first creature, "Nothing Left to Learn" when you finish an entry, and "Master of Beasts" when every entry is complete. They are trophies only. Nothing is gated behind them.

---

## What's Included

The built-in datapack covers 54 vanilla creatures: every hostile mob, the bosses, and the livestock and fish players normally kill. Pets, villagers, golems, and peaceful creatures like axolotls and turtles are left out on purpose, since rewarding their deaths would encourage it.

Creatures are classed as normal, Elite, Minor Boss, or Major Boss. The tougher the class, the fewer kills each milestone takes and the bigger the rewards. A zombie takes 250 kills to complete. The Ender Dragon takes 5.

Bounties are included for the hostiles and bosses. Rewards are themed to the creature and sized to its class: common mobs pay in iron, Nether mobs and the rarer undead in gold, illagers in emeralds, creepers and endermen in diamonds, and bosses in netherite and diamonds, with experience to match.

---

## Datapacks

Everything about a creature lives in the datapack. The mod works for any living entity, including those from other mods. Add a file for it and it is tracked.

### Bestiary entries

One file per creature at `data/<namespace>/bestiary/<entity>.json`. The file path is the entity id, so `data/minecraft/bestiary/zombie.json` is `minecraft:zombie` and `data/alexsmobs/bestiary/grizzly_bear.json` is `alexsmobs:grizzly_bear`.

```json
{
  "classification": "normal",
  "milestones": [
    { "kills": 1 },
    { "kills": 3, "name": true, "model": true, "type": "Undead" },
    { "kills": 10,
      "description": "A shambling corpse that never tires.",
      "stats": { "health": "medium", "damage": "medium", "speed": "low" },
      "attack_patterns": ["Melee"],
      "habitat": ["Overworld, at night or in darkness"] },
    { "kills": 25,
      "weaknesses": ["Sunlight", "Smite"],
      "strengths": ["Calls nearby zombies for help"],
      "rewards": [ { "type": "damage_bonus", "amount": 0.05 } ] },
    { "kills": 50,
      "notes": ["Villagers it kills may rise as zombie villagers"],
      "rewards": [
        { "type": "damage_resistance", "amount": 0.1 },
        { "type": "extra_loot", "chance": 0.25 },
        { "type": "passive_tracking", "level": 1 },
        { "type": "attribute", "attribute": "minecraft:generic.max_health",
          "amount": 1, "operation": "add_value" }
      ] }
  ]
}
```

**Top level**

| Field | Meaning |
|---|---|
| `classification` | `normal` (default), `elite`, `minor_boss`, or `major_boss`. Changes the name colour and icon in the list. |
| `milestones` | The list of milestones. At least one. |
| `replace` | `true` throws away this creature's entry from lower packs so this file is the whole entry. Default `false`. |

**Milestone fields.** Everything except the requirement is optional. A milestone needs `kills`, `bounties`, or both.

| Field | Meaning |
|---|---|
| `kills` | Kills needed to reach this milestone. |
| `bounties` | Bounties claimed for this creature needed to reach this milestone. |
| `name`, `model` | `true` reveals the creature's name or 3D model. Until then it shows as ???. |
| `type` | A category shown under the name, like Undead or Arthropod. |
| `description` | The description paragraph. |
| `stats` | A map of stat name to a number or a tier: `low`, `medium`, `high`, `extreme`. |
| `attack_patterns` | A list of lines for How It Fights. |
| `strengths`, `weaknesses`, `habitat`, `notes` | Lists of lines for their sections. |
| `rewards` | A list of rewards, below. |

Text fields accept a plain string or a vanilla text component like `{"translate": "my.lang.key"}`.

**Rewards**

| `type` | Fields | Effect |
|---|---|---|
| `damage_bonus` | `amount` | Damage dealt to this creature × (1 + total). `0.05` is +5%. |
| `damage_resistance` | `amount` | Damage taken from this creature × (1 − total). `0.1` is 10% less. |
| `extra_loot` | `chance` (0 to 1) | Chance that a kill rolls the loot table one extra time. |
| `bonus_experience` | `amount` | Experience dropped × (1 + total). |
| `attribute` | `attribute`, `amount`, `operation` | A permanent attribute modifier on the player. Operation is `add_value`, `add_multiplied_base`, or `add_multiplied_total`. Any attribute works, including ones from other mods. |
| `passive_tracking` | `level` (1 to 3) | Unlocks that Passive Sense level. |
| `active_tracking` | `level` (1 to 3) | Unlocks that Active Hunt level. |

A few rules worth knowing:

- Milestones unlock in order. A milestone that needs a bounty blocks everything after it until the bounty is claimed, however many kills the player has. To gate late progress, give it a kill count too: `{"kills": 25, "bounties": 1}`.
- Reached milestones stack. Lists append, later `name`, `type`, and `description` values replace earlier ones, and rewards add up.
- Files from different packs merge, like vanilla tags. Your pack can add a reward at 25 zombie kills without copying the vanilla file. Milestones with the same requirement combine.
- A bad file is logged and skipped. It never fails the reload.

### Bounties

One file per creature at `data/<namespace>/bounty/<entity>.json`, named the same way. A bounty is only ever posted for a creature that has a bestiary entry.

```json
{
  "kills": 5,
  "weight": 10,
  "bonus_kills": 4,
  "rewards": [
    { "type": "item", "item": "minecraft:iron_ingot", "count": 15 },
    { "type": "experience", "amount": 150 }
  ]
}
```

| Field | Default | Meaning |
|---|---|---|
| `kills` | 3 | How many the player must slay. |
| `weight` | 10 | How likely it is to be posted, relative to the other bounties. |
| `bonus_kills` | 0 | Kills added to the creature's bestiary entry when the bounty is completed. |
| `rewards` | none | A list of rewards, below. |

| `type` | Fields | Effect |
|---|---|---|
| `item` | `item`, `count` | Gives the items. Overflow drops at the player's feet. |
| `experience` | `amount` | Gives experience points. |
| `pmmo_xp` | `xp` | Gives Project MMO skill experience, written as `"skill/amount"`, for example `"combat/500"`. Ignored when Project MMO is not installed. |

Unlike entries, bounty files do not merge. The highest pack's file wins.

### The built-in datapack

The vanilla content is a built-in datapack, enabled by default. If you want to ship your own content from scratch, disable it on the world's datapack screen or with:

```
/datapack disable "mod/beasts_and_bounties:builtin_data_packs/vanilla_bestiary"
```

Only kill and bounty counts are saved on the player. Everything else is computed from the datapack, so you can change your data, run `/reload`, and every player is right with no migration.

---

## Configuration

Bestiary content is tuned in the datapack, not in config. The config files cover the board and tracking.

### `beasts_and_bounties-bounty-server.toml`

| Option | Default | Meaning |
|---|---|---|
| `refreshMinecraftDays` | 1.0 | How often the postings change, in Minecraft days. |
| `offersShown` | 3 | How many postings the board shows at once (1 to 3). |
| `claimCooldownMinutes` | 10 | Real minutes after completing a bounty before the board posts again. |
| `abandonCooldownMinutes` | 20 | Real minutes after abandoning a bounty before the board posts again. |
| `allowAbandon` | true | Whether a player may abandon an accepted bounty. |
| `showLastSeen` | false | Hovering a poster shows where the creature was last seen, even before the player has learned it. |

### `beasts_and_bounties-tracking-server.toml`

Per Passive Sense level:

| Option | Scent | Trail | Track | Meaning |
|---|---|---|---|---|
| `range` | 24 | 32 | 48 | How far away, in blocks, a creature can be sensed. |
| `pingIntervalSeconds` | 25 | 19 | 15 | Seconds between pings. |

Per Active Hunt level:

| Option | Hunt | Sight | Mark | Meaning |
|---|---|---|---|---|
| `durationSeconds` | 30 | 30 | 30 | How long a hunt lasts. |
| `cooldownSeconds` | 240 | 180 | 150 | Seconds after a hunt ends before the next can start. |
| `arrowRange` | 48 | 48 | 64 | How far away, in blocks, the compass can find a creature. |

And overall:

| Option | Default | Meaning |
|---|---|---|
| `heightHintBlocks` | 4.0 | Blocks above or below you before a ping or the compass says "above you" or "below you". |
| `distanceRoundingBlocks` | 10 | Pings round the distance to the nearest multiple of this. |
| `glowRange` | 32 | Within this many blocks, Sight makes the nearest creature glow and Mark makes all of them glow. |
| `huntHoldSeconds` | 2 | How long the hunt key must be held. 0 starts the hunt on press. |
| `huntScanIntervalTicks` | 10 | Ticks between scans during a hunt. Lower is more responsive, higher is cheaper. |

### `beasts_and_bounties-tracking-client.toml`

| Option | Default | Meaning |
|---|---|---|
| `blocksPerTick` | 0.45 | How fast the wisp travels. |
| `maxLengthBlocks` | 24.0 | The furthest the wisp travels. |
| `stopShortBlocks` | 8.0 | The wisp always stops at least this far before the creature. |
| `swirlRadius` | 0.25 | How wide the wisp's spiral is. 0 flies straight. |
| `swirlRadiansPerTick` | 0.4 | How fast the wisp spirals. |
| `pingTextSeconds` | 4.0 | How long a ping stays readable before it fades. |

---

## Commands

All `/bestiary` commands need operator level 2.

| Command | What it does |
|---|---|
| `/bestiary kills <set\|add\|remove> <players> <entity> <amount>` | Change a player's kills for a creature. |
| `/bestiary bounties <set\|add\|remove> <players> <entity> <amount>` | Change a player's claimed bounties for a creature. |
| `/bestiary get <player> <entity>` | Show kills, bounties, and the milestone reached. |
| `/bestiary reset <players> [entity]` | Clear progress for one creature, or everything. |
| `/bestiary complete <players> [entity]` | Raise progress to the final milestone for one entry or all of them. |
| `/bestiary board reroll` | Post a new set of bounties for everyone now. |
| `/bestiary board clear <players>` | Drop the players' active bounty and cooldown. |

---

## Mod Compatibility

- **Project MMO** (optional): bounties can reward skill experience with the `pmmo_xp` reward type. The shipped bounties do not use it. It is there for packs that run PMMO.

## Planned Features

- Bounty rewards from loot tables.
- Empowered bounty targets.

## Requirements

- Minecraft 1.21.1
- NeoForge
- [SilverLib](https://www.curseforge.com/minecraft/mc-mods/silverlib)

Install on both the client and the server.
