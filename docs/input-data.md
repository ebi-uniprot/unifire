# Input data preparation

This document walks through how to prepare your data for UniFIRE, assuming you are starting from scratch: from a set
of sequences (multi-FASTA) that you would like to annotate.

More advanced users / developers with an existing bioinformatics pipeline already integrating InterProScan results
can directly pass the InterProScan file in xml format (see [InterProScan XML input](#interproscan-xml-input)).

These input formats apply to all ways of running UniFIRE: the [Nextflow pipeline](nextflow.md), the
[Docker image](docker.md) and running [from the source code](build-from-source.md).

## MultiFasta header format

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

### Examples of valid headers

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

## InterProScan XML input

Instead of a FASTA file, you can provide a precomputed InterProScan XML file. Each protein in the InterProScan XML
file should have at least one xref element with 'name' attribute containing OX=<taxid> in the same format as
described above for the fasta header, as shown in the following example.
```
<xref id="tr|J0U7L2|J0U7L2_9BURK" name="tr|J0U7L2|J0U7L2_9BURK Cytochrome c553 OS=Acidovorax sp. CF316 OX=1144317 GN=PMI14_03334"/>
```

## Validating the MultiFasta file

If you want to ensure the headers are in the correct format, you can run the following script:

```bash
./distribution/bin/fasta-header-validator.sh multifasta_sequences.fasta
```

You will get an error message if at least one sequence's header is invalid.
The script also print out warnings if an important data (e.g organism name) is missing. The warnings can be ignored.

## Fetching the full lineages

From the previously described header format, you can use the following script to fetch the full NCBI taxonomy id lineage.

* python [./misc/taxonomy/updateIPRScanWithTaxonomicLineage.py](../misc/taxonomy/updateIPRScanWithTaxonomicLineage.py) `-i <input>` `-o <output>`

The script has dependency on NCBITaxa python package (ete4).

The script will simply replace the OX={taxId} by OX={fullLineage} in the **xref element name attribute**.

Having the full lineage is necessary for the majority of the rules to be executed.

Note: the [Nextflow pipeline](nextflow.md) performs this step automatically; it is only needed when running UniFIRE
from the [source code](build-from-source.md) with a manually prepared InterProScan input.

## Running InterProScan 6

InterProScan 6 is run automatically by the [Nextflow pipeline](nextflow.md) when the input is a FASTA file. If you prefer to run it separately, you can execute the InterProScan 6 Nextflow workflow directly. See the [InterProScan 6 documentation](https://interproscan6.readthedocs.io/) for full details.

### Prerequisites

Before you begin, install:

- [Nextflow](https://www.nextflow.io/) (at least 26.04)
- A container engine: **Docker** (default), **SingularityCE**/**Apptainer** or **Podman**

You don't need anything else: Nextflow downloads the workflow from GitHub, and the required InterPro and member database files are downloaded automatically into the `--datadir` on first run.

### Command

```bash
nextflow run ebi-pf-team/interproscan6 \
  -r 6.0.2.2 \
  -profile docker \
  --input multifasta_sequences.fasta \
  --datadir iprscan6-data \
  --interpro 110.0 \
  --formats xml \
  --outdir results \
  --no-matches-api
```

Notes:

- Pin the workflow version with `-r` (e.g. `6.0.2.2`) and the InterPro release with `--interpro` (e.g. `110.0`) to ensure reproducible results. The un-pinned defaults (`--interpro latest`) are convenient but not reproducible.
- UniFIRE requires the **XML output**, hence `--formats xml`.
- `--no-matches-api` disables the InterPro Matches Lookup Service, which returns precalculated matches for already-annotated sequences (these do not include sequence alignments). It is the InterProScan 6 equivalent of the `-dp` / `--disable-precalc` option of classic InterProScan, and is required if you are interested in the positional feature annotations provided by UniRule.
- The values shown above correspond to the UniFIRE pipeline defaults (`--iprscanVersion 6.0.2.2`, `--iprVersion 110.0`, see the [Nextflow pipeline parameters](nextflow.md#pipeline-parameters)).

### Selecting analyses

By default, InterProScan 6 runs all analyses except the deep-learning-based ones.

You can control which analyses run with:

- `--applications <LIST>`: run only the selected analyses
- `--skip-applications <LIST>`: exclude the given analyses

`<LIST>` is a comma-separated list of analysis names. Names are case-insensitive, and hyphens and underscores are ignored (e.g. `CATH-Gene3D`, `cathgene3d` and `CATH_GENE3D` are all valid). The two options are mutually exclusive. See the [analysis catalogue](https://interproscan6.readthedocs.io/stable/analyses/#analysis-catalogue) for the full list of supported analyses.

The UniFIRE Nextflow pipeline runs InterProScan 6 with a default set of applications,
configurable via the `--iprscanApplications` pipeline parameter:

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

UniFIRE will still be able to process data produced with other analyses included/excluded. However, by excluding some of those analyses, some rules might not be triggered as a result.

If you do not wish to install InterProScan 6, you can use the [online version](https://www.ebi.ac.uk/interpro/search/sequence-search) and then download the results in XML.
The only limitation is that the online version does not provide the sequence alignments for the matches, making the execution of UniRule positional features impossible (non-positional rules will still be executed).

### Running classic InterProScan (legacy)

UniFIRE also still accepts XML output from the classic (InterProScan 5) workflow, for backward compatibility
with existing pipelines. This is not recommended for new runs: use InterProScan 6 instead.

Required settings for UniFIRE compatibility:

- Output format must be **XML**.
- The `-dp` / `--disable-precalc` option must be used to obtain the sequence alignments (necessary for the positional feature annotations provided by UniRule) — equivalent to the `--no-matches-api` option of InterProScan 6.

For downloading, installing and running classic InterProScan, refer to the [legacy InterProScan documentation](https://interproscan-docs.readthedocs.io) and the [ebi-pf-team/interproscan](https://github.com/ebi-pf-team/interproscan) repository.

## Running UniFIRE on the prepared data

Once you have the InterProScan XML output (e.g. `results/*.xml` from the InterProScan 6 command above), you can use it as input for UniFIRE (e.g `--input results/yourfile.xml`) with any of the [three ways to run UniFIRE](../README.md#running-unifire).
