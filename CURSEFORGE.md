# Beasts & Bounties

Beasts & Bounties adds a Hunter's Bestiary to Minecraft: a book that fills in as you hunt. Every creature has an entry made of milestones. Each kill brings you closer to the next one, and each milestone teaches you something about the creature and makes you a little better at fighting it.

I wanted monster hunting to feel like it builds toward something. In vanilla, your hundredth zombie kill is the same as your first. With this mod, a player who has hunted a creature for a long time knows how it fights and where it lives, hits it harder, takes less from it, and can sense it nearby. None of that comes from gear. It comes from experience.

The mod has three parts that feed each other: the **Bestiary**, the **Bounty Board**, and **Tracking**. Everything is data-driven, and the vanilla content is included so you can install it and play.

**Website:** https://mods.silvertide.net/beasts-and-bounties

**Requires [SilverLib](https://www.curseforge.com/minecraft/mc-mods/silverlib).**

<insert picture of the Hunter's Bestiary open on a fully revealed entry here>

---

## The Bestiary

Craft the Hunter's Bestiary from leather, a book, and a bone stacked in a column. You do not need to carry it for kills to count. The book is just where you read what you have learned.

A creature appears in the book after your first kill, listed as ??? with its model hidden. As you reach milestones, the entry fills in:

- Its name, type, and a slowly rotating 3D model.
- A description and stats: Health, Damage, Speed, and Armor.
- **How It Fights**: its attacks and behaviors.
- **Strengths** and **Weaknesses**.
- **Where It Dwells**.
- **Hunter's Notes**: extra lore and tips.

Each milestone you reach shows a toast, "New Creature Recorded" for the first and "Knowledge Deepened" after that.

### Reading the book

The left page lists every creature you have discovered, with a search bar at the top. Dangerous creatures stand out: Elites, Minor Bosses, and Major Bosses get a coloured name and an icon. The right page shows the selected creature, split into tabs:

- **Overview**: name, model, type, description, how many you have slain, how many bounties you have completed, and the next milestone's requirement. Once there is nothing left it reads "Entry complete. Nothing left to learn."
- **Knowledge**: the stats, then How It Fights, Strengths, Weaknesses, Where It Dwells, and Hunter's Notes.
- **Rewards**: every bonus you have earned from this creature, explained below.
- **Tracking & Bounties**: the Track button, your active bounty's progress, and the tracking levels you have learned.

Tabs with nothing to show stay hidden until you unlock something for them.

<insert picture of the Knowledge tab showing stats and sections here>

### Rewards

Milestones grant permanent bonuses. The Rewards tab splits them into two sections.

**Against the creature.** These only apply while fighting that creature type:

- Increased damage.
- Less damage taken.
- A chance of an extra loot roll on each kill.
- Bonus experience from its drops.

**Permanent Boons.** A small attribute bonus that is always active, themed to the creature. Zombies give Max Health. Creepers give Explosion Knockback Resistance. Spiders give Step Height. Guardians give Submerged Mining Speed. There are 18 different attributes spread across the vanilla entries, and they all stack, so a well-travelled hunter ends up noticeably tougher. The individual numbers are small on purpose. Finishing every vanilla entry adds about 8 Max Health in total.

<insert picture of the Rewards tab here>

### Kill credit

Everyone who took part in a fight gets credit for the kill, including the owner of a pet or summon and anyone who healed the group. A party hunt moves everyone's book forward, not just whoever landed the last hit. This is what SilverLib is for.

### Advancements

Three advancements mark the big moments: "A Hunter's First Page" when you record your first creature, "Nothing Left to Learn" when you finish an entry, and "Master of Beasts" when every entry is complete. They are trophies only. Nothing is gated behind them.

### What's included

The built-in datapack covers 54 vanilla creatures: every hostile mob, the bosses, and the livestock and fish players normally kill. Pets, villagers, golems, and peaceful creatures like axolotls and turtles are left out on purpose, since rewarding their deaths would encourage it.

Creatures are classed as normal, Elite, Minor Boss, or Major Boss. The tougher the class, the fewer kills each milestone takes and the bigger the rewards. A zombie takes 250 kills to complete. The Ender Dragon takes 5.

---

## Bounties

The Bounty Board is a standing sign that posts hunts. Craft it from three paper across the top, any sign in the middle, and a stick below, then place it anywhere. The postings are shared by the whole server, so friends can take the same bounty and hunt it together. Progress, the active bounty, and cooldowns are per player.

<insert picture of the bounty board with three WANTED posters here>

### Taking a bounty

Each posting is a WANTED poster showing the creature's model and name, "Slay 5", and the rewards: "+150 XP" and the items as icons. The poster always names the creature, even if your book still shows it as ???. Click Accept and it becomes your active bounty. You can hold one bounty at a time, and the board shows that one poster with "2 of 5 slain" until you finish it.

Every kill of the target updates the action bar: "Bounty: 2 of 5 slain." Completing the bounty gives you the rewards, plays a short sound, and shows a "Bounty Claimed" toast.

<insert picture of an active bounty poster showing "2 of 5 slain" here>

### How bounties feed the bestiary

A completed bounty counts as a claimed bounty in your bestiary and adds bonus kills to that creature's entry. Milestones can require claimed bounties, not just kills, so a pack can make the board part of the progression rather than a side activity. The vanilla pack does not gate anything behind bounties yet, but the hook is there.

### Refresh and cooldowns

Postings change once a Minecraft day by default. After you complete or abandon a bounty, the board goes quiet for a while and the footer tells you when to come back: "More bounties should arrive in 10 minutes." Abandoning can be turned off entirely in the config.

Bounties are posted for the hostiles and bosses only. Rewards are themed to the creature and sized to its class: common mobs pay in iron, Nether mobs and the rarer undead in gold, illagers in emeralds, creepers and endermen in diamonds, and bosses in netherite and diamonds, with experience to match.

---

## Tracking

Tracking lets a seasoned hunter sense and chase the creatures they know best. Once a creature's entry unlocks it, a Track button appears on its Tracking & Bounties tab. You track one creature at a time, and an eye icon marks it in the list.

There are two kinds of tracking, each with three levels earned through milestones. The book describes each level in plain words, so you know what you have learned without reading a config file.

### Passive Sense

Every so often, if the tracked creature is nearby, a short message tells you where it is.

| Level | What you sense |
|---|---|
| **Scent** | Whether it is close and roughly which way: "You sense the Zombie to the left." |
| **Trail** | A finer direction and about how far: "You're tracking the Zombie about 30 blocks away, ahead to the left." |
| **Track** | The same, from further off, and a wisp of light spirals away from you toward it. |

The wisp points the way without marking the creature itself. It stops well short, so the last stretch is still a hunt.

<insert picture of the wisp trail here>

### Active Hunt

Hold the hunt key (G by default, rebindable) for two seconds. A compass appears above your hotbar and points the way to the nearest tracked creature, with an up or down marker when it is above or below you. A timer counts the hunt down. When it ends the trail goes cold and the timer shows how long until you can hunt again.

| Level | What you get |
|---|---|
| **Hunt** | The compass. |
| **Sight** | The compass, and once the nearest creature is close it glows through walls for your eyes only. A shorter cooldown. |
| **Mark** | The compass reaches further and every creature of that type nearby glows. The shortest cooldown. |

<insert picture of the hunt compass with a glowing creature here>

All of the ranges, timers, and cooldowns are configurable. The book never shows numbers for these, so changing the config never makes it wrong.

---

## Setting Up Your Own Content

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
    { "type": "item", "item": "minecraft:emerald", "count": 3 },
    { "type": "experience", "amount": 60 }
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

Only kill and bounty counts are saved on the player. Everything else is computed from the datapack, so you can change your data and run `/reload` and every player is right with no migration.

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
