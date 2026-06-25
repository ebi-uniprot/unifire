process runPirsrPipeline {
    container "unifire/nextflow:${params.unifireVersion}"

    input:
    val chunkSize
    path urmlRulesXmlFilePath
    path iprscanXmlFilePath
    path urmlTemplatesXmlFilePath
    path pirsrUrmlRuleFilePath
    path pirsrDir
    path outputDirPath
    val fileName
    val inputType

    output:
    path "${outputDirPath}/${fileName}"

    script:
    """
    echo "Running PIRSR hmmalign..."
    /opt/code/distribution/bin/pirsr.sh -i ${iprscanXmlFilePath} -o ${outputDirPath}/${fileName} -a /usr/bin/hmmalign -d ${pirsrDir}

    ls -lah
    """
}
