process runUnifirePipeline {
    label "time_verylong"
    label "mem_veryhigh"

    container "${unifireImage}:${unifireVersion}"
    publishDir { "${outputDir}" }, mode: 'copy'

    input:
    val chunkSize
    path urmlRulesXmlFilePath
    path iprscanXmlFilePath
    path urmlTemplatesXmlFilePath
    val fileName
    val inputType
    val outputFormat
    val unifireImage
    val unifireVersion
    val unifireMemory
    val outputDir

    output:
    path "${fileName}"

    script:
    def memoryOpt = unifireMemory ? "-m ${unifireMemory}" : ""
    """
    /opt/code/distribution/bin/unifire.sh -n ${chunkSize} -r ${urmlRulesXmlFilePath} -i ${iprscanXmlFilePath} -t ${urmlTemplatesXmlFilePath} -s ${inputType} -o ${fileName} -f ${outputFormat} ${memoryOpt}
    """
}
