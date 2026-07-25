# Version branches

DupeClient uses **one branch per Minecraft version**. Check out the branch you target, then run `./gradlew build` — no `-PmcTarget` switching.

| Branch | Minecraft | Java | Status |
|--------|-----------|------|--------|
| **`main`** / **`1.21.11`** | 1.21.11 | 21 | ✅ Production |
| **`1.21.10`** | 1.21.10 | 21 | ✅ Production |
| **`26.1`** | 26.1.x | 25 | ✅ Production |
| **`26.2`** | 26.2 | 25 | ✅ Production |

## Quick start

```bash
git checkout 1.21.11   # or 1.21.10, 26.1, 26.2
./gradlew build
```

Output: `build/libs/dupeclient-<version>+<suffix>.jar`

## How it works

- **`gradle.properties`** on each branch pins MC, Fabric API, loader, and `version_suffix`.
- **1.21.x** uses Yarn mappings and `mc_sources_variant` (`1210` vs `default`) for small API-specific source trees.
- **26.x** uses Mojang official names and unobfuscated Loom (`net.fabricmc.fabric-loom`); no Yarn, no variant trees beyond `*-default/`.
- **CI** (`.github/workflows/build.yml`) builds the checked-out branch.
- **Releases**: tag `v*` triggers `.github/workflows/release.yml`, which builds all version branches and attaches every jar.

## `26.x` branches

Both **26.1** and **26.2** share the same Mojang-mapped source layout. Mechanical API updates between snapshots are applied with `scripts/migrate-261-api.ps1` / `scripts/migrate-262-api.ps1` where needed.

**26.2-specific notes:**

- Screen access moved to `Minecraft.gui` (`gui.screen()`, `gui.setScreen(...)`).
- World rendering uses `LevelRenderEvents.COLLECT_SUBMITS` and `DrawableGizmoPrimitives` instead of `MultiBufferSource`.
- Fabric API **0.155.2+26.2**, Loader **0.19.3**, Loom **1.17.14**.

## Merging fixes across versions

1. Land the fix on **`1.21.11`** (`main`).
2. Cherry-pick or merge into **`1.21.10`** (resolve API diffs in `*-1210/` trees if needed).
3. **`26.1`** / **`26.2`**: cherry-pick or port separately; run the matching migrate script if compile fails on renamed Mojang APIs.
