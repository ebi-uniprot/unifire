# Running UniFIRE with the Docker image

> **Note:** The Docker image workflow is only to support backward compatibility with legacy workflows. The [Nextflow pipeline](nextflow.md) is the recommended way to run UniFIRE.

The image accepts either a FASTA file (InterProScan is then run inside the container) or a precomputed
InterProScan XML file. Additional pipeline options (e.g. `--systems`, `--version`) can be passed via the
`UNIFIRE_NXF_ARGS` environment variable.

## Prerequisites

### Hardware

A machine with 24 GB or more is recommended. Please allow enough free disk space for the downloaded rule, taxonomy
and InterProScan data (several tens of GB when running from a FASTA input).

> **Note:** Starting from UniFIRE version 2025.3, minimum memory requirement has increased to about 24 GB, because of the large increase in the number of ARBA rules.

### Operating system support

The Docker image is expected to run on any operating system

### Software

A recent version of Docker is necessary to start the UniFIRE docker image as a new container. It has been tested successfully on Ubuntu 24.04 and Docker version 23.0.6.

## Data preparation

The only input data you need to provide are either:

1. protein sequence data in multi-FASTA format, or
2. a precomputed InterProScan XML file,

for which functional predictions should be created. The required FASTA header format and the xref
requirement for InterProScan XML files are described in [Input data preparation](input-data.md).

## Usage

Running UniFIRE can be done either by using the provided script `run_unifire_docker.sh` or by using the
command line interface of Docker directly. The script is a wrapper around the Docker command line interface and
provides some additional features like automatic cleanup of temporary files.

**Warning:** The first time this command is run, the UniFIRE Docker image will be downloaded from the
docker container registry and extracted on the local machine.

**A) Using the wrapper script:**

```
usage: ./docker/bin/run_unifire_docker.sh -i <INPUT_FILE> -o <OUTPUT_FOLDER> [-t <FILE_TYPE>] [-v <DATA_VERSION>] [-e <IMAGE_VERSION>]
          [-w <WORKING_FOLDER] [-d <DATA_FOLDER>] [-c] [-s docker|singularity|podman]
    -i: Path to input file (Required). Can be either multi-FASTA file (default) or InterProScan xml file (see -t option).
    -t: Input file type. (Optional), DEFAULT: fasta
        Allowed values:
        fasta: multi-FASTA file with headers in UniProt FASTA header format, containing at least OX=<taxid>
        iprscanxml: InterProScan file in xml format. Each protein should have at least one xref element with 'name' attribute containing OX=<taxid>
    -o: Path to output folder. All output files with predictions in TSV format will be available in this
        folder at the end of the procedure. (Required)
    -v: Data version to run, e.g. 2026.4. Selects the bundled UniProt release and InterProScan/PIRSR
        data versions (see nextflow/versions.nf). (Optional), DEFAULT: version defined as defaultKey in
        nextflow/versions.nf. A --version given via UNIFIRE_NXF_ARGS takes precedence over -v.
    -e: Version of the UniFIRE docker image to use, e.g. 3.1.0. Available versions are listed under
        https://github.com/ebi-uniprot/unifire/pkgs/container/unifire%2Fnextflow. (Optional), DEFAULT: latest
    -w: Path to an empty working directory.  If this option is not given, then a temporary folder will be
        created and used to store intermediate files. (Optional)
    -d: Path to a data directory used to cache downloaded data (URML rules, PIRSR data, taxonomy and
        InterProScan data). If set, the directory is mounted into the container and the data persists
        between runs. If not given, data is downloaded into the container and discarded afterwards. (Optional)
    -c: Clean up temporary files. If set, then all temporary files will be cleaned up at the end of the
        procedure. If no working directory is provided through option -w then the temporary files are cleaned
        up by default
    -s: Container software to be used. (Optional), DEFAULT: docker
        Allowed values:
        docker: Use Docker to run UniFIRE Docker image
        singularity: Use Singularity to run UniFIRE Docker image
        podman: Use Podman to run UniFIRE Docker image

    Environment:
        UNIFIRE_NXF_ARGS: additional options passed to the UniFIRE Nextflow pipeline inside the
        container, e.g. UNIFIRE_NXF_ARGS="--systems unirule,arba" (default: all systems).
```

**B) Using the container command directly:**

