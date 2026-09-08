# Better Skylight

Better Skylight is a NeoForge mod for Minecraft 1.21.1 that makes skylight
respond to how much of the sky surrounding a block is actually visible.

Vanilla Minecraft treats a roof as a complete skylight blocker regardless of
its height. A large floating platform can therefore make the ground hundreds
of blocks below unnaturally dark. Better Skylight combines vanilla's smooth
light propagation with an angular sky-exposure calculation. Nearby cave roofs
remain dark, while distant floating structures have a weaker effect.

## Features

- Geometry-aware skylight based on ceiling height and nearby sky openings.
- Preserves vanilla's smooth lighting around edges, caves, and holes.
- Changes the authoritative raw skylight value used for rendering, F3 light
  readings, and server-side hostile-mob spawning checks.
- Updates cached skylight when vanilla processes a lighting-relevant block
  change.
- Treats unloaded chunks as unknown instead of assuming they contain open sky.
- Includes one in-game switch for direct vanilla-versus-mod comparisons.
- Supports both vanilla rendering and Sodium 0.8.13 on NeoForge.

Better Skylight changes skylight only. Directional sun shadows and other visual
effects belong in rendering mods such as Vibrancy.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.248 or newer in the 21.1 series
- Java 21
- Installation on both the client and server when used in multiplayer

## Configuration

Open Better Skylight from the mod list and toggle **Enabled**. Disabling it
returns the exact vanilla skylight values and rebuilds visible chunks for a
direct comparison.

Advanced values are stored in `config/better_skylight-common.toml`.

## Development

Build with:

```powershell
.\gradlew.bat build
```

The built JAR is written to `build/libs`.
