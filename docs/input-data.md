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

## Running InterProScan

Once the multifasta file is ready (cf. previous steps), you can find the matches of all sequences using InterProScan.
It is advised to download the last version from [https://www.ebi.ac.uk/interpro/download.html](https://www.ebi.ac.uk/interpro/download.html) and keep it up-to-date.

The output format must be XML to be accepted as a valid input for UniFIRE.

The option `-dp` or `--disable-precalc` must be used to be able to get the sequence alignments (necessary if you are interested in the positional features annotations provided by UniRule).

Command:

```bash
./interproscan.sh -f xml -dp -i multifasta_sequences.fasta --appl "Hamap,ProSiteProfiles,ProSitePatterns,Pfam,NCBIFAM,SMART,PRINTS,SFLD,CDD,Gene3D,PIRSF,PANTHER,SUPERFAMILY"
```

### Analyses to run

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

## Running InterProScan 6

InterProScan 6 is run automatically by the [Nextflow pipeline](nextflow.md) when the input is a FASTA file. If you prefer to run it separately, the InterProScan 6 Nextflow workflow can be executed directly. See the [InterProScan 6 documentation](https://interproscan6.readthedocs.io/) for full details.

Prerequisites:
- [Nextflow](https://www.nextflow.io/) (at least 26.04)
- A container engine: **Docker** (default), **Singularity** or **Podman**

Command:

```bash
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

### Analyses to run

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

## Running UniFIRE on the prepared data

Once you get the InterProScan output (by default, it is the name of the input file, appended with .xml), you can use it as a input for UniFIRE (e.g `--input multifasta_sequences.fasta.xml`) with any of the [three ways to run UniFIRE](../README.md#running-unifire).
