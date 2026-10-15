process RUN_UNIFIRE_PIPELINE {
    label 'unifire_container'
    publishDir { "${outputDir}" }, mode: 'copy'

    input:
    val chunkSize
    path urmlRulesXmlFilePath
    path iprscanXmlFilePath
    path urmlTemplatesXmlFilePath
    val fileName
    val inputType
    val outputFormat
    val outputDir

    output:
    path "${fileName}"

    script:
    def templatesOpt = urmlTemplatesXmlFilePath ? "-t ${urmlTemplatesXmlFilePath}" : ""
    """
    /opt/code/distribution/bin/unifire.sh -n ${chunkSize} -r ${urmlRulesXmlFilePath} -i ${iprscanXmlFilePath} ${templatesOpt} -s ${inputType} -o ${fileName} -f ${outputFormat}
    """

    stub:
    """
    touch ${fileName}
    """
}