The image looks for the input files in `/volume` and runs the Nextflow pipeline; additional pipeline
options are passed via the `UNIFIRE_NXF_ARGS` environment variable.
```
Usage (inside the container, e.g. appended to `docker run ... ghcr.io/ebi-uniprot/unifire/nextflow:<version>`):

  The image looks for the following input files in /volume:
  - proteins-ipr.xml: if it exists, it is used as input and the input type is set to InterProScan.
  - proteins.fasta:   if it exists, it is used as input and the input type is set to fasta.

  Pipeline options can be set via the UNIFIRE_NXF_ARGS environment variable, e.g.:
  --systems unirule,arba,pirsr   AA systems to run predictions for (default: all systems).
  --version 2026.4               UniProt/InterProScan release bundle to use.
  --outputFormat TSV|XML         Prediction output format (default: TSV).
  --chunkSize N                  Proteins chunk size (default: 500).

Input and output directories must be mounted at /volume in the container.
```

## Example

### 1) Fasta input file:

This is a simple example, which shows how to use the UniFIRE Docker image to run the whole UniFIRE workflow on some
sample protein data.

```bash
./docker/bin/run_unifire_docker.sh -i samples/proteins.fasta -o .
```
This command will use as input the file samples/proteins.fasta which is in multi-FASTA format with the header in
the format as described above. It will run the whole UniFIRE workflow to predict functional annotations from UniRule
and ARBA rules. The resulting functional predictions will be written into these files in the current working
directory:
```
predictions_unirule.out
predictions_unirule-pirsr.out
predictions_arba.out
```

_Alternatively, to run directly with docker, you can use the following command:_

```bash
docker run --rm --mount type=bind,source=$(pwd)/samples,target=/volume --env UNIFIRE_NXF_ARGS="--systems unirule,arba" ghcr.io/ebi-uniprot/unifire/nextflow:<version>
```

### 2) InterProScan input file:

This is a simple example, which shows how to use the UniFIRE Docker image to run UniFIRE workflow on some
sample interproscan xml data.

```bash
./docker/bin/run_unifire_docker.sh -i samples/input_ipr.xml -t iprscanxml -o .
```
This command will use as input the file samples/input_ipr.xml which is in InterProScan xml format with the **xref element
name attribute** in the format as described above. It will skip the interproscan step (as it is already given as input) and
run the remaining UniFIRE workflow to predict functional annotations from UniRule and ARBA rules. The resulting
functional predictions will be written into these files in the current working directory:
```
predictions_unirule.out
predictions_unirule-pirsr.out
predictions_arba.out
```

_Alternatively, to run directly with docker (using lite version), you can use the following command:_

```bash
docker run --rm --mount type=bind,source=$(pwd)/samples,target=/volume ghcr.io/ebi-uniprot/unifire/nextflow:<version>
```

## Runtime

The application of the UniFIRE Docker image on a complete bacterial proteome with ~4,500 proteins requires
a runtime of 6 h on an Intel Core i5-4690 CPU with 4 Cores. 98% of this runtime is necessary for the InterProScan
procedure.
<br/>

## Alternatives to Docker

For various reasons Docker is not a reasonable solution in a multi-user environment like most HPC clusters. Therefore
alternatives like *Singularity* and *Podman* have been tested to run the UniFIRE Docker image.

### Singularity

Instead of Docker, an available Singularity installation can be used to run the UniFIRE docker image. The executable
"singularity" must be available in the PATH environment variable. The UniFIRE Docker image has been tested successfully
with Singularity version 3.6.1.

Because the UniFIRE image is big, you may want to use a folder with enough free disk-space
(~200 GB) available for temporary and cached files:
```
export SINGULARITY_CACHEDIR=/path/to/cache/folder
export SINGULARITY_TMPDIR=/path/to/tmp/folder
export SINGULARITY_LOCALCACHEDIR=/path/to/localcache/folder
```

Run the Docker image with Singularity:
```
./docker/bin/run_unifire_docker.sh -i samples/proteins.fasta -o . -s singularity
```

### Podman

Instead of Docker, an available Podman installation can be used to run the UniFIRE docker image. The executable
"podman" must be available in the PATH environment variable. The UniFIRE Docker image has been tested successfully
with Podman version 2.0.3.

Because the UniFIRE image is big, you may want to use a folder with a larger amount of free disk-space
(~200 GB) available for temporary and cached files:
```
export TMPDIR=/path/to/tmp/folder
```

Run the Docker image with Podman:
```
./docker/bin/run_unifire_docker.sh -i samples/proteins.fasta -o . -s podman
```

For both cases, Singularity and Podman, the resulting output folder will be located in ${run_folder} with the filenames
```
predictions_unirule.out
predictions_unirule-pirsr.out
predictions_arba.out
```
