Beasts & Bounties does not add any new creature to Minecraft. It gives you many reasons to hunt the ones already out there though. Every kill is written into a Hunter's Bestiary that reveals a creature's secrets and sharpens you against it. Hunt one long enough and you learn to track it. A Bounty Board posts fresh hunts with real rewards. The hundredth zombie you slay should mean more than the first, and now it does.

Everything is data-driven and works with any creature, vanilla or modded. A default vanilla datapack is included, so you can install it and play right away. It is built to be light on the server and should sit comfortably alongside other mods.

**Website:** [https://mods.silvertide.net/beasts-and-bounties](https://mods.silvertide.net/beasts-and-bounties)

**Requires [SilverLib](https://www.curseforge.com/minecraft/mc-mods/silverlib).**

***

## The Bestiary

A book that contains all of the notes you've made from hunting the various entities around the world. Every kill or bounty completed on a creature brings you closer to milestones, which is when you unlock new insights or perks in the bestiary.

A new creature starts as a nameless ??? and, milestone by milestone, gives up its name and picture, its stats, how it fights, its strengths and weaknesses, where it dwells, and more. Every player keeps their own book and progress.

Knowledge is only half the battle. Milestones also grant permanent bonuses: more damage against that creature, less damage taken from it, extra loot rolls and bonus experience when killing it, and possibly permanent attributes. Slay enough creepers and explosions push you around a little less. Finish the spider and you step a little higher. It all stacks, so a seasoned hunter is a tougher one. Milestones can also unlock new ways of finding that creature, allowing you to track it.

 

## Tracking

 

Know a creature well enough and you can sense it. Tracking unlocks through milestones and comes in two kinds, each with three levels.

**Passive Sense** whispers to you now and then when your tracked creature is near: "You sense the Zombie to the left." Higher levels sharpen the direction, add a distance, and at the top send a wisp of light spiralling away from you toward it.

**Active Hunt** is the chase. Hold the hunt key and a compass rises above your hotbar, giving you much more detailed information of where the creature is than the passive sense. At higher levels the creature glows through walls, for your eyes only. Then the trail goes cold, and you must wait a short while before actively tracking again.

## Bounties

 

A Bounty Board posts WANTED posters: slay a number of that creature and claim the reward in items and experience. You hold one bounty at a time. Postings refresh every Minecraft day and are the same for everyone on the server, so friends can take the same hunt together. Completing a bounty also feeds your bestiary with a claimed bounty and bonus kills, and entries can require bounties to reach their later milestones.

***

## Getting Started

### 1\. Craft the Hunter's Bestiary

Leather over a book over a bone, stacked in a column. Right-click to open it. You do not need to carry it for kills to count; the book is where you read what you have learned. It also lives in the Tools & Utilities creative tab.

### 2\. Go hunting

Kill something. Anything with an entry appears in your book after the first kill, with a "New Creature Recorded" toast. Each milestone after that shows "Knowledge Deepened." Everyone who took part in a fight gets credit, including the owner of a pet or summon and anyone who healed the group, so a party hunt moves everyone's book forward as long as you were actively involved.

### 3\. Read the book

The left page lists every creature you have found, with a search bar at the top. Elites and bosses stand out with a colored name and an icon. The right page shows the selected creature across tabs:

*   **Overview**: name, model, type, description, "Slain" and "Bounties completed" counts, and the next milestone's requirement. When there is nothing left it reads "Entry complete. Nothing left to learn."
*   **Knowledge**: the stats, then How It Fights, Strengths, Weaknesses, Where It Dwells, and Hunter's Notes.
*   **Rewards**: what you have earned, in two sections. "Against the Zombie" holds the bonuses that apply only while fighting it. "Permanent Boons" holds the attribute bonuses that are always active.
*   **Tracking & Bounties**: the Track button, your active bounty's progress, and the tracking levels you have learned.

Tabs with nothing to show stay hidden until you unlock something for them.

### 4\. Track a creature

Once an entry has unlocked tracking, press **Track** on its Tracking & Bounties tab. You track one creature at a time, an eye icon marks it in the list, and **Stop Tracking** is in the same place. The book describes each level you have learned in plain words.

| Passive Sense |What you sense                                                    |
| ------------- |----------------------------------------------------------------- |
| <strong>Scent</strong> |Whether it is close and roughly which way.                        |
| <strong>Trail</strong> |A finer direction and about how far.                              |
| <strong>Track</strong> |The same, from further off, plus the wisp that spirals toward it. |

For the hunt, hold the **Hunt Tracked Creature** key (G by default, rebindable) for two seconds. The compass shows a timer counting the hunt down, with an up or down marker when the creature is above or below you. When the hunt ends the timer shows how long until the next one.

| Active Hunt |What you get                                                                                     |
| ----------- |------------------------------------------------------------------------------------------------ |
| <strong>Hunt</strong> |The compass.                                                                                     |
| <strong>Sight</strong> |The compass, and once the nearest creature is close it glows through walls. A shorter cooldown.  |
| <strong>Mark</strong> |The compass reaches further and every creature of that type nearby glows. The shortest cooldown. |

### 5\. Take a bounty

Craft a Bounty Board from three paper across the top, any sign in the middle, and a stick below. Place it anywhere and click the board. Each poster shows the creature, "Slay 5", and the rewards. The poster always names the creature, even if your book still shows ???. Click Accept and the hunt is on.

Every kill reports in on the action bar: "Bounty: 2 of 5 slain." Finish it and the rewards are yours, with a "Bounty Claimed" toast. Then the board goes quiet for a while before it posts again, and the footer tells you how long. Abandoning a bounty is allowed by default, with a longer wait after.

### Advancements

Three advancements mark the big moments: "A Hunter's First Page" when you record your first creature, "Nothing Left to Learn" when you finish an entry, and "Master of Beasts" when every entry is complete. They are trophies only. Nothing is gated behind them.

***

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

| Field          |Meaning                                                                                                 |
| -------------- |------------------------------------------------------------------------------------------------------- |
| <code>classification</code> |<code>normal</code> (default), <code>elite</code>, <code>minor_boss</code>, or <code>major_boss</code>. Changes the name colour and icon in the list. |
| <code>milestones</code> |The list of milestones. At least one.                                                                   |
| <code>replace</code> |<code>true</code> throws away this creature's entry from lower packs so this file is the whole entry. Default <code>false</code>. |

**Milestone fields.** Everything except the requirement is optional. A milestone needs `kills`, `bounties`, or both.

| Field                                 |Meaning                                                                   |
| ------------------------------------- |------------------------------------------------------------------------- |
| <code>kills</code>                    |Kills needed to reach this milestone.                                     |
| <code>bounties</code>                 |Bounties claimed for this creature needed to reach this milestone.        |
| <code>name</code>, <code>model</code> |<code>true</code> reveals the creature's name or 3D model. Until then it shows as ???. |
| <code>type</code>                     |A category shown under the name, like Undead or Arthropod.                |
| <code>description</code>              |The description paragraph.                                                |
| <code>stats</code>                    |A map of stat name to a number or a tier: <code>low</code>, <code>medium</code>, <code>high</code>, <code>extreme</code>. |
| <code>attack_patterns</code>          |A list of lines for How It Fights.                                        |
| <code>strengths</code>, <code>weaknesses</code>, <code>habitat</code>, <code>notes</code> |Lists of lines for their sections.                                        |
| <code>rewards</code>                  |A list of rewards, below.                                                 |

Text fields accept a plain string or a vanilla text component like `{"translate": "my.lang.key"}`.

**Rewards**

| <code>type</code> |Fields                       |Effect                                                                                                                                                                   |
| ----------------- |---------------------------- |------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| <code>damage_bonus</code> |<code>amount</code>          |Damage dealt to this creature × (1 + total). <code>0.05</code> is +5%.                                                                                                   |
| <code>damage_resistance</code> |<code>amount</code>          |Damage taken from this creature × (1 − total). <code>0.1</code> is 10% less.                                                                                             |
| <code>extra_loot</code> |<code>chance</code> (0 to 1) |Chance that a kill rolls the loot table one extra time.                                                                                                                  |
| <code>bonus_experience</code> |<code>amount</code>          |Experience dropped × (1 + total).                                                                                                                                        |
| <code>attribute</code> |<code>attribute</code>, <code>amount</code>, <code>operation</code> |A permanent attribute modifier on the player. Operation is <code>add_value</code>, <code>add_multiplied_base</code>, or <code>add_multiplied_total</code>. Any attribute works, including ones from other mods. |
| <code>passive_tracking</code> |<code>level</code> (1 to 3)  |Unlocks that Passive Sense level.                                                                                                                                        |
| <code>active_tracking</code> |<code>level</code> (1 to 3)  |Unlocks that Active Hunt level.                                                                                                                                          |

A few rules worth knowing:

*   Milestones unlock in order. A milestone that needs a bounty blocks everything after it until the bounty is claimed, however many kills the player has. To gate late progress, give it a kill count too: `{"kills": 25, "bounties": 1}`.
*   Reached milestones stack. Lists append, later `name`, `type`, and `description` values replace earlier ones, and rewards add up.
*   Files from different packs merge, like vanilla tags. Your pack can add a reward at 25 zombie kills without copying the vanilla file. Milestones with the same requirement combine.
*   A bad file is logged and skipped. It never fails the reload.

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

| Field       |Default |Meaning                                                                    |
| ----------- |------- |-------------------------------------------------------------------------- |
| <code>kills</code> |3       |How many the player must slay.                                             |
| <code>weight</code> |10      |How likely it is to be posted, relative to the other bounties.             |
| <code>bonus_kills</code> |0       |Kills added to the creature's bestiary entry when the bounty is completed. |
| <code>rewards</code> |none    |A list of rewards, below.                                                  |
| <code>type</code> |Fields      |Effect                                                                                                                              |
| ---------- |----------- |----------------------------------------------------------------------------------------------------------------------------------- |
| <code>item</code> |<code>item</code>, <code>count</code> |Gives the items. Overflow drops at the player's feet.                                                                               |
| <code>experience</code> |<code>amount</code> |Gives experience points.                                                                                                            |
| <code>pmmo_xp</code> |<code>xp</code> |Gives Project MMO skill experience, written as <code>"skill/amount"</code>, for example <code>"combat/500"</code>. Ignored when Project MMO is not installed. |

Unlike entries, bounty files do not merge. The highest pack's file wins.

### The built-in datapack

The vanilla content is a built-in datapack, enabled by default. If you want to ship your own content from scratch, disable it on the world's datapack screen or with:

```
/datapack disable "mod/beasts_and_bounties:builtin_data_packs/vanilla_bestiary"
```

Only kill and bounty counts are saved on the player. Everything else is computed from the datapack, so you can change your data, run `/reload`, and every player is right with no migration.

***

## Configuration

Bestiary content is tuned in the datapack, not in config. The config files cover the board and tracking.

### `beasts_and_bounties-bounty-server.toml`

| Option                 |Default |Meaning                                                                                          |
| ---------------------- |------- |------------------------------------------------------------------------------------------------ |
| <code>refreshMinecraftDays</code> |1.0     |How often the postings change, in Minecraft days.                                                |
| <code>offersShown</code> |3       |How many postings the board shows at once (1 to 3).                                              |
| <code>claimCooldownMinutes</code> |10      |Real minutes after completing a bounty before the board posts again.                             |
| <code>abandonCooldownMinutes</code> |20      |Real minutes after abandoning a bounty before the board posts again.                             |
| <code>allowAbandon</code> |true    |Whether a player may abandon an accepted bounty.                                                 |
| <code>showLastSeen</code> |false   |Hovering a poster shows where the creature was last seen, even before the player has learned it. |

### `beasts_and_bounties-tracking-server.toml`

Per Passive Sense level:

| Option              |Scent |Trail |Track |Meaning                                            |
| ------------------- |----- |----- |----- |-------------------------------------------------- |
| <code>range</code>  |24    |32    |48    |How far away, in blocks, a creature can be sensed. |
| <code>pingIntervalSeconds</code> |25    |19    |15    |Seconds between pings.                             |

Per Active Hunt level:

| Option          |Hunt |Sight |Mark |Meaning                                                   |
| --------------- |---- |----- |---- |--------------------------------------------------------- |
| <code>durationSeconds</code> |30   |30    |30   |How long a hunt lasts.                                    |
| <code>cooldownSeconds</code> |240  |180   |150  |Seconds after a hunt ends before the next can start.      |
| <code>arrowRange</code> |48   |48    |64   |How far away, in blocks, the compass can find a creature. |

And overall:

| Option                 |Default |Meaning                                                                                         |
| ---------------------- |------- |----------------------------------------------------------------------------------------------- |
| <code>heightHintBlocks</code> |4.0     |Blocks above or below you before a ping or the compass says "above you" or "below you".         |
| <code>distanceRoundingBlocks</code> |10      |Pings round the distance to the nearest multiple of this.                                       |
| <code>glowRange</code> |32      |Within this many blocks, Sight makes the nearest creature glow and Mark makes all of them glow. |
| <code>huntHoldSeconds</code> |2       |How long the hunt key must be held. 0 starts the hunt on press.                                 |
| <code>huntScanIntervalTicks</code> |10      |Ticks between scans during a hunt. Lower is more responsive, higher is cheaper.                 |

### `beasts_and_bounties-tracking-client.toml`

| Option              |Default |Meaning                                                      |
| ------------------- |------- |------------------------------------------------------------ |
| <code>blocksPerTick</code> |0.45    |How fast the wisp travels.                                   |
| <code>maxLengthBlocks</code> |24.0    |The furthest the wisp travels.                               |
| <code>stopShortBlocks</code> |8.0     |The wisp always stops at least this far before the creature. |
| <code>swirlRadius</code> |0.25    |How wide the wisp's spiral is. 0 flies straight.             |
| <code>swirlRadiansPerTick</code> |0.4     |How fast the wisp spirals.                                   |
| <code>pingTextSeconds</code> |4.0     |How long a ping stays readable before it fades.              |

***

## Commands

All `/bestiary` commands need operator level 2.

| Command                                                         |What it does                                                        |
| --------------------------------------------------------------- |------------------------------------------------------------------- |
| <code>/bestiary kills &lt;set|add|remove&gt; &lt;players&gt; &lt;entity&gt; &lt;amount&gt;</code> |Change a player's kills for a creature.                             |
| <code>/bestiary bounties &lt;set|add|remove&gt; &lt;players&gt; &lt;entity&gt; &lt;amount&gt;</code> |Change a player's claimed bounties for a creature.                  |
| <code>/bestiary get &lt;player&gt; &lt;entity&gt;</code>        |Show kills, bounties, and the milestone reached.                    |
| <code>/bestiary reset &lt;players&gt; [entity]</code>           |Clear progress for one creature, or everything.                     |
| <code>/bestiary complete &lt;players&gt; [entity]</code>        |Raise progress to the final milestone for one entry or all of them. |
| <code>/bestiary board reroll</code>                             |Post a new set of bounties for everyone now.                        |
| <code>/bestiary board clear &lt;players&gt;</code>              |Drop the players' active bounty and cooldown.                       |

***

## Mod Compatibility

*   **Project MMO** (optional): bounties can reward skill experience with the `pmmo_xp` reward type. The shipped bounties do not use it. It is there for packs that run PMMO.

## Planned Features

*   Bounty rewards from loot tables.
*   Empowered bounty targets.

## Requirements

*   Minecraft 1.21.1
*   NeoForge
*   [SilverLib](https://www.curseforge.com/minecraft/mc-mods/silverlib)

Install on both the client and the server.