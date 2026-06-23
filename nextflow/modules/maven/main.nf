process buildMaven {
    container 'maven:latest'

    input:
    path sourceDir

    output:
    path "artifacts"

    script:
    """
    mkdir -p tmpwdir
    mkdir -p artifacts
    ARTIFACTS_DIR=`realpath artifacts`

    for folder in core distribution engine io procedures pom.xml;
    do
        cp -r ${sourceDir}/\$folder tmpwdir/
    done

    cd tmpwdir
    mvn clean dependency:copy-dependencies package -Dmaven.test.skip=true -Dmaven.source.skip=true
    cp -r distribution/target \$ARTIFACTS_DIR
    """
}
