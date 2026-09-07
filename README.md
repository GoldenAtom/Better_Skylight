# Better Skylight

A standalone NeoForge mod for Minecraft 1.21.1 that replaces vanilla skylight propagation with geometry-aware per-block sky exposure.

The project currently contains the NeoForge foundation. The custom skylight engine will be implemented separately from Vibrancy so gameplay light checks and rendered terrain can consume the same sky-light values.

## Development

- Minecraft: 1.21.1
- NeoForge: 21.1.248
- Java: 21
- Mod ID: `better_skylight`

Build with:

```powershell
.\gradlew.bat build
```

The built JAR is written to `build/libs`.
