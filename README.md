# UniFIRE Project

The UniProt Functional annotation Inference Rule Engine (UniFIRE) is a rule execution platform that processes rules, written in the UniProt Rule Markup Language (URML), to generate automatic annotations for protein sequences by applying UniProt annotation rules (UniRule and ARBA).

The **aim** of UniFIRE is to enable users to apply UniProt's standard annotation processes to their own private protein 
sequences, making large-scale, private annotation possible on the user's side.

This project is a work in progress, open for collaboration.

Introducing presentation: [UniFIRE-URML.pptx](misc/media/UniFIRE-URML.pptx)

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

This documentation uses scripts and sample data provided by the UniFIRE GitLab repository. Please
 make sure you have checked out a local copy of UniFIRE Gitlab repository using the
 command below, which requires Git to be installed on your system:
```
git clone https://gitlab.ebi.ac.uk/uniprot-public/unifire.git
```

## 1. Using the Nextflow pipeline

UniFIRE provides a Nextflow pipeline (`nextflow/main.nf`) that automates the full annotation workflow, including data download, optional InterProScan 6 execution, taxonomy lineage generation, and rule inference for UniRule, ARBA and PIRSR. The pipeline is containerised and is the recommended way to run UniFIRE.

### Prerequisites

#### Hardware

A machine with 24 GB or more is recommended. Enough free disk space is needed for the downloaded rule data and for the Nextflow `work/` directory.

#### Software

