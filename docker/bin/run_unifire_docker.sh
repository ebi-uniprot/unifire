#!/usr/bin/env bash

############################################################################
#    Copyright (c) 2018 European Molecular Biology Laboratory
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
############################################################################

# Legacy-compatible wrapper around the UniFIRE Nextflow image
# (ghcr.io/ebi-uniprot/unifire/nextflow). The image runs the UniFIRE
# Nextflow pipeline inside the container; this script only prepares the
# working directory, runs the image and collects the prediction files.

set -e
set -u

infile=""
filetype="fasta" # either fasta or iprscanxml
outdir=""
workdir=""
datadir=""
cleanworkdir=0
container_software="docker"
docker_version=""
predictionfiles="predictions_unirule.out predictions_arba.out predictions_unirule-pirsr.out"

function usage() {
    echo "usage: $0 -i <INPUT_FILE> -o <OUTPUT_FOLDER> [-t <FILE_TYPE>] [-v <VERSION>] [-w <WORKING_FOLDER] [-d <DATA_FOLDER>] [-c]"
    echo "          [-s docker|singularity|podman]"
    echo "    -i: Path to input file (Required). Can be either multi-FASTA file (default) or InterProScan xml file (see -t option)."
    echo "    -t: Input file type. (Optional), DEFAULT: fasta"
    echo "        Allowed values:"
    echo "        fasta: multi-FASTA file with headers in UniProt FASTA header format, containing at least OX=<taxid>"
    echo "        iprscanxml: InterProScan file in xml format. Each protein should have at least one xref element with 'name' attribute containing OX=<taxid>"
    echo "    -o: Path to output folder. All output files with predictions in TSV format will be available in this"
    echo "        folder at the end of the procedure. (Required)"
    echo "    -v: Version of the docker image to use, e.g. 3.1.0. Available versions are listed under"
    echo "        https://github.com/ebi-uniprot/unifire/pkgs/container/unifire%2Fnextflow. (Optional), DEFAULT: engine"
    echo "        version (unifireVersion) defined in nextflow/defaults.nf"
    echo "    -w: Path to an empty working directory.  If this option is not given, then a temporary folder will be"
    echo "        created and used to store intermediate files. (Optional)"
    echo "    -d: Path to a data directory used to cache downloaded data (URML rules, PIRSR data, taxonomy and"
    echo "        InterProScan data). If set, the directory is mounted into the container and the data persists"
    echo "        between runs. If not given, data is downloaded into the container and discarded afterwards. (Optional)"
    echo "    -c: Clean up temporary files. If set, then all temporary files will be cleaned up at the end of the"
    echo "        procedure. If no working directory is provided through option -w then the temporary files are cleaned"
    echo "        up by default"
    echo "    -s: Container software to be used. (Optional), DEFAULT: docker"
    echo "        Allowed values:"
    echo "        docker: Use Docker to run UniFIRE Docker image"
    echo "        singularity: Use Singularity to run UniFIRE Docker image"
    echo "        podman: Use Podman to run UniFIRE Docker image"
    echo ""
    echo "    Environment:"
    echo "        UNIFIRE_NXF_ARGS: additional options passed to the UniFIRE Nextflow pipeline inside the"
    echo "        container, e.g. UNIFIRE_NXF_ARGS=\"--systems unirule,arba\" (default: all systems)."
    exit 1
}

while getopts "i:t:o:w:c:v:s:d:" optionName
do
  case "${optionName}" in
    i) infile=${OPTARG};;
    t) filetype="$OPTARG" ;;
    o) outdir=${OPTARG};;
    w) workdir=${OPTARG};;
    v) docker_version=${OPTARG};;
    s) container_software=${OPTARG};;
    d) datadir=${OPTARG};;
    c) cleanworkdir=1;;
  esac
done

if [ ${container_software} != "docker" ] && [ ${container_software} != "singularity" ] && \
[ ${container_software} != "podman" ]
then
    echo "Invalid container software ${container_software} given!"
    printf "This script supports docker, singularity or podman only at this time.\n\n"
    usage
fi

if ! command -v ${container_software} &> /dev/null
then
    echo "${container_software} executable could not be found. Please make sure ${container_software} is installed and available"
    printf "in the PATH environment variable. Exiting.\n\n"
    usage
fi

# determine docker image version, if provided as a CLI option, use that,
# otherwise read the engine version (unifireVersion) from nextflow/defaults.nf
function determine_docker_image_version() {
  if [ -z "$docker_version" ]
  then
    SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
    docker_version="$(grep -oE "unifireVersion:[[:space:]]*'[^']+'" "${SCRIPT_DIR}/../../nextflow/defaults.nf" | tail -1 | cut -d "'" -f 2)"
  fi
  echo "UniFIRE docker version to be used: ${docker_version}"
}

# infile
function check_infile() {
    if [[ ! -f ${infile} ]]
    then
      echo "Error: Input file ${infile} not found!"
      usage
    fi

   # Validate the file type
   if [[ "$filetype" != "fasta" && "$filetype" != "iprscanxml" ]]; then
       echo "Error: Invalid file type. Valid options are 'fasta' or 'iprscanxml'."
       usage
   fi
}

