# UniFIRE Nextflow Architecture

## Overview

UniFIRE is wrapped into a named, embeddable workflow with a strict params
boundary:

- **Entry point** — `main.nf`
- **Parent workflow** — `nextflow/unifire.nf` (`workflow UNIFIRE`)
- **Subworkflow** — `nextflow/data.nf` (`fetchData`)
- **Processes** — `nextflow/modules/**/main.nf`
- **Defaults (code)** — `nextflow/defaults.nf`, `nextflow/versions.nf`
- **Data versions** — `nextflow/versions.json` (bundled copy; remote source of truth on GitHub master)
- **Configuration** — `nextflow.config`, `nextflow/conf/*.config`

## The params boundary

`params` is referenced **only** inside `main.nf` and, when required, the
executor profile configs. No other `.nf` file reads `params`, directly or
indirectly.

```
nextflow.config / conf/*.config   (CLI override knobs + profiles)
        ↓
main.nf  (reads params only, builds grouped option maps)
        ↓
UNIFIRE(run, data, engine, versions)  — all inputs via `take:`
        ↓
fetchData / runIprscan6 / generateTaxonomyLineage / runUnifirePipeline / runPirsrPipeline
```

### What main.nf does

1. Short-circuits `--help` by calling `printUsage(...)` with the default
   option groups.
2. Resolves the data versions with `resolveDataVersions()` (see below) into
   concrete `uniprotRelease`, `pirsrRelease`, `iprscanVersion`, `iprVersion`
   with precedence `explicit CLI argument > --version mapping > default`.
3. Builds four option groups (see below) and invokes `UNIFIRE` positionally.

## UNIFIRE's `take:` contract

`UNIFIRE(run, data, engine)` receives three Groovy maps.
All values are optional; `UNIFIRE` merges them strictly against defaults.

| Group   | Keys |
|---------|------|
| `run`   | `input`, `outputDir`, `inputType`(nullable → inferred), `systems`, `outputFormat`, `chunkSize` |
| `data`  | `dataPath`, `forceDownloads`, `uniprotRelease`, `pirsrRelease`, `iprscanVersion`, `iprVersion`, `iprscanApplications` |
| `engine`| `unifireImage`, `unifireVersion`, `unifireMemory`, `pirsrMemory`, `iprscan6ProfileNames` |

### Defaults live in code

`nextflow/defaults.nf` exposes `getDefaultParams()` — a single nested dict of
the `run` / `data` / `engine` defaults. `nextflow/versions.nf` exposes
`resolveDataVersions(versionKey, dataVersionsPath)`, which resolves the data
versions (decoupled from workflow releases):

1. If `--dataVersions FILE` is given, that file is the exclusive source
   (no remote fetch). Use it for CI, offline runs, and developer overrides.
2. Otherwise, fetch `versions.json` from GitHub **master** and merge the
   bundled `$projectDir/versions.json` **on top of it**: local-only (e.g.
   in-development) versions become available, and local entries win on
   conflicts. Master's `default` key wins for unrequested versions.
3. If the fetch fails, the bundled file alone is used.

The requested version (`--version`, else the tree's `default`) is validated
against the merged tree; unknown versions fail fast with the available list.

Merging (`mergeStrict` in `nextflow/unifire.nf`) is strict: unknown keys fail
fast with the valid key list (exit 1), and `null`-valued user keys fall back
to defaults instead of overriding them. This makes `UNIFIRE` safe to embed in
other workflows:

```groovy
include { UNIFIRE } from './nextflow/unifire.nf'

workflow {
    UNIFIRE(
        [ input: file('samples/proteins.fasta'), outputDir: file('out'),
          systems: 'unirule' ],
        [ dataPath: file('data'), uniprotRelease: '2026_04', pirsrRelease: 'latest',
          iprscanVersion: '6.0.2', iprVersion: '110.0' ],
        [:]   // engine defaults
    )
}
```

> Note: Nextflow's module parser forbids bare top-level constant bindings
> ("Statements cannot be mixed with script declarations"), so shared constants
> are exposed as thin getter functions.

### Why profiles still need params

Executor profiles (e.g. `--docker`, `--test`) can only set values through
`params` (`params.iprscan6ProfileNames`). `main.nf` threads them into the
`engine` group; profiles never bypass `main.nf`.

## Inside UNIFIRE

Validation (`parseSystems`, `parseInputType`, `parseChunkSize`,
`parseOutputFormat`, `inferInputType`), banner printing, output-directory
creation, input-type inference, and system dispatch all happen here. Helper
functions live in `nextflow/unifire.nf`.

## Data prefetch (`nextflow/data.nf`)

`fetchData` is fully `take:`-driven too. It downloads URML rules, PIRSR data,
and the taxa SQLite DB only if missing (or when `forceDownloads` is set) and
emits the resolved file paths.

## Threading values into processes

Processes never read `params`; container images, memory options, publish
directories, and CLI profiles they need come in as `val` inputs passed down
from `UNIFIRE` (e.g. `container "${unifireImage}:${unifireVersion}"`,
`publishDir { "${outputDir}" }` — note the `{ }` for lazy evaluation,
required because `publishDir` cannot see inputs eagerly).

## Help text

`modules/help/main.nf` exposes `def printUsage(opts)` where `opts` is a map
with keys `run`, `data`, `engine`. It no longer accesses `params` and no
longer enumerates data versions (those are resolved at runtime from
`versions.json`).

## Config files

- `nextflow.config` keeps only CLI override knobs (`input`, `output`,
  `version`, `dataVersions`, memory opts, `iprscan6ProfileNames`,
  `forceDownloads`, `help`).
  All defaults come from `nextflow/defaults.nf` / `versions.nf` in code.
- Deleted `nextflow/conf/defaults.config`, `nextflow/conf/versions.config`.
- `--skipDownloads` was removed (dead option). Use `--forceDownloads` to
  invalidate existing local copies and redownload.
