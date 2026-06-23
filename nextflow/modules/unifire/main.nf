process runUnifirePipeline {
    container "unifire/unifire-pipeline:$params.unifireVersion"

    input:
    path urmlRulesXmlPath
    path iprscanXmlPath
    path urmlTemplatesXmlPath
    path outputDirPath
    val fileName
    val inputType

    script:
    """
    /opt/code/run.sh -r $urmlRulesXmlPath -i $iprscanXmlPath -t $urmlTemplatesXmlPath -s $inputType -o $outputDirPath/$fileName
    """
}