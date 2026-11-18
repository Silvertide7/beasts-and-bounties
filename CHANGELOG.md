1.0.6
---
- The bounty board can now be hung on a wall, like a sign.
- The bounty board now needs a solid block under it (or behind it on a wall), and pops off when that block is removed. A board already standing on something that isn't solid (carpet, a pressure plate, a flower pot) pops off and drops the next time the block under it changes.
- New config `perBoardPostings` (off by default): each board rolls its own postings from where it stands, so boards side by side offer different creatures. Claims still count per creature across every board, and you still hold one bounty at a time.
- Accepting a bounty with `perBoardPostings` on requires being within reach of that board.
