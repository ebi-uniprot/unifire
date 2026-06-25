process runPirsrPipeline {
    container "unifire/nextflow:${params.unifireVersion}"

    input:
    val chunkSize
    path iprscanXmlFilePath
    path pirsrUrmlXmlFilePath
    path pirsrDir
    path outputDirPath
    val fileName
    val inputType

    output:
    path "${outputDirPath}/${fileName}"

    script:
    """
    mkdir -p pirsr-pred
    echo "Running PIRSR hmmalign..."
    /opt/code/distribution/bin/pirsr.sh -i ${iprscanXmlFilePath} -o pirsr-pred -a /usr/bin/hmmalign -d ${pirsrDir}/pirsr_data -t ${inputType}
    mv pirsr-pred/*.xml pirsr-pred.xml

    echo "Running rules inference on PIRSR..."
    /opt/code/distribution/bin/unifire.sh -n ${chunkSize} -r ${pirsrUrmlXmlFilePath} -i pirsr-pred.xml -s XML -t ${pirsrDir}/pirsr_data/PIRSR_templates.xml -o ${outputDirPath}/${fileName}

    ls -lah
    """
}
