process runUnifirePipeline {
    container "unifire/unifire-pipeline:${params.unifireVersion}"

    input:
    val chunkSize
    path urmlRulesXmlFilePath
    path iprscanXmlFilePath
    path urmlTemplatesXmlFilePath
    path outputDirPath
    val fileName
    val inputType

    output:
    path "${outputDirPath}/${fileName}"

    script:
    """
    MIN_HEAP_MEM_OPTION="-Xms4G"
    # default max heap size 75% of available RAM
    MAX_HEAP_MEM_OPTION="-XX:MaxRAMPercentage=75"

    java \
      --add-opens java.desktop/java.awt.font=ALL-UNNAMED \
      --add-opens java.base/java.text=ALL-UNNAMED \
      --add-opens java.base/java.util=ALL-UNNAMED \
      --add-opens java.base/java.lang=ALL-UNNAMED \
      --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
      -XshowSettings:vm "\${MIN_HEAP_MEM_OPTION}" "\${MAX_HEAP_MEM_OPTION}" \
      -cp /opt/code/distribution/target/*:/opt/code/distribution/target/dependency/* \
      uk.ac.ebi.uniprot.unifire.UniFireApp \
      -n ${chunkSize} -r ${urmlRulesXmlFilePath} -i ${iprscanXmlFilePath} -t ${urmlTemplatesXmlFilePath} -s ${inputType} -o ${outputDirPath}/${fileName}
    """
}
