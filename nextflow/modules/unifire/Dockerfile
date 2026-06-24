FROM maven:3.8.3-openjdk-17

ADD core /opt/code/core
ADD distribution /opt/code/distribution
ADD engine /opt/code/engine
ADD io /opt/code/io
ADD procedures /opt/code/procedures
ADD pom.xml /opt/code/pom.xml

RUN cd /opt/code && mvn clean dependency:copy-dependencies package -Dmaven.test.skip=true -Dmaven.source.skip=true

ADD nextflow/modules/unifire/run.sh /opt/code/run.sh
RUN chmod 744 /opt/code/run.sh
ENV PATH=$PATH:/opt/code
CMD ["/opt/code/run.sh"]