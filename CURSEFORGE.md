# Beasts & Bounties

Beasts & Bounties adds a Hunter's Bestiary to Minecraft: a book that fills in as you hunt. Every creature has an entry made of milestones. Each kill brings you closer to the next one, and each milestone teaches you something new about the creature and makes you a little better at fighting it.

I wanted monster hunting to feel like it builds toward something. In vanilla, your hundredth zombie kill is the same as your first. With this mod, the player who has hunted a creature for a long time knows how it fights, where it lives, and takes less damage from it. They can also sense it nearby and hunt it down. None of that comes from gear. It comes from experience.

The mod is fully data-driven. Every entry, milestone, and reward lives in a datapack, and the vanilla pack is included so you can install it and play.

**Website:** https://mods.silvertide.net/beasts-and-bounties

**Requires [SilverLib](https://www.curseforge.com/minecraft/mc-mods/silverlib).**

<insert picture of the bestiary open on a fully revealed entry here>

## The Bestiary

Craft the Hunter's Bestiary from a book and a bone. You do not need to carry it for kills to count; the book is just where you read what you have learned.

A creature appears in the book after your first kill, with its name and model hidden. As you reach milestones, the entry fills in:

- Name, type, and a slowly rotating 3D model.
- A description and stats (health, damage, speed, armor).
- How It Fights: its attacks and behaviors.
- Strengths and weaknesses.
- Where It Dwells.
- Hunter's Notes: extra lore and tips.

The left page lists every creature you have discovered, with a search bar. The right page shows the selected entry, split into tabs: Overview, Knowledge, Rewards, and Tracking & Bounties. Each entry shows how many kills are left until the next milestone, so you always know what you are working toward.

<insert picture of the Knowledge tab showing stats and sections here>

Every player on a server has their own bestiary. Kills are credited to everyone who took part in the fight, including pets and summons, so a group hunt moves everyone's book forward.

## Rewards

Milestones grant permanent bonuses. They come in two kinds, shown separately on the Rewards tab.

**Against this creature.** Bonuses that only apply while fighting that creature type:

- Increased damage dealt to it.
- Less damage taken from it.
- A chance to roll its loot table an extra time on each kill.
- Bonus experience from its drops.

**Permanent boons.** A small attribute bonus that is always active, themed to the creature. Zombies give max health. Creepers give explosion knockback resistance. Spiders give step height. Guardians give faster mining underwater. There are 18 different attributes spread across the vanilla entries.

Every attribute from every entry stacks, so a fully completed bestiary adds up to a noticeable hunter. The individual numbers are small on purpose: in the vanilla pack, finishing every entry gives about 8 extra max health in total.

<insert picture of the Rewards tab here>

## Tracking

Once an entry unlocks tracking, a Track button appears on its Tracking & Bounties tab. You track one creature at a time. There are two kinds of tracking, each with three levels earned through milestones.

**Passive tracking** pings you now and then when the creature is nearby. At the first level you get a rough direction: "You sense the Zombie to the left." Later levels narrow the direction, add a distance, and finally send a wisp of light spiraling away from you toward the creature.

<insert picture of the wisp trail here>

**Active tracking** is the hunt. Hold the hunt key (G by default) for a couple of seconds and a compass appears above your hotbar, pointing the way to the nearest tracked creature. Then the trail goes cold and you wait before hunting again. At the second level the nearest creature glows through walls for your eyes only once you get close. At the third level the compass reaches further and every one nearby glows.

<insert picture of the hunt compass with a glowing creature here>

The book describes each level in plain words, so you know what you have unlocked without reading a config file. All of the ranges, timers, and cooldowns are configurable.

## Bounties

The Bounty Board is a standing sign that posts hunts. Everything on it is per player, so two players looking at the same board see their own postings and hold their own bounties.

<insert picture of the bounty board with three posters here>

Each posting is a WANTED poster: the creature's model, how many you need to slay, and the reward in items and experience. With the last-seen option turned on in the config, hovering a poster shows where that creature was last seen. Accept one and it becomes your active bounty. You can only hold one at a time.

Completing a bounty gives you the rewards, counts as a claimed bounty in your bestiary, and adds bonus kills to that creature's entry. Entries can require claimed bounties to reach later milestones, so a pack can make the board part of the progression rather than a side activity. The vanilla pack does not gate anything behind bounties yet.

Postings refresh each Minecraft day. There is a cooldown after completing or abandoning a bounty before the board will post again. Abandoning can be turned off in the config.

<insert picture of an active bounty poster showing progress here>

## Vanilla Content

The included datapack covers 54 vanilla creatures: every hostile mob, the bosses, and the livestock and fish players normally kill. Pets, villagers, golems, and peaceful creatures like axolotls and turtles are left out on purpose, since rewards for killing them would encourage it.

Creatures are classified as normal, elite, minor boss, or major boss. Elites and bosses stand out in the list and reach their milestones faster. A zombie takes 250 kills to complete. The Ender Dragon takes 5.

Bounties are posted for the hostiles and bosses only.

## For Pack Makers

Everything about a creature is in one JSON file at `data/<namespace>/bestiary/<entity>.json`. The file path is the entity id, so `data/minecraft/bestiary/zombie.json` is the zombie. Each file is a list of milestones, and each milestone says what it reveals and what it rewards.

Files from different packs merge like vanilla tags. You can add a reward at 25 zombie kills from your own pack without copying the vanilla file, or set `"replace": true` to overwrite an entry outright. Bad files are logged and skipped, never failing the reload.

The mod works for any living entity, including those from other mods. Add a file for it and it is tracked.

Bounties follow the same pattern at `data/<namespace>/bounty/<entity>.json`.

Only kill and bounty counts are saved on the player. Everything else is computed from the datapack, so changing your data and running `/reload` puts every player right with no migration.

The built-in vanilla pack can be disabled on the world's datapack screen if you want to ship your own content from scratch.

Admins have `/bestiary` commands to set, add, reset, or complete any player's progress, and to reroll or clear bounty postings.

## Configuration

- `beasts_and_bounties-tracking-server.toml`: passive ping ranges and intervals, hunt durations, cooldowns, compass and glow ranges.
- `beasts_and_bounties-tracking-client.toml`: how the wisp trail looks.
- `beasts_and_bounties-bounty-server.toml`: refresh period, postings shown, claim and abandon cooldowns, whether abandoning is allowed.

Bestiary content itself is tuned in the datapack, not in config.

## Planned Features

- Bounty rewards from loot tables.
- Empowered bounty targets.

## Requirements

- Minecraft 1.21.1
- NeoForge
- SilverLib
