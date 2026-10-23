# Building and running UniFIRE from the source code

This approach requires more manual interaction from the user. Each step of a UniFIRE workflow must be executed
separately or combined by a script. Also, some steps require external software like InterProScan or HMMER, which
needs to be installed by the user separately or accessed through a web interface. We recommend this approach to
advanced users who wish to create a particular workflow, e.g. who need to run the heavy InterProScan within a
separate procedure.

## Prerequisites

### Hardware

A machine with 24 GB or more is recommended.

### Operating system support

The Java software is portable for any system.
Scripts are only provided for Linux and Mac OS.

### Software

- Java 17 (e.g. OpenJDK 17)
- Bash
- Maven (version 3.6.0 has been tested successfully)

## Build

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

## Usage

We provide some sample files in the [sample](../samples) folder to test the software.<br/>
**build.sh MUST be executed** before trying out the command below using sample files provided
<br/>

**Example with UniRule rules & InterProScan XML input:**
```bash
./distribution/bin/unifire.sh -r samples/unirule-urml-latest.xml -i samples/input_ipr.xml -t samples/unirule-templates-latest.xml -o output_unirule_annotations.csv
```

*Note: To be able to predict the UniRule positional annotations, a template file is provided (`samples/unirule-templates-latest.xml`) (optional.)*
<br/>

**Example with ARBA rules & Fact XML input:**
```bash
./distribution/bin/unifire.sh -r samples/arba-urml-latest.xml -i samples/input_facts.xml -s XML -o output_arba_annotations.csv
```
<br/>

**Example with PIRSR rules and protein data in InterProScan XML format:**

In order to use UniRule-PIRSR rules to annotate protein input data, alignments of the protein sequences against
SRHMM signatures need to be calculated in a preparation step. This requires *HMMER*, in particular an
installation of the executable *hmmalign*. With Ubuntu 18.04 *hmmeralign* can be installed at /usr/bin/hmmalign
by the command below:
```bash
sudo apt-get install hmmer
```
As an alternative, *hmmer* source code can be downloaded from the http://hmmer.org/. In the example below we
assume hmmalign binary is available at this path on the filesystem: /usr/bin/hmmalign

Running UniRule-PIRSR rules is a two step process:
First, calculate the alignment(s) of your protein(s) against all SRHMM signatures, combine data from the input in
InterProScan XML format with these alignments and write the output to the Fact XML file PIRSR-input-iprscan-urml.xml:
```bash
./distribution/bin/pirsr.sh -i ./samples/pirsr_data/PIRSR-input-iprscan.xml -o . -a /usr/bin/hmmalign -d ./samples/pirsr_data
```
Second run UniFIRE with UniRule-PIRSR rules and PIRSR-templates on the protein data in PIRSR-input-iprscan-urml.xml:
```bash
./distribution/bin/unifire.sh -r samples/unirule.pirsr-urml-latest.xml  -i ./PIRSR-input-iprscan-urml.xml -s XML -t samples/pirsr_data/PIRSR_templates.xml -o ./pirsr_unifire_annotation.csv
```

_Note_: With all rule systems, it is possible that a protein gets the exact same annotation from different rules due
to overlap in condition spaces.

## Options

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

## Next steps

For details on preparing your input data (FASTA headers, InterProScan runs), see [Input data preparation](input-data.md).
