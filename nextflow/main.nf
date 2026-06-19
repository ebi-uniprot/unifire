include { fetchData } from './data.nf'
include { runIprscan6 } from './modules/interproscan6'
include { generateTaxonomyLineage } from './modules/taxonomy'
include { runUnifirePipeline as runUnirulePipeline } from './modules/unifire'
include { runUnifirePipeline as runArbaPipeline } from './modules/unifire'
include { runPirsrPipeline } from './modules/pirsr'
include { printUsage } from './modules/help'

workflow {
    if (params.help) {
        printUsage()
        exit(0)
    }

    printBanner()

    def systems = parseSystems(params.systems)
    validateIprscan6ProfileName(params.iprscan6ProfileName)
    def outputFormat = parseOutputFormat(params.outputFormat)
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

    def inputType = params.inputType ? parseInputType(params.inputType) : inferInputType(params.input)
    println("Inferred input type: ${inputType}")

    def chunkSize = parseChunkSize(params.chunkSize)

    if (inputType == "fasta") {
        // Run InterProScan 6 pipeline
        println("Running InterProScan 6 pipeline with iprscanVersion=${params.iprscanVersion}, iprVersion=${params.iprVersion}")
        def iprDataPath = dataPaths.dataPath.resolve("iprscan6")
        assert iprDataPath.mkdirs()
        iprscanXmlPath = runIprscan6(params.iprscanVersion, params.iprVersion, inputPath, iprDataPath)
        inputType = "InterProScan6"
    }

    println("Running inference on input type: ${inputType}")

    // Run taxonomy lineage script
    def taxonomyLineageXmlPath = generateTaxonomyLineage(iprscanXmlPath)

    if ("unirule" in systems) {
        runUnirulePipeline(chunkSize, dataPaths.uniruleUrmlFilePath, taxonomyLineageXmlPath, dataPaths.urmlTemplatesFilePath, "predictions_unirule.out", inputType, outputFormat)
    }

    if ("arba" in systems) {
        runArbaPipeline(chunkSize, dataPaths.arbaUrmlFilePath, taxonomyLineageXmlPath, dataPaths.urmlTemplatesFilePath, "predictions_arba.out", inputType, outputFormat)
    }

    if ("pirsr" in systems) {
        runPirsrPipeline(chunkSize, taxonomyLineageXmlPath, dataPaths.pirsrUrmlFilePath, dataPaths.pirsrDir, "predictions_unirule-pirsr.out", inputType, outputFormat)
    }
}

def printBanner() {
    log.info """
    UniFIRE - UniProt Functional-Annotation Inference Rule Engine
    Copyright (c) 2026 European Molecular Biology Laboratory
    """.stripIndent()
}

def parseSystems(systemsParam) {
    def validSystems = ['unirule', 'arba', 'pirsr']
    def systems = systemsParam.tokenize(",")
    def invalidSystems = systems - validSystems
    if (invalidSystems) {
        log.error("Invalid system(s): ${invalidSystems}. '--systems' must be a comma-separated list of: ${validSystems.join(', ')}")
        exit(1)
    }
    return systems
}

def parseInputType(inputTypeParam) {
    def validInputTypes = ['fasta', 'InterProScan', 'InterProScan6']
    if (!(inputTypeParam in validInputTypes)) {
        log.error("Invalid input type: ${inputTypeParam}. '--inputType' must be one of: ${validInputTypes.join(', ')}")
        exit(1)
    }
    return inputTypeParam
}

def parseChunkSize(chunkSizeParam) {
    if (chunkSizeParam <= 0) {
        log.error("Invalid chunk size: ${chunkSizeParam}. '--chunkSize' must be greater than 0.")
        exit(1)
    }
    return chunkSizeParam
}

def validateIprscan6ProfileName(profileName) {
    def validProfiles = ['docker', 'singularity', 'podman']
    if (!(profileName in validProfiles)) {
        log.error("Invalid iprscan6ProfileName: ${profileName}. Must be one of: ${validProfiles.join(', ')}")
        exit(1)
    }
}

def parseOutputFormat(outputFormatParam) {
    def validFormats = ['TSV', 'XML']
    if (!(outputFormatParam in validFormats)) {
        log.error("Invalid output format: ${outputFormatParam}. '--outputFormat' must be one of: ${validFormats.join(', ')}")
        exit(1)
    }
    return outputFormatParam
}

def inferInputType(inputFile) {
    def inferredType = null
    def lowerName = inputFile.toLowerCase()

    if (lowerName.endsWith('.fasta') || lowerName.endsWith('.fa')) {
        inferredType = 'fasta'
    } else if (lowerName.endsWith('.xml')) {
        def file = file(inputFile)
        def content = file.text
        // Remove XML declaration and comments, then find the first root-like element
        def cleaned = content.replaceAll(/<\?xml[^?]*\?>/, '')
                           .replaceAll(/<!--[\s\S]*?-->/, '')
                           .trim()
        def matcher = cleaned =~ /<([\w-]+)/
        def rootElement = matcher ? matcher[0][1] : null

        if (rootElement == 'protein-matches') {
            inferredType = 'InterProScan'
        } else if (rootElement == 'results') {
            inferredType = 'InterProScan6'
        }
    }

    return inferredType
}
