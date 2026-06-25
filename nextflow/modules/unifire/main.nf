process runUnifirePipeline {
    container "unifire/nextflow:${params.unifireVersion}"
    publishDir "${params.output}", mode: 'copy'

    input:
    val chunkSize
    path urmlRulesXmlFilePath
    path iprscanXmlFilePath
    path urmlTemplatesXmlFilePath
    val fileName
    val inputType

    output:
    path "${fileName}"

    script:
    def memoryOpt = params.unifireMemory ? "-m ${params.unifireMemory}" : ""
    """
    /opt/code/distribution/bin/unifire.sh -n ${chunkSize} -r ${urmlRulesXmlFilePath} -i ${iprscanXmlFilePath} -t ${urmlTemplatesXmlFilePath} -s ${inputType} -o ${fileName} ${memoryOpt}
    """
}
