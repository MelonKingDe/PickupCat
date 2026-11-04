# Pickup Cat

A cat hangs from the top middle of your screen and acts as your item pickup log:

- **Pick something up** -> the item flies to the cat and it eats it.
- **Drop something** -> the cat retches and throws it back up.
- A small fading log under the cat shows `+12x Enchanted Coal` / `-1x Aspect of the End`.

Built for **Hypixel SkyBlock** (items are tracked by their SkyBlock id, so stat/lore updates don't count as pickups), but works on any server and in singleplayer. Client-side only, no mixins.

Requires Minecraft **26.2**, Fabric Loader 0.19.3+, Fabric API, Java 25.

## Build

```bash
# one-time: create the Gradle wrapper (or copy gradlew + gradle/wrapper/* from FabricMC/fabric-example-mod)
gradle wrapper --gradle-version latest

./gradlew build
```

Output: `build/libs/pickupcat-1.0.0+26.2.jar` (upload this one; `-sources.jar` is optional).
Test in-game: `./gradlew runClient`.

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
| `eatFromGuis` | true | also eat items moved in from chests/sacks/bazaar |

## Art

`python3 tools/generate_cat.py` regenerates `cat.png` (10-frame sprite sheet, 32x44 each) and the icons. You can also just edit the PNG or override it with a resource pack at `assets/pickupcat/textures/gui/cat.png`.

## Publish to Modrinth

1. Create the project on modrinth.com (icon: `modrinth/icon.png`, description: `modrinth/description.md`, license MIT, client-side only).
2. Upload `build/libs/pickupcat-1.0.0+26.2.jar` manually, **or** use the included GitHub Action: add the repo variable `MODRINTH_ID` and secret `MODRINTH_TOKEN`, then publish a GitHub release.
