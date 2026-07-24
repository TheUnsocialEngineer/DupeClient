# Version branches

DupeClient uses **one branch per Minecraft version**. Check out the branch you target, then run `./gradlew build` — no `-PmcTarget` switching.

| Branch | Minecraft | Java | Status |
|--------|-----------|------|--------|
| **`main`** / **`1.21.11`** | 1.21.11 | 21 | ✅ Production |
| **`1.21.10`** | 1.21.10 | 21 | ✅ Production |
| **`26.1`** | 26.1.x | 25 | 🚧 Port in progress |

## Quick start

```bash
git checkout 1.21.11   # or 1.21.10
./gradlew build
```

Output: `build/libs/dupeclient-<version>+<suffix>.jar`

## How it works

- **`gradle.properties`** on each branch pins MC, Fabric API, loader, and `version_suffix`.
- **`mc_sources_variant`** selects version-specific source trees (`1210` vs `default`) for GUI, waypoints, macros, etc.
- **CI** (`.github/workflows/build.yml`) builds the checked-out branch.
- **Releases**: tag `v*` triggers `.github/workflows/release.yml`, which builds all version branches and attaches every jar.

## `26.1` branch

Uses Mojang official names and unobfuscated Loom (`net.fabricmc.fabric-loom`). Sources were migrated with `migrateMappings`; compile is not green yet. See branch `26.1` for ongoing port work.

## Merging fixes across versions

1. Land the fix on **`1.21.11`** (`main`).
2. Cherry-pick or merge into **`1.21.10`** (resolve API diffs in `*-1210/` trees if needed).
3. **`26.1`**: port separately once Mojang-mapped sources compile.
