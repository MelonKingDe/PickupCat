# Pickup Cat

A cat hangs from the top middle of your screen and acts as your item pickup log:

- **Pick something up** -> the item flies to the cat and it eats it.
- **Drop something** -> the cat retches and throws it back up.
- It talks! Speech bubbles with random reactions ("im hungry :(" when you haven't picked anything up for a minute, comments on what it eats, complaints when you make it throw up, and some SkyBlock-flavoured chatter).
- A small fading log under the cat shows `+12x Enchanted Coal` / `-1x Aspect of the End`.

Built for **Hypixel SkyBlock** (items are tracked by their SkyBlock id, so stat/lore updates don't count as pickups), but works on any server and in singleplayer. Client-side only, no mixins.

Requires Minecraft **26.2**, Fabric Loader 0.19.3+, Fabric API, Java 25.

## Config

`.minecraft/config/pickupcat.json` is created on first launch:

| Option | Default | Meaning |
|---|---|---|
| `enabled` | true | master switch |
| `onlyOnHypixel` | false | only show on hypixel.net |
| `scale` | 1 | cat size, 1-4 |
| `offsetX` / `offsetY` | 0 | move the cat (e.g. below a boss bar) |
| `sway` | true | gentle swinging |
| `showLog` / `logLines` / `logSeconds` | true / 5 / 5.0 | the text log |
| `animationSpeed` | 1.0 | animation speed multiplier |
| `speech` | true | speech bubbles on/off |
| `hungryAfterSeconds` | 60 | idle time before "im hungry :(" (repeats, more dramatic each time) |
| `chatterSeconds` | 75 | average time between random chatter lines, 0 = off |
| `eatFromGuis` | true | also eat items moved in from chests/sacks/bazaar |

## Art

`python3 tools/generate_cat.py` regenerates `cat.png` (10-frame sprite sheet, 32x44 each) and the icons. You can also just edit the PNG or override it with a resource pack at `assets/pickupcat/textures/gui/cat.png`.
