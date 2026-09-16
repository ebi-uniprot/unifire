process runPirsrPipeline {
    label "time_verylong"
    label "mem_high"

    container "${unifireImage}:${unifireVersion}"
    publishDir { "${outputDir}" }, mode: 'copy'

    input:
    val chunkSize
    path iprscanXmlFilePath
    path pirsrUrmlXmlFilePath
    path pirsrDir
    val fileName
    val inputType
    val outputFormat
    val unifireImage
    val unifireVersion
    val pirsrMemory
    val outputDir

    output:
    path "${fileName}"

    script:
    def memoryOpt = pirsrMemory ? "-m ${pirsrMemory}" : ""
    """
    mkdir -p pirsr-pred
    echo "Running PIRSR hmmalign..."
    /opt/code/distribution/bin/pirsr.sh -i ${iprscanXmlFilePath} -o pirsr-pred -a /usr/bin/hmmalign -d ${pirsrDir}/pirsr_data -t ${inputType}
    mv pirsr-pred/*.xml pirsr-pred.xml

    echo "Running rules inference on PIRSR..."
    /opt/code/distribution/bin/unifire.sh -n ${chunkSize} -r ${pirsrUrmlXmlFilePath} -i pirsr-pred.xml -s XML -t ${pirsrDir}/pirsr_data/PIRSR_templates.xml -o ${fileName} -f ${outputFormat} ${memoryOpt}

    ls -lah
    """
}