- [Nextflow](https://www.nextflow.io/) (recent version, DSL2 compatible)
- A container engine: **Docker** (default), **Singularity** or **Podman**
- Internet access to download rule files from EBI FTP and PIRSR data files

### Examples

Run the full workflow from a FASTA file using Docker:

```bash
nextflow run nextflow/main.nf \
  --input samples/proteins.fasta \
  --output out \
  --dataPath data
```

Run only UniRule and ARBA from a precomputed InterProScan 6 XML file:

```bash
nextflow run nextflow/main.nf \
  --input samples/input_ipr6.xml \
  --output out \
  --dataPath data \
  --systems unirule,arba \
  --skipDownloads
```

Run with Singularity and a custom working directory:

```bash
nextflow run nextflow/main.nf -profile singularity \
  --input samples/proteins.fasta \
  --output out \
  --dataPath data \
  -work-dir /path/to/workdir
```

### Pipeline overview

The pipeline is composed of the following stages, orchestrated by `nextflow/main.nf`:

1. **Data fetching** (`nextflow/data.nf`)
   Downloads the selected rule sets and templates into `--dataPath`:
   - `unirule-urml.xml` and `unirule-templates.xml` from EBI FTP
   - `arba-urml.xml` from EBI FTP
   - `unirule.pirsr-urml.xml` from EBI FTP
   - `pirsr_data_latest.tar.gz` from the Protein Information Resource
   Use `--skipDownloads` to reuse previously downloaded files.

2. **InterProScan 6** (`nextflow/modules/interproscan6/main.nf`)  
   Runs only when the input is a FASTA file. It executes the [ebi-pf-team/interproscan6](https://github.com/ebi-pf-team/interproscan6) Nextflow workflow using the configured container profile (`docker`, `singularity` or `podman`).

3. **Taxonomy lineage** (`nextflow/modules/taxonomy/main.nf`)  
   Enriches the InterProScan XML with full NCBI taxonomy lineages using the bundled `updateIPRScanWithTaxonomicLineage.py` script.

4. **Rule inference** (`nextflow/modules/unifire/main.nf` and `nextflow/modules/pirsr/main.nf`)  
   Runs UniRule, ARBA and PIRSR inference inside the `unifire/nextflow` container. PIRSR first runs `hmmalign` and then invokes UniFIRE on the generated alignment XML.

### Pipeline parameters

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `--input` | yes | - | Path to the input file (multi-FASTA or InterProScan XML). |
| `--output` | yes | - | Output directory where prediction files are published. |
| `--dataPath` | yes | - | Directory where rule files and PIRSR data are downloaded/cached. |
| `--inputType` | no | inferred | Input type: `fasta`, `InterProScan` or `InterProScan6`. Inferred from the file extension and XML root element when omitted. |
| `--systems` | no | `unirule,arba,pirsr` | Comma-separated list of systems to run: `unirule`, `arba`, `pirsr`. |
| `--outputFormat` | no | `TSV` | Prediction output format: `TSV` or `XML`. |
| `--chunkSize` | no | `500` | Number of proteins processed per chunk. |
| `--uniprotRelease` | no | `latest` | UniProt release used to download rule files. |
| `--skipDownloads` | no | `false` | Skip downloading remote rule files (requires valid files already present in `--dataPath`). |
| `--iprscanVersion` | no | `6.0.1` | InterProScan 6 version to run when the input is FASTA. |
| `--iprVersion` | no | `latest` | InterPro version used with InterProScan 6. |
| `--iprscan6ProfileName` | no | `docker` | Container profile used by the InterProScan 6 sub-workflow: `docker`, `singularity` or `podman`. |
| `--unifireMemory` | no | - | Max heap memory (in MB) for UniFIRE rule inference. |
| `--pirsrMemory` | no | - | Max heap memory (in MB) for PIRSR alignment. |
| `--maxWorkers` | no | - | Maximum number of parallel local workers. |
| `--help` | no | `false` | Print usage and exit. |

### Container profiles

The pipeline supports three container engines via Nextflow profiles:

- **Docker** (default): `nextflow run nextflow/main.nf -profile docker ...`
- **Singularity**: `nextflow run nextflow/main.nf -profile singularity ...`
- **Podman**: `nextflow run nextflow/main.nf -profile podman ...`

The chosen profile is also propagated to the InterProScan 6 sub-workflow through `--iprscan6ProfileName`.

### Input types

The pipeline accepts three input types:

- **FASTA** (`fasta`): protein sequences in multi-FASTA format with UniProt-style headers containing at least `OX=<taxid>`. InterProScan 6 is executed automatically.
- **InterProScan XML** (`InterProScan`): output from the classic InterProScan (`<protein-matches>` root element).
- **InterProScan 6 XML** (`InterProScan6`): output from InterProScan 6 (`<results>` root element).

If `--inputType` is omitted, the pipeline infers the type from the file extension (`.fasta`/`.fa` → `fasta`, `.xml` → `InterProScan` or `InterProScan6` based on the root element).

### Output files

The following prediction files are published in the directory specified by `--output`:

```
predictions_unirule.out
predictions_arba.out
predictions_unirule-pirsr.out
```

The format of these files is controlled by `--outputFormat` (`TSV` or `XML`).

### Working directory and cleanup

The pipeline writes intermediate files to a `work/` folder in the launch directory by default. Use the Nextflow `-work-dir` option to change the location:

```bash
nextflow run nextflow/main.nf --input samples/proteins.fasta --output out --dataPath data -work-dir /path/to/workdir
```

To remove intermediate files after a successful run, use:

```bash
nextflow clean <run_name> -f
```

The run name is printed when the pipeline starts. You can list previous runs with `nextflow log`.

***

## 2. Using the Docker image (legacy)

> **Note:** The Docker image workflow is considered legacy. The [Nextflow pipeline](#1-using-the-nextflow-pipeline) is the recommended way to run UniFIRE.

There are two Docker image variants provided to suit different user needs and environments::

**1. Full image**  
This image includes all required data and dependencies, notably the InterProScan tool and its substantial datasets, which account for most of its size.

 - Size: Large (~60 GB)

 - Input: Accepts either a FASTA file (to run InterProScan automatically) or a precomputed InterProScan XML file.

 - Benefit: Provides a complete, "all-in-one" workflow with zero manual dependency setup.

**2. Lite image**  
This image excludes InterProScan, resulting in a significantly smaller size.

 - Size: Much smaller (~4 GB)

 - Input: Accepts only a precomputed InterProScan XML file.

 - Benefit: Recommended for users who already have InterProScan XML inputs or prefer to run InterProScan separately. 
   It saves significant download time and image storage space.

The Lite image is identified by the -lite suffix in its tag (e.g., unifire:<version>-lite).
### Prerequisites

#### Hardware

A machine with 24 GB or more is recommended. Please allow enough free disk space based on the image type being used (full/lite).

> **Note:** Starting from UniFIRE version 2025.3, minimum memory requirement has increased to about 24 GB, because of the large increase >in the number of ARBA rules.

#### Operating system support

The Docker image is expected to run on any operating system 

#### Software

A recent version of Docker is necessary to start the UniFIRE docker image as a new container. It has been tested  successfully on Ubuntu 24.04 and Docker version 23.0.6.

### Data preparation

**1. Fasta input type:**  
The only input data that need to be provided are the protein sequence data in multi-FASTA format for which functional predictions should be created. The FASTA header needs to follow the UniProtKB conventions ([https://www.uniprot.org/help/fasta-headers](https://www.uniprot.org/help/fasta-headers))
 
 The minimal structure of the header is:
```
>{id}|{name} {flags}
```
* `{id}` must be a unique string amongst the processed sequences
* `{name}`:
    * can be any string starting from the previous separator, that should not contain any flag
    * might contains `(Fragment)` if applicable (e.g "`ACN2_ACAGO Acanthoscurrin-2 (Fragment)`")
* `{flags}` \[mandatory]: to be considered a valid header, only the following flags should be provided:
    * OX=`{taxonomy Id}`
* `{flags}` \[optional]: If possible / applicable, you should also provide:
    * OS=`{organism name}`
    * GN=`{recommended gene name}`
    * GL=`{recommended ordered locus name (OLN) or Open Reading Frame (OLN) name}`
    * OG=`{gene location(s), comma-separated if multiple}` ([cf. organelle ontology](https://www.ebi.ac.uk/ena/WebFeat/qualifiers/organelle.html))


**2. InterProScan XML input type:**  
The only input data that need to be provided is the InterProScan XML file for which functional predictions should be created.   
Each protein in the InterProScan XML file should have at least one xref element with 'name' attribute containing OX=<taxid> in the same format as described above for the fasta header, as shown in the following example.
```
<xref id="tr|J0U7L2|J0U7L2_9BURK" name="tr|J0U7L2|J0U7L2_9BURK Cytochrome c553 OS=Acidovorax sp. CF316 OX=1144317 GN=PMI14_03334"/>
```

### Usage
Running UniFIRE can de done either by using the provided script `run_unifire_docker.sh` or by using the
 command line interface of Docker directly. The script is a wrapper around the Docker command line interface and
  provides some additional features like automatic cleanup of temporary files.

**Warning:** The first time this command is run, the UniFIRE Docker image will be downloaded from the
docker container registry and extracted on the local machine. 

**A) Using the wrapper script:**

```
usage: ./docker/bin/run_unifire_docker.sh -i <INPUT_FILE> -o <OUTPUT_FOLDER> [-t <FILE_TYPE>] [-v <VERSION>] [-w <WORKING_FOLDER] [-c]
          [-s docker|singularity|podman]
    -i: Path to input file (Required). Can be either multi-FASTA file (default) or InterProScan xml file (see -t option).
    -t: Input file type. (Optional), DEFAULT: fasta
        Allowed values:
        fasta: multi-FASTA file with headers in UniProt FASTA header format, containing at least OX=<taxid>
        iprscanxml: InterProScan file in xml format. Each protein should have at least one xref element with 'name' attribute containing OX=<taxid>
    -o: Path to output folder. All output files with predictions in TSV format will be available in this
        folder at the end of the procedure. (Required)
    -v: Version of the docker image to use, e.g. 2020.2. Available versions are listed under
        https://gitlab.ebi.ac.uk/uniprot-public/unifire/container_registry. (Optional), DEFAULT: version defined in version.properties
    -w: Path to an empty working directory.  If this option is not given, then a temporary folder will be
        created and used to store intermediate files. (Optional)
    -c: Clean up temporary files. If set, then all temporary files will be cleaned up at the end of the
        procedure. If no working directory is provided through option -w then the temporary files are cleaned
        up by default
    -s: Container software to be used. (Optional), DEFAULT: docker
        Allowed values:
        docker: Use Docker to run UniFIRE Docker image
        singularity: Use Singularity to run UniFIRE Docker image
        podman: Use Podman to run UniFIRE Docker image
```

**B) Using the container command directly:**

Directly run the UniFIRE Docker image entrypoint script, with any provided arguments.
```
Usage: unifire-workflow.sh [options]
Options:
  -i FILE       Input FASTA or InterProScan XML file (required)
  -t TYPE       Input type: options are fasta or iprscanxml (required)
                Default to fasta if input file name ends with '.fasta' or '.fa', and to iprscanxml if it ends with '.xml'.
                When none of -i and -t are provided, the script will look for following file names in the output directory:
                - proteins-ipr.xml: if exists, then it is used as input and type is set to iprscanxml.
                - proteins.fasta:   if exists, then it is used as input and type is set to fasta.
  -s SYSTEM     AA system to run predictions for: options [unirule,arba,pirsr] (default is all systems).
                Multiple systems can be selected using comma to separate them (e.g., -s unirule,arba).
  -n N          Proteins chunk size (default: 500)
  -o DIR        Output directory (required) - default is "/volume"
  -h            Show this help

Images:
  Full image: includes InterProScan. Accepts FASTA (will run InterProScan) or InterProScan XML.
  Lite image: does NOT include InterProScan. Accepts only InterProScan XML as input.

Examples:
  unifire-workflow.sh -i proteins.fasta -t fasta -n 500 -o /volume
  unifire-workflow.sh -i proteins-ipr.xml -t iprscanxml -s unirule,arba -n 1000 -o /volume
  unifire-workflow.sh (without options) - will look for default files in the /volume output directory and run all systems.

Note that input and output directories must be mounted in the container.
```

### Example

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
docker run --rm --mount type=bind,source=$(pwd)/samples,target=/volume dockerhub.ebi.ac.uk/uniprot-public/unifire:<version> -i /volume/proteins.fasta -o /volume
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
docker run --rm --mount type=bind,source=$(pwd)/samples,target=/volume dockerhub.ebi.ac.uk/uniprot-public/unifire:<version>-lite -i /volume/input_ipr.xml -t iprscanxml -o /volume
```

### Runtime
The application of the UniFIRE Docker image on a complete bacterial proteome with ~4,500 proteins requires
 a runtime of 6 h on an Intel Core i5-4690 CPU with 4 Cores. 98% of this runtime is necessary for the InterProScan
 procedure.
<br/>

### Alternatives to Docker
For various reasons Docker is not a reasonable solution in a multi-user environment like most HPC clusters. Therefore
 alternatives like *Singularity* and *Podman* have been tested to run the UniFIRE Docker image. 

#### Singularity
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
    
 
#### Podman
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

For both cases, Singularity and Podman, the resulting output filder will be located in ${run_folder} with the filenames
```
predictions_unirule.out
predictions_unirule-pirsr.out
predictions_arba.out
```

## 3. Run UniFIRE after building it from its source code

### Prerequisites

#### Hardware

A machine with 24 GB or more is recommended.

#### Operating system support

The Java software is portable for any system.
Scripts are only provided for Linux and Mac OS.

#### Software

- Java 17 (e.g. OpenJDK 17)
- Bash
- Maven (version 3.6.0 has been tested successfully)

### Build
In order to build the software and download the latest rules and templates into the folder samples/ 
please execute below command. 

```
<Path to UniFIRE parent folder>/build.sh
```

Depending on the speed of your internet connection, it will take a few minutes to download all dependencies through 
maven. You will require in total ~500 MB disk space in UniFIRE folder and in your local maven cache. The script also 
downloads the latest UniRule, UniRule-PIRSR and ARBA rules in URML format and UniRule template
alignments in fact XML format from EBI FTP into samples/ folder. Additionally, it downloads data necessary to run
UniRule-PIRSR rules from https://proteininformationresource.org/pirsr/pirsr_data_latest.tar.gz and places them
under the folder samples/pirsr_data. 
   
### Usage

We provide some sample files in the [sample](samples) folder to test the software.<br/>
**build.sh MUST be executed** before trying out the command below using sample files provided
<br/>

**Example with UniRule rules & InterProScan XML input:**
``` bash
./distribution/bin/unifire.sh -r samples/unirule-urml-latest.xml -i samples/input_ipr.xml -t samples/unirule-templates-latest.xml -o output_unirule_annotations.csv
```

*Note: To be able to predict the UniRule positional annotations, a template file is provided (`samples/unirule-templates-latest.xml`) (optional.)*
<br/>

**Example with ARBA rules & Fact XML input:**
``` bash
./distribution/bin/unifire.sh -r samples/arba-urml-latest.xml -i samples/input_facts.xml -s XML -o output_arba_annotations.csv
```
<br/>

**Example with PIRSR rules and protein data in InterProScan XML format:**

In order to use UniRule-PIRSR rules to annotate protein input data, alignments of the protein sequences against
 SRHMM signatures need to be calculated in a preparation step. This requires *HMMER*, in particular an
  installation of the executable *hmmalign*. With Ubuntu 18.04 *hmmeralign* can be installed at /usr/bin/hmmalign
   by the command below:
``` bash 
sudo apt-get install hmmer
```
As an alternative, *hmmer* source code can be downloaded from the http://hmmer.org/. In the example below we
 assume hmmalign binary is available at this path on the filesystem: /usr/bin/hmmalign

Running UniRule-PIRSR rules is a two step process:
First, calculate the alignment(s) of your protein(s) against all SRHMM signatures, combine data from the input in
 InterProScan XML format with these alignments and write the output to the Fact XML file PIRSR-input-iprscan-urml.xml:   
``` bash
./distribution/bin/pirsr.sh -i ./samples/pirsr_data/PIRSR-input-iprscan.xml -o . -a /usr/bin/hmmalign -d ./samples/pirsr_data
```
Second run UniFIRE with UniRule-PIRSR rules and PIRSR-templates on the protein data in PIRSR-input-iprscan-urml.xml:
``` bash
./distribution/bin/unifire.sh -r samples/unirule.pirsr-urml-latest.xml  -i ./PIRSR-input-iprscan-urml.xml -s XML -t samples/pirsr_data/PIRSR_templates.xml -o ./pirsr_unifire_annotation.csv
```

_Note_: With all rule systems, it is possible that a protein gets the exact same annotation from different rules due
 to overlap in condition spaces.

#### Options

```
usage: unifire -i <INPUT_FILE> -o <OUTPUT_FILE> -r <RULE_URML_FILE> [-f <OUTPUT_FORMAT>] [-n
       <INPUT_CHUNK_SIZE>] [-s <INPUT_SOURCE>] [-t <TEMPLATE_FACTS>] [-h]
--------------------------------------------
     -i,--input <INPUT_FILE>                Input file (path) containing the proteins to annotate
                                            and required data, in the format specified by the -s
                                            option.
     -o,--output <OUTPUT_FILE>              Output file (path) containing predictions in the format
                                            specified in the -f option.
     -r,--rules <RULE_URML_FILE>            Rule base file (path) provided by UniProt (e.g UniRule
                                            or ARBA) (format: URML).
     -f,--output-format <OUTPUT_FORMAT>     Output file format. Supported formats are:
                                            - TSV (Tab-Separated Values)
                                            - XML (URML Fact XML)
                                            (default: TSV).
     -n,--chunksize <INPUT_CHUNK_SIZE>      Chunk size (number of proteins) to be batch processed
                                            simultaneously
                                            (default: 1000).
     -s,--input-source <INPUT_SOURCE>       Input source type. Supported input sources are:
                                            - InterProScan (InterProScan Output XML)
                                            - InterProScan6 (InterProScan6 Output XML)
                                            - UniParc (UniParc XML)
                                            - XML (Input Fact XML)
                                            (default: InterProScan).
     -t,--templates <TEMPLATE_FACTS>        UniRule template sequence matches, provided by UniProt
                                            (format: Fact Model XML).
     -h,--help                              Print this usage.
```

***

## Data preparation

This section is a walk through on how to prepare your data, assuming you are starting from scratch: from a set of
 sequences (multifasta) that you would like to annotate.

More advanced users / developers with an existing bioinformatics pipeline already integrating InterProScan results can directly pass the InterProScan file in xml format. Note that the same fasta header format described below applies to the **xref elements name attribute** in the interproscan xml file.

### MultiFasta header format

The MultiFasta headers should, more or less, follow the UniProtKB conventions ([https://www.uniprot.org/help/fasta-headers](https://www.uniprot.org/help/fasta-headers))

The minimal structure of the header is the following:

```
>{id}|{name} {flags}
```

* `{id}` must be a unique string amongst the processed sequences
* `{name}`:
    * can be any string starting from the previous separator, that should not contain any flag
    * might contains `(Fragment)` if applicable (e.g "`ACN2_ACAGO Acanthoscurrin-2 (Fragment)`")
* `{flags}` \[mandatory]: to be considered a valid header, only the following flags should be provided:
    * OX=`{taxonomy Id}`
* `{flags}` \[optional]: If possible / applicable, you should also provide:
    * OS=`{organism name}`
    * GN=`{recommended gene name}`
    * GL=`{recommended ordered locus name (OLN) or Open Reading Frame (OLN) name}`
    * OG=`{gene location(s), comma-separated if multiple}` ([cf. organelle ontology](https://www.ebi.ac.uk/ena/WebFeat/qualifiers/organelle.html))

The UniProt header format has been slightly extended with GL and OG flags.

Optionally, `{id}` can be prepended by `{database}|`, to follow UniProt conventions. If used, it will simply be skipped during the parsing.

Also note that any additional flags will also be ignored.

#### Examples of valid headers:

The standard header used in UniProt:
```
>tr|Q3SA23|Q3SA23_9HIV1 Protein Nef (Fragment) OS=Human immunodeficiency virus 1  OX=11676 GN=nef PE=3 SV=1
```

The standard UniProt header, customized with additional flags:
```
>tr|A0A0D6DT88|A0A0D6DT88_BRADI Maturase K (Fragment) OS=Brachypodium distachyon OX=15368 GN=matK GL=BN3904_34004 OG=Plastid,Chloroplast PE=3 SV=1
```

Customized minimal header:
```
>123|Mystery protein OX=62977
```

Customized full header:
```
>MyPlantDB|P987|Photosystem II protein D1 OS=Lolium multiflorum OX=4521 GN=psbA GL=LomuCp001 OG=Plastid
```

### Fetching the full lineages

From the previously described header format, you can use the following script to fetch the full NCBI taxonomy id lineage. The script has dependency on NCBITaxa python package (ete4).

* python [./misc/taxonomy/updateIPRScanWithTaxonomicLineage.py](misc/taxonomy/fetchLineageLocal.py) `-i <input>` `-o <output>`

The script will simply replace the OX={taxId} by OX={fullLineage} in the **xref element name attribute**.

Having the full lineage is necessary for the majority of the rules to be executed.

### Validating the MultiFasta file

If you want to ensure the headers are in the correct format, you can run the following script:

``` bash
./distribution/bin/fasta-header-validator.sh multifasta_sequences.fasta
```

You will get an error message if at least one sequence's header is invalid.
The script also print out warnings if an important data (e.g organism name) is missing. The warnings can be ignored.

### Running InterProScan

Once the multifasta file is ready (cf. previous steps), you can find the matches of all sequences using InterProScan.
It is advised to download the last version from [https://www.ebi.ac.uk/interpro/download.html](https://www.ebi.ac.uk/interpro/download.html) and keep it up-to-date.

The output format must be XML to be accepted as a valid input for UniFIRE.

The option `-dp` or `--disable-precalc` must be used to be able to get the sequence alignments (necessary if you are interested in the positional features annotations provided by UniRule).

Command:

``` bash
./interproscan.sh -f xml -dp -i multifasta_sequences.fasta --appl "Hamap,ProSiteProfiles,ProSitePatterns,Pfam,NCBIFAM,SMART,PRINTS,SFLD,CDD,Gene3D,PIRSF,PANTHER,SUPERFAMILY"
```

#### Analyses to run

* Hamap
* ProSiteProfiles
* ProSitePatterns
* Pfam
* NCBIfam
* SMART
* PRINTS
* SFLD
* CDD
* Gene3D
* ProDom
* PIRSF
* PANTHER
* SUPERFAMILY
* FunFam

It is possible to include/exclude some of the analyses by modifying the `--appl` option in the above command. UniFIRE will still be able to process the data. 
By excluding some of those analyses, some rules might not be triggered as a result.

If you do not wish to install InterProScan, you can use the [online version](https://www.ebi.ac.uk/interpro/search/sequence-search) and then download the results in XML.
The only limitation is that the online version does not provide the sequence alignments for the matches, making the execution of UniRule positional features impossible (non-positional rules will still be executed).

### Running InterProScan 6

InterProScan 6 is run automatically by the Nextflow pipeline when the input is a FASTA file. If you prefer to run it separately, the InterProScan 6 Nextflow workflow can be executed directly. See the [InterProScan 6 documentation](https://interproscan6.readthedocs.io/) for full details.

Prerequisites:
- [Nextflow](https://www.nextflow.io/)
- A container engine: **Docker** (default), **Singularity** or **Podman**

Command:

``` bash
nextflow run ebi-pf-team/interproscan6 \
  --applications HAMAP,PROSITE-profiles,PROSITE-patterns,Pfam,NCBIFAM,SMART,PRINTS,SFLD,CDD,CATH-Gene3D,PIRSF,PANTHER,SUPERFAMILY,CATH-FunFam \
  -r 6.0.1 \
  --interpro latest \
  -profile docker \
  --datadir iprscan6-data \
  --input multifasta_sequences.fasta \
  --formats xml \
  --outdir results
```

#### Analyses to run

* HAMAP
* PROSITE-profiles
* PROSITE-patterns
* Pfam
* NCBIFAM
* SMART
* PRINTS
* SFLD
* CDD
* CATH-Gene3D
* PIRSF
* PANTHER
* SUPERFAMILY
* CATH-FunFam

It is possible to include/exclude some of the analyses by modifying the `--applications` option in the above command. UniFIRE will still be able to process the data.
By excluding some of those analyses, some rules might not be triggered as a result.

### Running UniFIRE

Once you get the InterProScan output (by default, it is the name of the input file, appended with .xml), you can use it as a input for UniFIRE (e.g `--input multifasta_sequences.fasta.xml`).

***

## Developer guide

### Fact Model

The fact model is automatically created from the following XML Schema: [urml-facts.xsd](core/src/main/resources/schemas/xsd/urml-facts.xsd)

The corresponding Java classes are built in the [core](core) module under the package `org.uniprot.urml.facts`, after building the project with Maven (or using the `./build.sh` script).

You can use these classes (described in the UML diagram below) and the container `org.uniprot.urml.facts.FactSet` to load your own data directly into objects (via an ORM or a custom parser).

### Fact Model Diagram

![fact-model-diagram](misc/media/fact-model.png)

### Rule Model

The rule model is described in another XML Schema available here: [urml-rules.xsd](core/src/main/resources/schemas/xsd/urml-rules.xsd)  

The corresponding Java classes are built in the [core](core) module under the package `org.uniprot.urml.rules`.

### Building the software

#### Requirement

- Maven (>= 3.0)
- Java (>=11 required, cf. limitations)

Please make sure your JAVA_HOME environment variable points to the root folder of your JDK 11 installation, e.g.

```
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
```
#### Command

``` bash
./build.sh
```

This script is installing some libraries in your local maven repository (temporary solution before publishing the artifacts on a public repository).

Then it runs `mvn clean install`, assembling all the libs and the execution script under `./distribution/target/unifire-distribution/`.

To use the newly built distribution, you will need to run: `./distribution/target/unifire-distribution/bin/unifire.sh` instead.


### Execution

The execution of the rules relies on [Drools](https://www.drools.org), an open-source rule-based technology developed by RedHat. 
It is using an optimized version of the [Rete algorithm](https://en.wikipedia.org/wiki/Rete_algorithm) to match facts and rules in a scalable way.
This tool translates the URML rules into the Drools language, converts the input data according to the rule model and execute all the rules to produce a list of protein annotations.  

***

## Limitations

### Memory
A minimum of 24 GB of memory is recommended for this software to run. By default, the JVM max heap space is configured to use 75% of the available memory. 
For a large number of protein to process, it is advised to split them into chunks of approx. 500 proteins per rule evaluation to keep the memory usage low.
This is automatically handled by the `-n / --chunksize` option of UniFIRE (by default 500).  
In case you face OOM heap space memory errors, try to either use a smaller chunksize (-n option) or manually split the input file into smaller chunks.

### Java 9 / 10 issues

- For users, the software will be functional under Java 9 or 10, but you will get some warning messages complaining about illegal reflective accesses. You can simply ignore them at the moment.
- For developers, building the software with JDK 9 / 10 is currently not possible because of JAXB Maven plugin issues. Cf:

    * https://github.com/highsource/maven-jaxb2-plugin/issues/120
    * https://stackoverflow.com/questions/49717155/failed-to-run-custom-xjc-extension-within-cxf-xjc-plugin-on-java-9

    Those issues have been raised very recently. The fixes will be applied as soon as there are available.

***

## Issues & Suggestions

If you have any questions regarding this software, if you experience any bugs or have suggestions for improvements on
 the software, the models, the helper scripts, the documentation, etc, please contact us through the UniFIRE mailing
  list
  
  * **UniFIRE Mailing List** - [unifire@ebi.ac.uk](mailto:unifire:ebi.ac.uk)


## Authors

* **Alexandre Renaux**
* **Chuming Chen**
* **Hermann Zellner**
* **Vishal Joshi**

## Contact

* **UniProt Help** - https://www.uniprot.org/contact
