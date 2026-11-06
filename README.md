# Pickup Cat

A cat hangs from the top middle of your screen and acts as your item pickup log:

- **Pick something up** -> the item flies to the cat and it eats it.
- **Drop something** -> the cat retches and throws it back up.
- It talks! Speech bubbles with random reactions ("im hungry :(" when you haven't picked anything up for a minute, comments on what it eats, complaints when you make it throw up, and some SkyBlock-flavoured chatter).
- **Drop a dye** (SkyBlock dyes or vanilla dyes) and the cat lets go, falls to the bottom of the screen, puts on a party hat in the dye's colour and throws a confetti party with it, then climbs back up.
- A small fading log under the cat shows `+12x Enchanted Coal` / `-1x Aspect of the End`.

Built for **Hypixel SkyBlock** (items are tracked by their SkyBlock id, so stat/lore updates don't count as pickups), but works on any server and in singleplayer. Client-side only, no mixins.

Requires Minecraft **26.2**, Fabric Loader 0.19.3+, Fabric API, Java 25.

## Build

```bash
# one-time: create the Gradle wrapper (or copy gradlew + gradle/wrapper/* from FabricMC/fabric-example-mod)
gradle wrapper --gradle-version latest

./gradlew build
```

Output: `build/libs/pickupcat-1.3.0+26.2.jar` (upload this one; `-sources.jar` is optional).
Test in-game: `./gradlew runClient`.

## Resizing

Press **`]`** to make the cat bigger and **`[`** to make it smaller (steps of 0.25, saved automatically). Both keys can be rebound in *Options > Controls > Key Binds > Pickup Cat*. You can also edit `scale` in `config/pickupcat.json` - changes apply live while the game is running.

## Config

`.minecraft/config/pickupcat.json` is created on first launch:

| Option | Default | Meaning |
|---|---|---|
| `enabled` | true | master switch |
| `onlyOnHypixel` | false | only show on hypixel.net |
| `scale` | 1.0 | cat size, 0.5-6 (whole numbers look crispest). Edits to the file apply live within ~2 s |
| `offsetX` / `offsetY` | 0 | move the cat (e.g. below a boss bar) |
| `sway` | true | gentle swinging |
| `showLog` / `logLines` / `logSeconds` | true / 5 / 5.0 | the text log |
| `animationSpeed` | 1.0 | animation speed multiplier |
| `dyeParty` | true | dropping a dye triggers the party |
| `speech` | true | speech bubbles on/off |
| `hungryAfterSeconds` | 60 | idle time before "im hungry :(" (repeats, more dramatic each time) |
| `chatterSeconds` | 75 | average time between random chatter lines, 0 = off |
| `eatFromGuis` | true | also eat items moved in from chests/sacks/bazaar |

## Art

`python3 tools/generate_cat.py` regenerates `cat.png` (10-frame sprite sheet, 32x44 each) and the icons. You can also just edit the PNG or override it with a resource pack at `assets/pickupcat/textures/gui/cat.png`.

## Publish to Modrinth

1. Create the project on modrinth.com (icon: `modrinth/icon.png`, description: `modrinth/description.md`, license MIT, client-side only).
2. Upload `build/libs/pickupcat-1.3.0+26.2.jar` manually, **or** use the included GitHub Action: add the repo variable `MODRINTH_ID` and secret `MODRINTH_TOKEN`, then publish a GitHub release.

## Dye colours

`DyeColors.java` maps SkyBlock dye ids (`DYE_NYANZA`, ...) to confetti colours. They are approximate; unknown or multicolour dyes get rainbow confetti. Edit the table to taste.
