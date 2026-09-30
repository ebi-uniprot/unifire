#!/usr/bin/env bash

############################################################################
#    Copyright (c) 2026 European Molecular Biology Laboratory
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

# Runs the UniFIRE Nextflow pipeline locally inside the unifire/nextflow
# container (profile "local"), keeping the legacy /volume contract of the
# retired unifire image:
#
#   /volume/proteins.fasta     multi-FASTA input  -> --inputType fasta
#   /volume/proteins-ipr.xml   InterProScan input -> --inputType InterProScan
#
# The prediction files (predictions_unirule.out, predictions_arba.out,
# predictions_unirule-pirsr.out) are published into /volume.
#
# Downloaded rule/taxonomy/InterProScan data is stored in
# ${UNIFIRE_DATA_PATH} (default: /opt/unifire/data). Mount a host directory
# there to persist it across runs.
#
# Additional pipeline options can be passed via the UNIFIRE_NXF_ARGS
# environment variable (e.g. --systems unirule,arba). Arguments appended to
# `docker run` are NOT supported because the image is also used as the
# per-process container image (its CMD is replaced by the task command).

set -e
set -u

VOLUME=/volume
FASTA_INPUT="${VOLUME}/proteins.fasta"
IPR_XML_INPUT="${VOLUME}/proteins-ipr.xml"
DATA_PATH="${UNIFIRE_DATA_PATH:-/opt/unifire/data}"

if [[ -f "${IPR_XML_INPUT}" ]]
then
    input="${IPR_XML_INPUT}"
    input_type="InterProScan"
elif [[ -f "${FASTA_INPUT}" ]]
then
    input="${FASTA_INPUT}"
    input_type="fasta"
else
    echo "ERROR: no input file found in ${VOLUME}." >&2
    echo "Expected ${FASTA_INPUT} (multi-FASTA) or ${IPR_XML_INPUT} (InterProScan XML)." >&2
    exit 1
fi

echo "UniFIRE input file: ${input} (type: ${input_type})"
echo "UniFIRE data directory: ${DATA_PATH}"

cd "${VOLUME}"

# shellcheck disable=SC2086
exec nextflow run /opt/unifire/main.nf \
    -profile local \
    --input "${input}" \
    --inputType "${input_type}" \
    --output "${VOLUME}" \
    --dataPath "${DATA_PATH}" \
    -work-dir "${VOLUME}/nxf-work" \
    ${UNIFIRE_NXF_ARGS:-} \
    "$@"
