include { fetchData } from './data.nf'
include { runIprscan6 } from './modules/interproscan6'
include { generateTaxonomyLineage } from './modules/taxonomy'
include { runUnifirePipeline as runUnirulePipeline } from './modules/unifire'
include { runUnifirePipeline as runArbaPipeline } from './modules/unifire'
include { runPirsrPipeline } from './modules/pirsr'

workflow {
    // Fetch required data
    def systems = params.systems.tokenize(",")
    println("Using UniProt release: ${params.uniprotRelease}, systems: ${systems}")
    dataPaths = fetchData(params.dataPath, systems)

    // Define pipeline inputs
    def inputPath = file(params.input)
    def iprscanXmlPath = inputPath

    def outputDir = file(params.output)
    if (outputDir.isFile()) {
        log.error("'--output <DATA-DIR>' is required and cannot be an existing file.")
        exit(1)
    }
    else if (!outputDir.isDirectory()) {
        assert outputDir.mkdirs()
    }

    def inputType = params.inputType ?: inferInputType(params.input)
    println("Inferred input type: ${inputType}")

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

    if ("unirule" in systems) {
        runUnirulePipeline(params.chunkSize, dataPaths.uniruleUrmlFilePath, taxonomyLineageXmlPath, dataPaths.urmlTemplatesFilePath, outputDir, "predictions_unirule.out", inputType)
    }

    if ("arba" in systems) {
        runArbaPipeline(params.chunkSize, dataPaths.arbaUrmlFilePath, taxonomyLineageXmlPath, dataPaths.urmlTemplatesFilePath, outputDir, "predictions_arba.out", inputType)
    }

    if ("pirsr" in systems) {
        runPirsrPipeline(params.chunkSize, taxonomyLineageXmlPath, dataPaths.pirsrUrmlFilePath, dataPaths.pirsrDir, outputDir, "predictions_pirsr.out", inputType)
    }
}

def inferInputType(inputFile) {
    def inferredType = null
    def lowerName = inputFile.toLowerCase()

    if (lowerName.endsWith('.fasta')) {
        inferredType = 'fasta'
    } else if (lowerName.endsWith('.xml')) {
        def file = file(inputFile)
        def content = file.text
        def matcher = content =~ /<(\w+)/
        def rootElement = matcher ? matcher[0][1] : null

        if (rootElement == 'protein-matches') {
            inferredType = 'InterProScan'
        } else if (rootElement == 'results') {
            inferredType = 'InterProScan6'
        }
    }

    return inferredType
}