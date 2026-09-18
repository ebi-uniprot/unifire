# Developer guide

For the architecture of the Nextflow pipeline (params boundary, subworkflows, configuration), see
[Nextflow architecture](architecture.md).

## Fact Model

The fact model is automatically created from the following XML Schema: [urml-facts.xsd](../core/src/main/resources/schemas/xsd/urml-facts.xsd)

The corresponding Java classes are built in the [core](../core) module under the package `org.uniprot.urml.facts`, after building the project with Maven (or using the `./build.sh` script).

You can use these classes (described in the UML diagram below) and the container `org.uniprot.urml.facts.FactSet` to load your own data directly into objects (via an ORM or a custom parser).

### Fact Model Diagram

![fact-model-diagram](../misc/media/fact-model.png)

## Rule Model

The rule model is described in another XML Schema available here: [urml-rules.xsd](../core/src/main/resources/schemas/xsd/urml-rules.xsd)

The corresponding Java classes are built in the [core](../core) module under the package `org.uniprot.urml.rules`.

## Building the software

### Requirements

- Maven (>= 3.0)
- Java 17

Please make sure your JAVA_HOME environment variable points to the root folder of your JDK 17 installation, e.g.

```
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

### Command

```bash
./build.sh
```

This script is installing some libraries in your local maven repository (temporary solution before publishing the artifacts on a public repository).

Then it runs `mvn clean install`, assembling all the libs and the execution script under `./distribution/target/unifire-distribution/`.

To use the newly built distribution, you will need to run: `./distribution/target/unifire-distribution/bin/unifire.sh` instead.

### Execution

The execution of the rules relies on [Drools](https://www.drools.org), an open-source rule-based technology developed by RedHat.
It is using an optimized version of the [Rete algorithm](https://en.wikipedia.org/wiki/Rete_algorithm) to match facts and rules in a scalable way.
This tool translates the URML rules into the Drools language, converts the input data according to the rule model and execute all the rules to produce a list of protein annotations.

## Limitations

### Memory

A minimum of 24 GB of memory is recommended for this software to run. By default, the JVM max heap space is configured to use 75% of the available memory.
For a large number of protein to process, it is advised to split them into chunks of approx. 500 proteins per rule evaluation to keep the memory usage low.
This is automatically handled by the `-n / --chunksize` option of UniFIRE (by default 500).
In case you face OOM heap space memory errors, try to either use a smaller chunksize (-n option) or manually split the input file into smaller chunks.

### Java 9 / 10 issues

- For users, the software will be functional under Java 9 or 10, but you will get some warning messages complaining about illegal reflective accesses. You can simply ignore them at the moment.
- For developers, building the software with JDK 9 / 10 is currently not possible because of JAXB Maven plugin issues. Cf:

    * https://github.com/highsource/maven-jaxb2-plugin/issues/120
    * https://stackoverflow.com/questions/49717155/failed-to-run-custom-xjc-extension-within-cxf-xjc-plugin-on-java-9

    Those issues have been raised very recently. The fixes will be applied as soon as there are available.
