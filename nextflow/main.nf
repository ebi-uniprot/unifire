include { fetchData } from './data.nf'
include { generateTaxonomyLineage } from './modules/taxonomy'
include { runUnifirePipeline as runUrmlPipeline } from './modules/unifire'
include { runUnifirePipeline as runArbaPipeline } from './modules/unifire'


workflow {
    println("Using UniProt release: ${params.uniprotRelease}")

    // Fetch required data
    dataPaths = fetchData(params.dataPath)

    // Define pipeline inputs
    def iprscanXmlPath = file(params.input)
    def dataPath = dataPaths.dataPath
    def urmlRulesXmlPath = dataPaths.urmlFilePath
    def urmlTemplatesXmlPath = dataPaths.urmlTemplatesFilePath
    def arbaRulesXmlPath = dataPaths.arbaFilePath
    def pirsrDir = dataPaths.pirsrDir
    def pirsrTemplatesXmlPath = dataPaths.pirsrUrmlFilePath

    def outputDir = file(params.output)
    if (outputDir.isFile()) {
        log.error("'--output <DATA-DIR>' is required and cannot be an existing file.")
        exit(1)
    }
    else if (!outputDir.isDirectory()) {
        outputDir.mkdirs()
    }

    def inputType = params.inputType ?: inferInputType(params.input)

    if (inputType == "fasta") {
        // Run InterProScan 6 pipeline
        // TODO set iprscanFilePath to the new artifact path
        inputType = "InterProScan6"
    }

    println("Running inference on input type: ${inputType}")

    // Run taxonomy lineage script
    def taxonomyLineageXmlPath = generateTaxonomyLineage(iprscanXmlPath)

    if (params.useUrml) {
        def outPath = runUrmlPipeline(dataPath, urmlRulesXmlPath, taxonomyLineageXmlPath, urmlTemplatesXmlPath, outputDir, "predictions_unirule.out", inputType)
        println("URML finished: ${outPath}")
    }

    if (params.useArba) {
        def outPath = runArbaPipeline(dataPath, arbaRulesXmlPath, taxonomyLineageXmlPath, urmlTemplatesXmlPath, outputDir, "predictions_arba.out", inputType)
        println("ARBA finished: ${outPath}")
    }

    if (params.usePirsr) {
        // TODO run pirsr
        def pirsr = ""
    }
}

process inferInputType {
    input:
    path fileName

    output:
    stdout

    script:
    """
    if [[ "${fileName}" == *.fasta ]]; then
        echo "fasta"
    elif [[ "${fileName}" == *.xml ]]; then
        root_element=\$(grep -oP '(?<=<)[^>\\s/]+' "${fileName}" | head -1)
        if [[ "\$root_element" == "protein-matches" ]]; then
            echo "InterProScan"
        elif [[ "\$root_element" == "results" ]]; then
            echo "InterProScan6"
        fi
    fi
    """
}
