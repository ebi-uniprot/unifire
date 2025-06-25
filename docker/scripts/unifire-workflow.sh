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

set -e
set -u


function usage() {
  cat <<EOF
Usage: $(basename "$0") [options]
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
Examples:
  $(basename "$0") -i proteins.fasta -t fasta -n 500 -o /volume
  $(basename "$0") -i proteins-ipr.xml -t iprscanxml -s unirule,arba -n 1000 -o /volume
  $(basename "$0") (without options) - will look for default files in the /volume output directory and run all systems.

Note that input and output directories must be mounted in the container.
EOF
}

function usage_exit() {
  code="${1:-1}"
  usage
  exit "$code"
}

# --- Constants and Defaults ---
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="${JAVA_HOME}/bin:${PATH}"
VERSION_PROP_FILE="/opt/scripts/bin/versions.properties"
UNIFIRE_REPO="/opt/git/unifire"
ETE4_TAXA="/opt/ete4/taxa.sqlite"
UNIFIRE_SCRIPT="${UNIFIRE_REPO}/distribution/bin/unifire.sh"
PIRSR_SCRIPT="${UNIFIRE_REPO}/distribution/bin/pirsr.sh"
LINEAGE_SCRIPT="${UNIFIRE_REPO}/misc/taxonomy/updateIPRScanWithTaxonomicLineage.py"
INTERPROSCAN_SCRIPT="/opt/interproscan/interproscan.sh"
HMMALIGN="/usr/bin/hmmalign"

URML_DIR="${UNIFIRE_REPO}/samples"
PIRSR_DIR="${UNIFIRE_REPO}/samples/pirsr_data"
OUT_DIR=/volume

CHUNK_SIZE=500
SYSTEM="unirule,arba,pirsr"  # Default to all systems
# Allowed systems (case-insensitive)
#ALLOWED_SYSTEMS=("unirule" "arba" "pirsr")
# split on comma and Create map of allowed systems (all lowercase)
IFS=',' read -ra ALLOWED_SYSTEMS <<< "$SYSTEM"
declare -A allowed_systems_map
for item in "${ALLOWED_SYSTEMS[@]}"; do
    key=$(echo "$item" | tr '[:upper:]' '[:lower:]')
    allowed_systems_map["$key"]=1
done

# for user input systems
declare -A input_systems_map

# --- Parse args ---
while getopts ":i:t:s:n:o:h" opt; do
  case $opt in
    i) INPUT_FILE="$OPTARG" ;;
    t) INPUT_TYPE="$OPTARG" ;;
    s) SYSTEM="$OPTARG" ;;
    n) CHUNK_SIZE="$OPTARG" ;;
    o) OUT_DIR="$OPTARG" ;;
    h) usage_exit 0 ;;
    \?)
      echo "Invalid option: -$OPTARG"
      usage_exit ;;
    :)
      echo "Option -$OPTARG requires an argument."
      usage_exit ;;
  esac
done

UR_RULES="${URML_DIR}/unirule-urml-latest.xml"
UR_TMPL="${URML_DIR}/unirule-templates-latest.xml"
ARBA_RULES="${URML_DIR}/arba-urml-latest.xml"
PIRSR_RULES="${URML_DIR}/unirule.pirsr-urml-latest.xml"
PIRSR_TMPL="${PIRSR_DIR}/PIRSR_templates.xml"


