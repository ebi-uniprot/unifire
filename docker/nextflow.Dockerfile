FROM ubuntu:24.04

RUN apt-get update \
    && DEBIAN_FRONTEND="noninteractive" apt-get install -y wget openjdk-17-jdk maven git coreutils hmmer \
     python3-pip python3 ncbi-data libdw1 libpcre3-dev python3-dev gcc libc-dev \
    && pip3 install ete4 lxml --break-system-packages

ADD core /opt/code/core
ADD distribution /opt/code/distribution
ADD engine /opt/code/engine
ADD io /opt/code/io
ADD procedures /opt/code/procedures
ADD pom.xml /opt/code/pom.xml
ADD misc/taxonomy /opt/misc/taxonomy

RUN cd /opt/code && mvn clean dependency:copy-dependencies package -Dmaven.test.skip=true -Dmaven.source.skip=true

COPY docker/scripts /opt/scripts/bin
COPY docker/versions.properties /opt/scripts/bin
RUN chmod 775 /opt/scripts/bin/*.sh
RUN /opt/scripts/bin/update-taxonomy-cache.sh
RUN mkdir /volume

VOLUME /volume
ENV PATH=$PATH:/opt/code
