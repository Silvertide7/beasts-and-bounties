1.0.4
---
- The bounty board now follows the world's day/night cycle: new postings arrive at dawn, so mods that lengthen the day (Better Days) lengthen the wait with it, and sleeping through the night brings a new board.
- The bounty config's `refreshMinecraftDays` is replaced by `refreshDays`, a whole number of dawns between new postings (default 1). A custom value in an existing config resets to 1.
- The board footer now reads "New postings at dawn." instead of a countdown in minutes.
- Bounty cooldowns are gone. Instead, each posting can be claimed once per player per board: a claimed posting stays up marked "Claimed", and the board resets at dawn. Abandoning a bounty no longer locks you out; you can take any open posting, including the one you abandoned, straight away.
- The `claimCooldownMinutes` and `abandonCooldownMinutes` bounty configs are removed.
