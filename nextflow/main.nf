include { fetchData } from './data.nf'
include { generateTaxonomyLineage } from './modules/taxonomy'
include { runUnifirePipeline as runUrmlPipeline } from './modules/unifire'
include { runUnifirePipeline as runArbaPipeline } from './modules/unifire'
include { runIprscan6 } from './modules/interproscan6'

workflow {
    println("Using UniProt release: ${params.uniprotRelease}")

    // Fetch required data
    dataPaths = fetchData(params.dataPath)

    // Define pipeline inputs
    def inputPath = file(params.input)
    def iprscanXmlPath = inputPath
    def pirsrDir = dataPaths.pirsrDir
    def pirsrTemplatesXmlPath = dataPaths.pirsrUrmlFilePath

    def outputDir = file(params.output)
    if (outputDir.isFile()) {
        log.error("'--output <DATA-DIR>' is required and cannot be an existing file.")
        exit(1)
    }
    else if (!outputDir.isDirectory()) {
        assert outputDir.mkdirs()
    }

    def inputType = params.inputType ?: inferInputType(params.input)

    if (inputType == "fasta") {
        // Run InterProScan 6 pipeline
        def iprDataPath = dataPaths.dataPath.resolve("iprscan6")
        assert iprDataPath.mkdirs()
        iprscanXmlPath = runIprscan6(params.iprscanVersion, params.iprVersion, inputPath, iprDataPath)
        inputType = "InterProScan6"
    }

    println("Running inference on input type: ${inputType}")

    // Run taxonomy lineage script
    def taxonomyLineageXmlPath = generateTaxonomyLineage(iprscanXmlPath)

    if (params.useUnirule != "false") {
        runUrmlPipeline(params.chunkSize, dataPaths.uniruleUrmlFilePath, taxonomyLineageXmlPath, dataPaths.urmlTemplatesFilePath, outputDir, "predictions_unirule.out", inputType)
    }

    if (params.useArba != "false") {
        runArbaPipeline(params.chunkSize, dataPaths.arbaUrmlFilePath, taxonomyLineageXmlPath, dataPaths.urmlTemplatesFilePath, outputDir, "predictions_arba.out", inputType)
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