function validate_input_output() {
  if [[ -z "${OUT_DIR:-}" ]]; then
    echo "Error: Output directory (-o) is required."
    usage_exit
  fi

  # if no input file and type provided, then try to use default file names in the output directory
  DEFAULT_FASTA_FILE=${OUT_DIR}/proteins.fasta
  DEFAULT_IPRSCAN_FILE=${OUT_DIR}/proteins-ipr.xml
  if [[ -z "${INPUT_FILE:-}" && -z "${INPUT_TYPE:-}" ]]; then
    echo "No input file provided. Looking for default files..."
    # If iprscan xml file name 'proteins-ipr.xml' is found, then it is used as input and type is set to iprscanxml.
    # Otherwise, if fasta file name 'proteins.fasta' is found, then it is used as input and type is set to fasta.
    if [ -f "${DEFAULT_IPRSCAN_FILE}" ]; then
        INPUT_FILE=${DEFAULT_IPRSCAN_FILE}
        INPUT_TYPE="iprscanxml"
        echo "Using default input file: ${INPUT_FILE} of type ${INPUT_TYPE}"
    elif [ -f "${DEFAULT_FASTA_FILE}" ]; then
        INPUT_FILE=${DEFAULT_FASTA_FILE}
        INPUT_TYPE="fasta"
        echo "Using default input file: ${INPUT_FILE} of type ${INPUT_TYPE}"
    fi
  fi
  # if only input file is provided, then try to set type based on file name
  if [[ -n "${INPUT_FILE:-}" && -z "${INPUT_TYPE:-}" ]]; then
    FILE_EXT="${INPUT_FILE##*.}"
    if [[ "$FILE_EXT" == "fasta" || "$FILE_EXT" == "fa" ]]; then
      INPUT_TYPE="fasta"
    elif [[ "$FILE_EXT" == "xml" ]]; then
      INPUT_TYPE="iprscanxml"
    else
      echo "Error: Unable to determine input type from file name '$INPUT_FILE'. Please specify using -t option."
      usage_exit
    fi
  fi

  if [[ -z "${INPUT_FILE:-}" || -z "${INPUT_TYPE:-}" ]]; then
    echo "Error: input file information is required."
    usage_exit
  fi
}

function validate_input_file() {
  if [[ ! -f "$INPUT_FILE" ]]; then
    echo "Error: Input file '$INPUT_FILE' does not exist."
    usage_exit
  fi
  if [[ "$INPUT_TYPE" != "fasta" && "$INPUT_TYPE" != "iprscanxml" ]]; then
    echo "Error: Invalid input type '$INPUT_TYPE'."
    usage_exit
  fi
}

function validate_chunk() {
  if [[ "$CHUNK_SIZE" -le 0 ]]; then echo "Error: Chunk size must be greater than zero." && usage_exit; fi
}

function validate_system() {
  # split on comma and build map of user inputs (in lowercase)
  IFS=',' read -ra input_systems <<< "$SYSTEM"
  for item in "${input_systems[@]}"; do
      lower_item=$(echo "$item" | tr '[:upper:]' '[:lower:]')

      if [[ -z "${allowed_systems_map[$lower_item]}" ]]; then
          echo "Invalid system: '$item'"
          usage_exit
      fi

      input_systems_map["$lower_item"]=1
  done
  echo "Selected systems: ${!input_systems_map[@]}"
}

function validate_out_dir() {
  if [[ ! -d "$OUT_DIR" ]]; then mkdir -p "$OUT_DIR"; fi
}

function check_iprscan_version() {
    input_iprscan_version="$(head -n 5 ${IPRSCAN_FILE} | grep -o 'interproscan-version="[^"]*"' | sed -E 's/interproscan-version="([^"]*)"/\1/')"
    unifire_iprscan_version="$(grep -E "^INTERPROSCAN_VERSION=" "${VERSION_PROP_FILE}" | cut -d '=' -f 2)"
    if [[ "${input_iprscan_version}" != "${unifire_iprscan_version}" ]]; then
        # If the versions do not match, show warning to the user (colored with ANSI escape codes)
        echo -e "\033[33mWARNING: Input interproscan version ${input_iprscan_version} does not match UniFIRE interproscan version ${unifire_iprscan_version} used for the rules." \
            "This may cause some compatibility issues. It is recommended to run interproscan with the same version as UniFIRE.\033[0m"
    fi
}

