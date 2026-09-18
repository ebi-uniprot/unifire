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

# UniFIRE Nextflow image. Serves two purposes:
#
#   1. Per-process container image for docker-profile runs of the pipeline
#      (see the `container` directives in nextflow/modules/). Nextflow task
#      commands replace the CMD below, so this image must NOT set an
#      ENTRYPOINT.
#   2. All-in-one runner of the UniFIRE Nextflow pipeline: a bare
#      `docker run` (no arguments) executes the pipeline locally inside the
#      container (profile "local", no docker-in-docker) on files mounted at
#      /volume, keeping the legacy unifire image contract.
#
# Build:
#   docker build -f docker/nextflow.Dockerfile \
#       -t ghcr.io/ebi-uniprot/unifire/nextflow:<version> .

# Groovy runtime used by the nested interproscan6 Nextflow workflow
# (see https://github.com/ebi-pf-team/interproscan6/blob/main/docker/groovy.Dockerfile).
FROM groovy:4.0.27-jdk17 AS groovy-runtime

FROM ubuntu:24.04

RUN apt-get update \
    && DEBIAN_FRONTEND="noninteractive" apt-get install -y wget openjdk-17-jdk maven git coreutils hmmer procps \
     python3-pip python3 ncbi-data libdw1 libpcre3-dev python3-dev gcc libc-dev \
    && pip3 install ete4 lxml --break-system-packages

# Groovy 4.0.27 from the interproscan6 groovy image; Java 17 comes from apt.
COPY --from=groovy-runtime /opt/groovy /opt/groovy
ENV GROOVY_HOME=/opt/groovy
ENV PATH="/opt/groovy/bin:${PATH}"

# Nextflow launcher plus a baked runtime distribution (so container runs do
# not need to download it on first use).
ENV NXF_HOME=/opt/nextflow
ENV NXF_ANSI_LOG=false
ENV NXF_DISABLE_CHECK_LATEST=true
RUN wget -qO- https://get.nextflow.io | bash \
    && mv nextflow /usr/local/bin/nextflow \
    && chmod 775 /usr/local/bin/nextflow \
    && nextflow -v

ENV UNIFIRE_DATA_PATH=/opt/unifire/data

# UniFIRE engine
ADD core /opt/code/core
ADD distribution /opt/code/distribution
ADD engine /opt/code/engine
ADD io /opt/code/io
ADD procedures /opt/code/procedures
ADD pom.xml /opt/code/pom.xml
ADD misc/taxonomy /opt/misc/taxonomy

RUN cd /opt/code && mvn clean dependency:copy-dependencies package -Dmaven.test.skip=true -Dmaven.source.skip=true

# UniFIRE Nextflow pipeline
COPY main.nf /opt/unifire/main.nf
COPY nextflow /opt/unifire/nextflow

# Helper scripts (including the run_unifire_nextflow.sh CMD entrypoint)
COPY docker/scripts /opt/scripts/bin
RUN chmod 775 /opt/scripts/bin/*.sh /opt/scripts/bin/*.py

ENV PATH=$PATH:/opt/code

RUN mkdir /volume
VOLUME /volume

WORKDIR /volume
CMD ["/opt/scripts/bin/run_unifire_nextflow.sh"]
