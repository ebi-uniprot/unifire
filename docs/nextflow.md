# Running UniFIRE with the Nextflow pipeline

UniFIRE provides a Nextflow pipeline (`main.nf`) that automates the full annotation workflow, including data download, optional InterProScan 6 execution, taxonomy lineage generation, and rule inference for UniRule, ARBA and PIRSR. The pipeline is containerised and is the recommended way to run UniFIRE.

All commands below are run from the root of the [UniFIRE repository](https://github.com/ebi-uniprot/unifire). See also [Input data preparation](input-data.md) for the required input formats.

## Prerequisites

### Hardware

A machine with 24 GB or more is recommended. Enough free disk space is needed for the downloaded rule data and for the Nextflow `work/` directory.

### Software

- [Nextflow](https://www.nextflow.io/) (at least 26.04)
- A container engine: **Docker** (default), **Singularity** or **Podman**
- Internet access to download rule files from EBI FTP and PIRSR data files

## Examples

Run the full workflow from a FASTA file using Docker:

```bash
nextflow run main.nf \
  --input samples/proteins.fasta \
  --output out \
  --dataPath data
```

Run only UniRule and ARBA from a precomputed InterProScan 6 XML file:

```bash
nextflow run main.nf \
  --input samples/input_ipr6.xml \
  --output out \
  --dataPath data \
  --systems unirule,arba
```

Run only UniRule and ARBA from a precomputed InterProScan 6 XML file, with a specific version set:

```bash
nextflow run main.nf \
  --input samples/input_ipr6.xml \
  --version 2026.4 \
  --output out \
  --dataPath data \
  --systems unirule,arba
```

Run with Singularity and a custom working directory:

```bash
nextflow run main.nf -profile singularity \
  --input samples/proteins.fasta \
  --output out \
  --dataPath data \
  -work-dir /path/to/workdir
```

## Pipeline overview

The pipeline is composed of the following stages, orchestrated by `main.nf`:

1. **Data fetching** (`nextflow/data.nf`)
   Downloads the selected rule sets and templates into `--dataPath`:
   - `urml/<uniprotRelease>/unirule-urml.xml` and `unirule-templates.xml` from EBI FTP
   - `urml/<uniprotRelease>/arba-urml.xml` from EBI FTP
   - `urml/<uniprotRelease>/unirule.pirsr-urml.xml` from EBI FTP
   - `pirsr/<pirsrRelease>/pirsr_data_<release>.tar.gz` from the Protein Information Resource
   - `taxa/taxa.sqlite`, the NCBI taxonomy database

   Files already present locally are reused; pass `--forceDownloads` to re-download them.

2. **InterProScan 6** (`nextflow/modules/interproscan6/main.nf`)  
   Runs only when the input is a FASTA file. It executes the [ebi-pf-team/interproscan6](https://github.com/ebi-pf-team/interproscan6) Nextflow workflow using the configured container profile (`docker`, `singularity` or `podman`).

3. **Taxonomy lineage** (`nextflow/modules/taxonomy/main.nf`)  
   Enriches the InterProScan XML with full NCBI taxonomy lineages using the bundled `updateIPRScanWithTaxonomicLineage.py` script.

4. **Rule inference** (`nextflow/modules/unifire/main.nf` and `nextflow/modules/pirsr/main.nf`)  
   Runs UniRule, ARBA and PIRSR inference inside the `ghcr.io/ebi-uniprot/unifire/nextflow` container. PIRSR first runs `hmmalign` and then invokes UniFIRE on the generated alignment XML.

## Pipeline parameters

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `--input` | yes | - | Path to the input file (multi-FASTA or InterProScan XML). |
| `--output` | yes | - | Output directory where prediction files are published. |
| `--dataPath` | no | `.unifire/data` | Directory where rule files, PIRSR data and the taxonomy database are downloaded/cached. |
| `--inputType` | no | inferred | Input type: `fasta`, `InterProScan` or `InterProScan6`. Inferred from the file extension and XML root element when omitted. |
| `--systems` | no | `unirule,arba,pirsr` | Comma-separated list of systems to run: `unirule`, `arba`, `pirsr`. |
| `--outputFormat` | no | `TSV` | Prediction output format: `TSV` or `XML`. |
| `--chunkSize` | no | `500` | Number of proteins processed per chunk. |
| `--version` | no | `2026.4` | Predefined version set to use. Determines defaults for `--uniprotRelease`, `--iprVersion` and `--iprscanVersion`. |
| `--uniprotRelease` | no | `2026_04` (from `--version 2026.4`) | UniProt release used to download rule files. |
| `--pirsrRelease` | no | `latest` (from `--version 2026.4`) | PIRSR data release used to download PIRSR data files. |
| `--forceDownloads` | no | `false` | Re-download remote rule and taxonomy data files even if they already exist locally. |
| `--iprscanVersion` | no | `6.0.2.2` (from `--version 2026.4`) | InterProScan 6 version to run when the input is FASTA. |
| `--iprVersion` | no | `110.0` (from `--version 2026.4`) | InterPro version used with InterProScan 6. |
| `--iprscanApplications` | no | `HAMAP,PROSITE-profiles,PROSITE-patterns,Pfam,NCBIFAM,SMART,PRINTS,SFLD,CDD,CATH-Gene3D,PIRSF,PANTHER,SUPERFAMILY,CATH-FunFam` | Comma-separated list of InterProScan 6 analyses to run when the input is FASTA (see the [analysis catalogue](https://interproscan6.readthedocs.io/stable/analyses/#analysis-catalogue) for valid names). If set to empty, InterProScan 6 runs all its analyses. |
| `--iprscan6ProfileNames` | no | `standard` | List of container/executor profiles propagated to the InterProScan 6 sub-workflow. Set automatically by the selected `-profile` (e.g. `-profile slurm,singularity` propagates `slurm,singularity`); can be extended via CLI. |
| `--unifireImage` | no | `ghcr.io/ebi-uniprot/unifire/nextflow` | Docker image used for UniFIRE rule inference. |
| `--unifireVersion` | no | value of `engine.unifireVersion` in [nextflow/defaults.nf](../nextflow/defaults.nf) | Tag of the UniFIRE Docker image. |
| `--unifireMemory` | no | - | Max heap memory (in MB) for UniFIRE rule inference. |
| `--pirsrMemory` | no | - | Max heap memory (in MB) for PIRSR alignment. |
| `--maxWorkers` | no | - | Maximum number of parallel local workers. |
| `--help` | no | `false` | Print usage and exit. |

## Container profiles

The pipeline supports three container engines via Nextflow profiles:

- **Docker** (default): `nextflow run main.nf -profile docker ...`
- **Singularity**: `nextflow run main.nf -profile singularity ...`
- **Podman**: `nextflow run main.nf -profile podman ...`

The chosen profiles are also propagated to the InterProScan 6 sub-workflow through `--iprscan6ProfileNames`. Each `-profile` contributes its own child profile name; multiple profiles accumulate, so `-profile slurm,singularity` results in `-profile slurm,singularity` for InterProScan 6 (earlier behavior let only the last profile apply).

## Input types

The pipeline accepts three input types:

- **FASTA** (`fasta`): protein sequences in multi-FASTA format with UniProt-style headers containing at least `OX=<taxid>`. InterProScan 6 is executed automatically.
- **InterProScan XML** (`InterProScan`): output from the classic InterProScan (`<protein-matches>` root element).
- **InterProScan 6 XML** (`InterProScan6`): output from InterProScan 6 (`<results>` root element).

If `--inputType` is omitted, the pipeline infers the type from the file extension (`.fasta`/`.fa` → `fasta`, `.xml` → `InterProScan` or `InterProScan6` based on the root element).

See [Input data preparation](input-data.md) for the required FASTA header format and the xref requirement in InterProScan XML files.

## Output files

The following prediction files are published in the directory specified by `--output`:

```
predictions_unirule.out
predictions_arba.out
predictions_unirule-pirsr.out
```

The format of these files is controlled by `--outputFormat` (`TSV` or `XML`).

## Working directory and cleanup

The pipeline writes intermediate files to a `work/` folder in the launch directory by default. Use the Nextflow `-work-dir` option to change the location:

```bash
nextflow run main.nf --input samples/proteins.fasta --output out --dataPath data -work-dir /path/to/workdir
```

To remove intermediate files after a successful run, use:

```bash
nextflow clean <run_name> -f
```

The run name is printed when the pipeline starts. You can list previous runs with `nextflow log`.