function run_workflow() {
  # Conditionally run or skip interproscan
  if [[ "$INPUT_TYPE" == "fasta" ]]; then
      echo "Running interproscan on input fasta file ${INPUT_FILE}..."
      ${INTERPROSCAN_SCRIPT} -f xml -dp -i ${INPUT_FILE} \
          --appl "Hamap,ProSiteProfiles,ProSitePatterns,Pfam,NCBIFAM,SMART,PRINTS,SFLD,CDD,Gene3D,PIRSF,PANTHER,SUPERFAMILY,FunFam" \
          -o ${IPRSCAN_FILE}
  else
      echo "Skipping interproscan and using provided file ${INPUT_FILE}..."
      check_iprscan_version
  fi

  # Update the lineage in the interproscan file
  echo "Updating taxonomy lineage for interproscan file ${IPRSCAN_FILE} into ${IPRSCAN_LINEAGE_FILE}..."
  ${LINEAGE_SCRIPT} -i ${IPRSCAN_FILE} -o ${IPRSCAN_LINEAGE_FILE} -t ${ETE4_TAXA}


  # UniFIRE on UniRule rules
  if [[ -v input_systems_map["unirule"] ]]; then
      echo "Running rules inference on UniRule..."
      ${UNIFIRE_SCRIPT} -n ${CHUNK_SIZE} -r ${UR_RULES} -i ${IPRSCAN_LINEAGE_FILE} -t ${UR_TMPL} -o ${UR_OUT_FILE}
  fi

  # UniFIRE on ARBA rules
  # UniFIRE on ARBA rules
  if [[ -v input_systems_map["arba"] ]]; then
    echo "Running rules inference on ARBA..."
    ${UNIFIRE_SCRIPT} -n ${CHUNK_SIZE} -r ${ARBA_RULES} -i ${IPRSCAN_LINEAGE_FILE} -o ${ARBA_OUT_FILE}
  fi

  # UniFIRE on PIRSR rules
  # This is a two-steps process: run hmmalign to align the sequences to the PIRSR templates, and then run rules inference
  if [[ -v input_systems_map["pirsr"] ]]; then
    echo "Running PIRSR hmmalign..."
    ${PIRSR_SCRIPT} -i ${IPRSCAN_LINEAGE_FILE} -o ${OUT_DIR} -a ${HMMALIGN} -d ${PIRSR_DIR}

    echo "Running rules inference on PIRSR..."
    ${UNIFIRE_SCRIPT} -n ${CHUNK_SIZE} -r ${PIRSR_RULES} -i ${PIRSR_FACT_FILE} -s XML -t ${PIRSR_TMPL} -o ${PIRSR_OUT_FILE}
  fi
}

function set_output_files_permission() {
  # prediction output files must belong to the same user and group as input file
  ownership=`stat -c "%u:%g" ${INPUT_FILE}`
  for outfile in  ${IPRSCAN_LINEAGE_FILENAME} ${PIRSR_FACT_FILENAME} ${UR_OUT_FILENAME} \
    ${ARBA_OUT_FILENAME} ${PIRSR_OUT_FILENAME} seq aln
  do
    if [[ -e ${OUT_DIR}/${outfile} ]]
    then
      chown -R ${ownership} ${OUT_DIR}/${outfile}
    fi
  done
}

# Run validations (needs to be called in the correct order)
validate_input_output
validate_input_file
validate_system
validate_chunk
validate_out_dir

# --- Summary ---
echo "Input: $INPUT_FILE"
echo "Type: $INPUT_TYPE"
echo "System: $SYSTEM"
echo "Chunk size: $CHUNK_SIZE"
echo "Output dir: $OUT_DIR"

# Set output file names based on input file basename
INPUT_FILE_BASENAME=$(basename "$INPUT_FILE") && INPUT_FILE_BASENAME="${INPUT_FILE_BASENAME%.*}"
if [[ "$INPUT_TYPE" == "iprscanxml" ]]; then
  IPRSCAN_FILE=$INPUT_FILE;
else
  IPRSCAN_FILE="${OUT_DIR}/${INPUT_FILE_BASENAME}-ipr.xml"
fi
IPRSCAN_LINEAGE_FILENAME="${INPUT_FILE_BASENAME}-lineage.xml"
IPRSCAN_LINEAGE_FILE="${OUT_DIR}/${IPRSCAN_LINEAGE_FILENAME}"

PIRSR_FACT_FILENAME="${IPRSCAN_LINEAGE_FILENAME%.*}-urml.xml"
PIRSR_FACT_FILE="${OUT_DIR}/${PIRSR_FACT_FILENAME}"

# predictions output files
UR_OUT_FILENAME="predictions_unirule.out"
UR_OUT_FILE="${OUT_DIR}/${UR_OUT_FILENAME}"
ARBA_OUT_FILENAME="predictions_arba.out"
ARBA_OUT_FILE="${OUT_DIR}/${ARBA_OUT_FILENAME}"
PIRSR_OUT_FILENAME="predictions_unirule-pirsr.out"
PIRSR_OUT_FILE="${OUT_DIR}/${PIRSR_OUT_FILENAME}"

# Run the workflow
run_workflow
# Set permissions for output files
set_output_files_permission
echo "Finished running UniFIRE workflow... Output files generated in: $OUT_DIR"
