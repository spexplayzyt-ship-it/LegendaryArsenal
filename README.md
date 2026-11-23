# LegendaryArsenal — Paper 26.3

A Paper plugin project for five custom weapons and the Sekue four-piece armour set.

## Requirements
- Paper 26.3
- Java 25 (required by the 26.x Paper toolchain)
- Gradle 9.x or an IDE with Gradle support

## Build
From this folder, run:

```bash
gradle build
```

The plugin JAR will be created at `build/libs/LegendaryArsenal-1.0.0.jar`.
Put that JAR in your server's `plugins/` folder and restart the server.

## Commands
- `/weapons give <player> nightspear`
- `/weapons give <player> hazremace`
- `/weapons give <player> uryusword`
- `/weapons give <player> benrobow`
- `/weapons give <player> sekuefull`
- Individual armour: `sekuehelmet`, `sekuechestplate`, `sekueleggings`, `sekueboots`

Permission: `legendaryarsenal.admin` (OP by default).

## Abilities
- **Nightspear:** sneak to activate 8 seconds of flight (30-second cooldown); right-click to lunge; left-click swing performs a charged strike.
- **Hazre Mace:** sneak + right-click teleports up to 16 blocks; sneak + left-click damages and knocks back mobs/players in a 50-block radius (60-second cooldown); falling smash gets +50% damage.
- **Uryu Sword:** +70% melee damage and freezes hit targets for 5 seconds; creates temporary frosted-ice footing over water and temporary glass footing over fire/lava while held.
- **Benro Bow:** right-click fires a rapid, high-velocity arrow (quick-shot interval 350 ms), +50% arrow damage, lightning effect on hit, and 2x movement speed while held. The quick-shot uses arrows from inventory unless Creative or Infinity.
- **Sekue Full Armour:** 50% incoming damage reduction, no fall damage, and Haste II, Strength I, Fire Resistance I and Absorption II while the full set is worn.

## Resource pack
Upload `LegendaryArsenal-ResourcePack.zip` to your host's resource-pack setting and have players accept it. It includes custom item meshes and pixel textures. Worn Sekue armour uses vanilla netherite armour-layer visuals in this initial version; its inventory item icons are custom.

## Important notes
- This is a source project, not a precompiled JAR. The build environment used to prepare it has Java 21 and no Gradle/Maven or network access, so I could not compile or runtime-test the JAR here. Build it with Java 25 and internet access so Gradle can resolve the Paper API.
- Paper 26.3 API dependency is pinned to `26.3.build.255-stable`.
- The resource pack targets Java 26.3 resource-pack format 97.1 using `min_format` / `max_format`.
- The 50-block blast is an entity-only blast: it does not break terrain.
