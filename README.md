# Beasts & Bounties

A Forge mod for Minecraft 1.20.1 that adds a **Hunter's Bestiary**: a book that fills in as you
hunt. Every creature has a data-driven entry made of milestones. Reaching one reveals more about the
creature (name, 3D model, stats, how it fights, strengths, weaknesses, where it dwells) and grants
permanent bonuses: more damage against it, less damage taken from it, extra loot rolls, bonus
experience, a small attribute boon, and the ability to track it.

## Playing

- Craft the bestiary from a book and a bone, or find it in the Tools & Utilities creative tab.
- Kill creatures. Everyone who took part in the fight gets credit, pets and summons included.
- Open the book to browse what you've learned. Search by name, page with the arrows, and switch
  between the Overview, Knowledge and Rewards tabs.
- Once tracking is unlocked for a creature, press **Track** in the book and the bestiary will
  sense it nearby. Press **G** (rebindable) to hunt: an arrow points the way, and at higher levels
  the creature glows through walls for you.

## Configuring

- All creature entries live in a built-in datapack, `Beasts & Bounties Vanilla Bestiary`, enabled
  by default. Disable it on the world's datapack screen or with
  `/datapack disable "mod/beasts_and_bounties:builtin_data_packs/vanilla_bestiary"`.
- Add or change entries with your own datapack at `data/<namespace>/bestiary/<entity>.json`. Files
  from several packs merge; set `"replace": true` to overwrite an entry outright. The full format
  is documented in `CLAUDE.md`.
- Tracking distances and times are in `config/beasts_and_bounties-tracking-server.toml`; the
  wisp trail's look is in `config/beasts_and_bounties-tracking-client.toml`.
- Admins have `/bestiary` to inspect, set, complete or reset any player's progress.

## Requirements

- Forge 47+ for Minecraft 1.20.1.

## Building

```bash
./gradlew build
```

Dev runs: `./gradlew runClient`, `./gradlew runServer`, `./gradlew runGameTestServer`.
