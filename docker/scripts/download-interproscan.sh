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

ROOT_FOLDER="/opt"
DOWNLOAD_FOLDER="/opt/download"

function set_interpro_scan_version() {
  local SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
  IPRS_VERSION="$(grep -E "^INTERPROSCAN_VERSION=" "${SCRIPT_DIR}"/versions.properties | cut -d '=' -f 2)"
  echo "INTERPROSCAN_VERSION: ${IPRS_VERSION}"
}

set_interpro_scan_version
mkdir -p ${DOWNLOAD_FOLDER}
cd ${DOWNLOAD_FOLDER}
echo "Downloading InterProScan..."
wget -q ftp://ftp.ebi.ac.uk/pub/software/unix/iprscan/5/${IPRS_VERSION}/interproscan-${IPRS_VERSION}-64-bit.tar.gz
echo "Done."
wget -q ftp://ftp.ebi.ac.uk/pub/software/unix/iprscan/5/${IPRS_VERSION}/interproscan-${IPRS_VERSION}-64-bit.tar.gz.md5

ipr_check=$(md5sum -c interproscan-${IPRS_VERSION}-64-bit.tar.gz.md5)

if [[ ${ipr_check} != "interproscan-${IPRS_VERSION}-64-bit.tar.gz: OK" ]]
then
	exit 11
fi

mkdir -p ${ROOT_FOLDER}
cd ${ROOT_FOLDER}
echo "Extracting InterProScan..."
tar -pxzf ${DOWNLOAD_FOLDER}/interproscan-${IPRS_VERSION}-64-bit.tar.gz
ln -s "interproscan-${IPRS_VERSION}" interproscan
echo "Done."

echo "Initializing InterProScan..."
cd /opt/interproscan
python3 setup.py interproscan.properties
echo "Done initializing InterProScan"

# Clean up tar to reduce the size of the image
rm -f ${DOWNLOAD_FOLDER}/interproscan-${IPRS_VERSION}-64-bit.tar.gz
rm -f ${DOWNLOAD_FOLDER}/interproscan-${IPRS_VERSION}-64-bit.tar.gz.md5
