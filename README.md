# UniFIRE Project

The UniProt Functional annotation Inference Rule Engine (UniFIRE) is a rule execution platform that processes rules, written in the UniProt Rule Markup Language (URML), to generate automatic annotations for protein sequences by applying UniProt annotation rules (UniRule and ARBA).

The **aim** of UniFIRE is to enable users to apply UniProt's standard annotation processes to their own private protein 
sequences, making large-scale, private annotation possible on the user's side.

This project is a work in progress, open for collaboration.

Introducing presentation: [UniFIRE-URML.pptx](misc/media/UniFIRE-URML.pptx)

## Quick start

**Prerequisites**

- [Nextflow](https://www.nextflow.io/) (at least 26.04)
- A container engine: **Docker** (default), **Singularity** or **Podman**
- A machine with 24 GB or more of RAM, with internet access

**Input**

UniFIRE accepts either input:

- a **multi-FASTA file** of protein sequences — [InterProScan 6](https://github.com/ebi-pf-team/interproscan6) is run automatically, or
- a **precomputed InterProScan XML file** (classic or InterProScan 6 format).

See [Input data preparation](docs/input-data.md) for the required formats.

**Run** (no clone needed — Nextflow fetches the pipeline from GitHub; `-r` selects a release tag):

```bash
nextflow run ebi-uniprot/unifire -r v<version> \
  --input my_proteins.fasta \
  --output out \
  --dataPath data
```

Replace `my_proteins.fasta` with a precomputed InterProScan XML file to skip the InterProScan step.

Prediction files (`predictions_unirule.out`, `predictions_arba.out`,
`predictions_unirule-pirsr.out`) are written to the `--output` directory.
See [Running UniFIRE with the Nextflow pipeline](docs/nextflow.md) for all parameters.

## Running UniFIRE

There are three primary ways to run UniFIRE:

1. **Running the UniFIRE *Nextflow* pipeline** (recommended)<br/>
The Nextflow workflow automates data download, optional InterProScan 6 execution, taxonomy lineage generation and
   rule inference in a containerised, reproducible pipeline. It requires Nextflow and a container engine
   (Docker, Singularity or Podman). <br/>
This is the preferred way to run UniFIRE.

2. **Downloading and running the UniFIRE *Docker* image** (legacy)<br/>
The UniFIRE pre-built Docker image allows you to run the entire UniFIRE workflow, including all dependencies like
   InterProScan and HMMER, with a single command.  The only necessary software dependency is an installation of
   Docker. <br/>
This method is kept for backward compatibility but is no longer the recommended way to run UniFIRE.

3. **Running UniFIRE After Building from the Source Code**<br/>
This way requires more manual interaction from the user. Each step of a UniFIRE workflow must be executed separately
   or combined by a script. Also, some steps require external software like InterProScan or HMMER, which needs to
   be installed by the user separately or accessed through a web interface.
Therefore, we recommend this approach to advanced users who wish to create a particular workflow, e.g. who
   need to run the heavy InterProScan within a separate procedure.

This documentation uses scripts and sample data provided by the UniFIRE repository. Please
 make sure you have checked out a local copy of the UniFIRE repository using the
 command below, which requires Git to be installed on your system:
```
git clone https://github.com/ebi-uniprot/unifire.git
```

### Running from a local clone

Nextflow pipeline (from the repository root):

```bash
nextflow run main.nf --input samples/proteins.fasta --output out --dataPath data
```

Docker image (legacy):

```bash
./docker/bin/run_unifire_docker.sh -i samples/proteins.fasta -o .
```

From the source code (after `./build.sh`):

```bash
./distribution/bin/unifire.sh -r samples/unirule-urml-latest.xml -i samples/input_ipr.xml -t samples/unirule-templates-latest.xml -o output_unirule_annotations.csv
```

## Documentation

| Document | Contents |
|----------|----------|
| [Running UniFIRE with the Nextflow pipeline](docs/nextflow.md) | The recommended way to run UniFIRE: examples, pipeline overview, all parameters, container profiles, input types and output files. |
| [Running UniFIRE with the Docker image](docs/docker.md) | The legacy Docker workflow: wrapper script and direct container usage, Singularity/Podman alternatives, runtime. |
| [Building and running UniFIRE from the source code](docs/build-from-source.md) | Prerequisites, `build.sh`, and the `unifire.sh` / `pirsr.sh` command-line usage. |
| [Input data preparation](docs/input-data.md) | The required FASTA header format and InterProScan XML format, header validation, taxonomy lineage fetching, and how to run InterProScan yourself. |
| [Nextflow architecture](docs/architecture.md) | Architecture of the Nextflow pipeline: params boundary, workflows, configuration. |
| [Developer guide](docs/developer-guide.md) | Fact and rule models, building the software, execution engine, limitations. |
| [Releasing a new version](docs/release.md) | Release and snapshot procedures, version invariant and enforcement. |

## Issues & Suggestions

If you have any questions regarding this software, if you experience any bugs or have suggestions for improvements on
 the software, the models, the helper scripts, the documentation, etc, please contact us through the UniFIRE mailing
 list.

  * **UniFIRE Mailing List** - [unifire@ebi.ac.uk](mailto:unifire@ebi.ac.uk)


## Authors

* **Alexandre Renaux**
* **Chuming Chen**
* **Hermann Zellner**
* **Vishal Joshi**
* **Abdulrahman Hussein**
* **Muhammad Hilmy**

## Contact

* **UniProt Help** - https://www.uniprot.org/contact
