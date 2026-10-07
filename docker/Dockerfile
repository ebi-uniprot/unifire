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

FROM ubuntu:24.04 AS base

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

RUN apt-get update \
    && DEBIAN_FRONTEND="noninteractive" apt-get install -y wget openjdk-17-jdk maven git coreutils hmmer \
     python3-pip python3 ncbi-data libdw1 libpcre3-dev python3-dev gcc libc-dev

# --break-system-packages flag is required to force system-wide installation (not in virtualenv)
RUN pip install numpy lxml https://github.com/etetoolkit/ete/archive/ete4.zip --break-system-packages

COPY scripts /opt/scripts/bin
COPY versions.properties /opt/scripts/bin
RUN chmod 775 /opt/scripts/bin/*.sh

RUN /opt/scripts/bin/update-taxonomy-cache.sh
RUN /opt/scripts/bin/download-unifire.sh

RUN mkdir /volume
VOLUME /volume

# --- Lite image (no InterProScan) ---
FROM base AS lite
ENTRYPOINT ["/opt/scripts/bin/unifire-workflow.sh"]

# --- Full image with InterProScan ---
FROM base AS full
RUN /opt/scripts/bin/download-interproscan.sh

ENTRYPOINT ["/opt/scripts/bin/unifire-workflow.sh"]