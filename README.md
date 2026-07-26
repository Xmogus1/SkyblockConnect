# Skyblock Connect

A social/common use Hypixel SkyBlock Fabric mod to share your progress with anyone. Share your progress or use the party finder for virtually anything.

Type `/sbc` to open the menu.

## What it does
![menu1](https://i.imgur.com/iAzs2XQ.png)
![menu2](https://i.imgur.com/eBX3Vw5.png)
![menu3](https://i.imgur.com/TfNUKoH.png)

**Party finder** - `/sbc pf` lists your party with optional requirements, and
lets people join with one click

![party finder](https://i.imgur.com/uSLLCcD.png)

**Recent** - `/sbc recent` keeps the last 50 things that got shared so you can scroll back through them,
with tabs and a search.

![recent](https://i.imgur.com/VRYC2cK.png)

## Commands

| Command | What it does |
|---|---|
| `/sbc` | Open the menu |
| `/sbc pf` | Party finder |
| `/sbc recent` | Recent shares |
| `/sbc status` | Relay connection status |
| `/sbc reconnect` | Drop and redial the relay |
| `/sbc help` | List everything |

## Install

1. Install [Fabric](https://fabricmc.net/use/installer/) for your Minecraft version.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) and
   [Fabric Kotlin Language](https://modrinth.com/mod/fabric-language-kotlin).
3. (Optional) the Hypixel Mod API mod, which the party finder uses to read your party.
4. Drop the jar for your version into `.minecraft/mods` and launch the Fabric profile.

Builds: Minecraft 26.1.2 and 26.2.

## Building

Needs JDK 25.

```
./gradlew jarLegit -Dorg.gradle.java.home=<jdk25>
```

- `SkyblockConnect/` for 26.1.2

## Notes

Still early and being worked on, so there's probably bugs. Ideas are welcome.

## License

[CC0-1.0](LICENSE).