# outdir
function check_outdir() {
    if [[ ! -d ${outdir} ]]
    then
      echo "Given outdir ${outdir} does not exist. Trying to create it..."
      set +e
      mkdir -p ${outdir}
       if [[ $? == 0 ]]
      then
        echo "Successfully created output directory ${outdir}."
      else
        echo "Failed to create output directory ${outdir}."
        usage
      fi
      set -e
    fi
}

# workdir
function check_workdir() {
    usemktmp=0
    if [[ ${workdir} == "" ]]
    then
      usemktmp=1
      echo "No working directory given. Creating temporary directory."
    elif  [[ ! -d ${workdir} ]]
    then
      echo "Given working directory does not exist. Trying to create it ..."
      set +e
      mkdir -p ${workdir}
      if [[ $? == 0 ]]
      then
        echo "Successfully created working directory ${workdir}"
      else
        usemktmp=1
        echo "Failed to create working directory ${workdir}. Creating temporary directory."
      fi
      set -e
    fi

    if [[ ${usemktmp} == 0 ]] && [[ ! -z "$(ls -A ${workdir})" ]]
    then
      usemktmp=1
      echo "Given working directory ${workdir} is not empty. Creating temporary directory instead."
    fi

    if [[ ${usemktmp} == 1 ]]
    then
      workdir=`mktemp -d`
      cleanworkdir=1
      echo "Using ${workdir} for temporary files. Please make sure there is enough free space on the according filesystem."
    fi
}

# datadir (optional persistent data cache mounted into the container)
function check_datadir() {
    if [[ ${datadir} == "" ]]
    then
      return
    fi
    if [[ ! -d ${datadir} ]]
    then
      echo "Given data directory ${datadir} does not exist. Trying to create it ..."
      mkdir -p ${datadir}
      if [[ $? != 0 ]]
      then
        echo "Failed to create data directory ${datadir}."
        usage
      fi
    fi
    datadir="$(cd ${datadir} && pwd)"
    echo "Using ${datadir} as persistent data directory inside the container."
}

# Run the docker image on $the prepared {workdir}
function run_docker_image() {
    # Perform the copy based on file type
    # the entrypoint of the image expects the input files to be named exactly as below
    if [[ "$filetype" == "fasta" ]]; then
        cp -v ${infile} ${workdir}/proteins.fasta
    elif [[ "$filetype" == "iprscanxml" ]]; then
        cp -v ${infile} ${workdir}/proteins-ipr.xml
    fi

    local image="ghcr.io/ebi-uniprot/unifire/nextflow:${docker_version}"
    # Optional persistent data cache, mounted to /data inside the container
    local datamount=""
    if [[ ${datadir} != "" ]]
    then
      datamount="--mount type=bind,source=${datadir},target=/data --env UNIFIRE_DATA_PATH=/data"
    fi

    if [ ${container_software} == "docker" ]
    then
      docker run \
          --env UNIFIRE_NXF_ARGS \
          ${datamount} \
          --mount type=bind,source=${workdir},target=/volume \
          ${image}
    elif [ ${container_software} == "singularity" ]
    then
      local singularity_bind=""
      if [[ ${datadir} != "" ]]
      then
        singularity_bind="--bind ${datadir}:/data --env UNIFIRE_DATA_PATH=/data"
      fi
      singularity run \
          --env UNIFIRE_NXF_ARGS \
          ${singularity_bind} \
          --bind ${workdir}:/volume \
          docker://${image}
    elif [ ${container_software} == "podman" ]
    then
      podman run \
          --env UNIFIRE_NXF_ARGS \
          ${datamount} \
          --mount type=bind,source=${workdir},target=/volume \
          ${image}
    fi
}

# Move output files from ${workdir} to ${outdir}
function move_output_files() {
    for predictionfile in ${predictionfiles}
    do
      if [[ -f ${workdir}/${predictionfile} ]]
      then
        echo Copying prediction file ${predictionfile} to ${outdir}
        cp -p ${workdir}/${predictionfile} ${outdir}/
      else
        echo "Warning: prediction file ${predictionfile} not found in ${workdir} (was the corresponding system skipped or the run aborted?)"
      fi
    done
}

# Clean up
function cleanup_workdir() {
    if [[ ${cleanworkdir} == 1 ]]
    then
      echo "Cleaning up folder ${workdir}"
      for predictionfile in ${predictionfiles}
      do
        rm -f ${workdir}/${predictionfile}
      done
      rm -f ${workdir}/proteins.fasta
      rm -f ${workdir}/proteins_lineage.fasta
      rm -f ${workdir}/proteins-ipr.xml
      rm -f ${workdir}/proteins_lineage-ipr-urml.xml
      rm -f ${workdir}/proteins_lineage-ipr.xml
      rm -f ${workdir}/seq/*.fasta
      rm -f ${workdir}/aln/*.aln
      rm -rf ${workdir}/nxf-work
      rm -rf ${workdir}/.nextflow
      rm -f ${workdir}/.nextflow.log*
      if [[ -d ${workdir}/aln ]];
      then
        rmdir ${workdir}/aln
      fi
      if [[ -d ${workdir}/seq ]]
      then
        rmdir ${workdir}/seq
      fi
    fi
}

# main
determine_docker_image_version
check_infile
check_outdir
check_workdir
check_datadir
run_docker_image
move_output_files
cleanup_workdir
