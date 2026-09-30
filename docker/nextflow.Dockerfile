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
#      container (profile "local") on files mounted at /volume, keeping the
#      legacy unifire image contract.
#
# Built as a multi-stage image to keep the runtime stage small: the Java
# build toolchain (JDK, Maven, compilers) only exists in the builder stage;
# the runtime stage ships a JRE plus the dependencies needed to execute the
# pipeline.
#
# Build:
#   docker build -f docker/nextflow.Dockerfile \
#       -t ghcr.io/ebi-uniprot/unifire/nextflow:<version> .

# Groovy runtime used by the nested interproscan6 Nextflow workflow
# (see https://github.com/ebi-pf-team/interproscan6/blob/main/docker/groovy.Dockerfile).
FROM groovy:4.0.27-jdk17 AS groovy-runtime

# Builder stage: compiles the UniFIRE engine and copies its runtime
# dependencies; nothing from this stage except /opt/code ends up in the
# final image. Python dependency packages (ete4, lxml) are installed here
# because ete4 has no valid wheel and needs a temporary compiler toolchain;
# the installed packages are copied into the runtime stage (same base
# image, therefore the same Python minor version and site-packages path).
FROM ubuntu:24.04 AS builder

RUN apt-get update \
    && DEBIAN_FRONTEND="noninteractive" apt-get install -y --no-install-recommends \
        openjdk-17-jdk maven git python3 python3-pip python3-dev gcc libc-dev \
    && rm -rf /var/lib/apt/lists/*

ENV UNIFIRE_DATA_PATH=/opt/unifire/data

# Python dependency packages (ete4, lxml) are built here because ete4 has no
# valid wheel and needs a compiler toolchain; they are copied into the
# runtime stage (same base image, therefore the same Python minor version
# and site-packages path).
RUN pip3 install --break-system-packages --no-cache-dir lxml ete4 \
    && python3 -c "import ete4, lxml"

# UniFIRE engine
ADD core /opt/code/core
ADD distribution /opt/code/distribution
ADD engine /opt/code/engine
ADD io /opt/code/io
ADD procedures /opt/code/procedures
ADD pom.xml /opt/code/pom.xml
ADD misc/taxonomy /opt/misc/taxonomy

RUN cd /opt/code && mvn clean dependency:copy-dependencies package -Dmaven.test.skip=true -Dmaven.source.skip=true

# Runtime stage: minimal toolset to run the UniFIRE Nextflow pipeline.
FROM ubuntu:24.04

RUN apt-get update \
    && DEBIAN_FRONTEND="noninteractive" apt-get install -y --no-install-recommends \
        wget openjdk-17-jre-headless git coreutils hmmer procps \
        python3 ncbi-data \
    && rm -rf /var/lib/apt/lists/*

# Python packages (ete4, lxml + dependencies), compiled in the builder stage
# (same base image, so the same python3.12 site-packages path applies).
COPY --from=builder /usr/local/lib/python3.12/dist-packages /usr/local/lib/python3.12/dist-packages

# Groovy 4.0.27 from the interproscan6 groovy image; Java 17 comes from apt.
COPY --from=groovy-runtime /opt/groovy /opt/groovy
# JAVA_HOME is not resolved automatically by a JRE-only image (groovy infers
# it from javac); point it at the platform-specific JRE location.
RUN ln -s /usr/lib/jvm/java-17-openjdk-$(dpkg --print-architecture) /opt/java
ENV JAVA_HOME=/opt/java
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

# UniFIRE engine sources and build outputs from the builder stage
COPY --from=builder /opt/code /opt/code
COPY --from=builder /opt/misc/taxonomy /opt/misc/taxonomy

# UniFIRE Nextflow pipeline
COPY main.nf /opt/unifire/main.nf
COPY nextflow /opt/unifire/nextflow

# Helper scripts (including the unifire-workflow.sh CMD entrypoint)
COPY docker/scripts /opt/scripts/bin
RUN chmod 775 /opt/scripts/bin/*.sh /opt/scripts/bin/*.py

ENV PATH=$PATH:/opt/code

RUN mkdir /volume
VOLUME /volume

WORKDIR /volume
CMD ["/opt/scripts/bin/unifire-workflow.sh"]
