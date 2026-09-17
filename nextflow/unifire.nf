include { fetchData } from './data.nf'
include { runIprscan6 } from './modules/interproscan6'
include { generateTaxonomyLineage } from './modules/taxonomy'
include { runUnifirePipeline as runUnirulePipeline } from './modules/unifire'
include { runUnifirePipeline as runArbaPipeline } from './modules/unifire'
include { runPirsrPipeline } from './modules/pirsr'

include { getDefaultParams } from './defaults.nf'

workflow UNIFIRE {
    take:
    run       // map: input, outputDir, inputType, systems, outputFormat, chunkSize
    data      // map: dataPath, forceDownloads, uniprotRelease, pirsrRelease, iprscanVersion, iprVersion
    engine    // map: unifireImage, unifireVersion, unifireMemory, pirsrMemory, iprscan6ProfileNames

    main:
    def params = getDefaultParams()
    // Defaults per group; allowed keys come from the full default key sets so
    // optional fields (e.g. input/outputDir/inputType, null in defaults) validate.
    def defRun = [systems: params.run.systems, outputFormat: params.run.outputFormat, chunkSize: params.run.chunkSize]
    def defData = [dataPath: params.data.dataPath, forceDownloads: params.data.forceDownloads, uniprotRelease: null, pirsrRelease: null, iprscanVersion: null, iprVersion: null]
    def defEngine = [unifireImage: params.engine.unifireImage, unifireVersion: params.engine.unifireVersion, unifireMemory: '', pirsrMemory: '', iprscan6ProfileNames: params.engine.iprscan6ProfileNames]
    def cfgRun = mergeStrict(run, defRun, params.run.keySet(), 'run')
    def cfgData = mergeStrict(data, defData, params.data.keySet(), 'data')
    def cfgEngine = mergeStrict(engine, defEngine, params.engine.keySet(), 'engine')
    // An empty profile list (no -profile selected) falls back to the default.
    if (!cfgEngine.iprscan6ProfileNames) {
        cfgEngine.iprscan6ProfileNames = ['standard']
    }

    printBanner()

    def parsedSystems = parseSystems(cfgRun.systems)
    def parsedOutputFormat = parseOutputFormat(cfgRun.outputFormat)
    def parsedChunkSize = parseChunkSize(cfgRun.chunkSize)

    // Validate run options before any data fetching, so bad or incomplete
    // inputs fail fast.
    def inputPath = cfgRun.input ? file(cfgRun.input) : null
    if (!inputPath) {
        log.error("'--input <FILE>' is required: path to a multi-FASTA or InterProScan XML file.")
        exit(1)
    }
    else if (!inputPath.exists()) {
        log.error("Input file does not exist: ${cfgRun.input}")
        exit(1)
    }
    else if (!inputPath.isFile()) {
        log.error("Input path must be a regular file, not a directory: ${cfgRun.input}")
        exit(1)
    }

    def outputDir = (cfgRun.outputDir != null) ? file(cfgRun.outputDir) : null
    if (!outputDir) {
        log.error("'--output <DATA-DIR>' is required: output directory for prediction files.")
        exit(1)
    }
    else if (outputDir.isFile()) {
        log.error("'--output <DATA-DIR>' is required and cannot be an existing file.")
        exit(1)
    }
    else if (!outputDir.isDirectory()) {
        assert outputDir.mkdirs()
    }

    def resolvedInputType = cfgRun.inputType ? parseInputType(cfgRun.inputType) : inferInputType(inputPath)
    if (!resolvedInputType) {
        log.error("Could not infer input type from '${inputPath}'. '--input <FILE>' must be a multi-FASTA file (.fasta, .fa) or an InterProScan XML file (root element 'protein-matches' or 'results'), or specify '--inputType' explicitly (fasta, InterProScan, InterProScan6).")
        exit(1)
    }
    println("Inferred input type: ${resolvedInputType}")

    println("Using UniProt release: ${cfgData.uniprotRelease}, systems: ${parsedSystems}")

    dataPaths = fetchData(
        cfgData.dataPath,
        parsedSystems,
        cfgData.uniprotRelease,
        cfgData.pirsrRelease,
        cfgData.forceDownloads,
        cfgEngine.unifireImage,
        cfgEngine.unifireVersion
    )

    def iprscanXmlPath = inputPath

    if (resolvedInputType == "fasta") {
        // Run InterProScan 6 pipeline
        println("Running InterProScan 6 pipeline with iprscanVersion=${cfgData.iprscanVersion}, iprVersion=${cfgData.iprVersion}, profiles=${cfgEngine.iprscan6ProfileNames}")
        def iprDataPath = dataPaths.dataPath.resolve("iprscan6")
        assert iprDataPath.mkdirs()
        iprscanXmlPath = runIprscan6(cfgData.iprscanVersion, cfgData.iprVersion, inputPath, iprDataPath, cfgEngine.iprscan6ProfileNames)
        resolvedInputType = "InterProScan6"
    }

    println("Running inference on input type: ${resolvedInputType}")

    // Run taxonomy lineage script
    def taxonomyLineageXmlPath = generateTaxonomyLineage(
        iprscanXmlPath,
        dataPaths.taxaFilePath,
        cfgEngine.unifireImage,
        cfgEngine.unifireVersion
    )

    if ("unirule" in parsedSystems) {
        runUnirulePipeline(
            parsedChunkSize,
            dataPaths.uniruleUrmlFilePath,
            taxonomyLineageXmlPath,
            dataPaths.urmlTemplatesFilePath,
            "predictions_unirule.out",
            resolvedInputType,
            parsedOutputFormat,
            cfgEngine.unifireImage,
            cfgEngine.unifireVersion,
            cfgEngine.unifireMemory,
            outputDir
        )
    }

    if ("arba" in parsedSystems) {
        runArbaPipeline(
            parsedChunkSize,
            dataPaths.arbaUrmlFilePath,
            taxonomyLineageXmlPath,
            dataPaths.urmlTemplatesFilePath,
            "predictions_arba.out",
            resolvedInputType,
            parsedOutputFormat,
            cfgEngine.unifireImage,
            cfgEngine.unifireVersion,
            cfgEngine.unifireMemory,
            outputDir
        )
    }

    if ("pirsr" in parsedSystems) {
        runPirsrPipeline(
            parsedChunkSize,
            taxonomyLineageXmlPath,
            dataPaths.pirsrUrmlFilePath,
            dataPaths.pirsrDir,
            "predictions_unirule-pirsr.out",
            resolvedInputType,
            parsedOutputFormat,
            cfgEngine.unifireImage,
            cfgEngine.unifireVersion,
            cfgEngine.pirsrMemory,
            outputDir
        )
    }
}

def mergeStrict(userMap, defaultMap, allowedKeys, groupName) {
    def unknownKeys = userMap.keySet() - allowedKeys
    if (unknownKeys) {
        log.error("Unknown ${groupName} option(s): ${unknownKeys.sort().join(', ')}. Valid options: ${allowedKeys.sort().join(', ')}")
        exit(1)
    }
    // Unset options (null) fall back to defaults instead of overriding them.
    def provided = userMap.findAll { it.value != null }
    return defaultMap + provided
}

def printBanner() {
    log.info(
        """
    UniFIRE - UniProt Functional-Annotation Inference Rule Engine
    Copyright (c) 2026 European Molecular Biology Laboratory
    """.stripIndent()
    )
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
    def chunkSize = chunkSizeParam instanceof String ? chunkSizeParam as Integer : chunkSizeParam
    if (chunkSize <= 0) {
        log.error("Invalid chunk size: ${chunkSize}. '--chunkSize' must be greater than 0.")
        exit(1)
    }
    return chunkSize
}

def parseOutputFormat(outputFormatParam) {
    def validFormats = ['TSV', 'XML']
    if (!(outputFormatParam in validFormats)) {
        log.error("Invalid output format: ${outputFormatParam}. '--outputFormat' must be one of: ${validFormats.join(', ')}")
        exit(1)
    }
    return outputFormatParam
}

def inferInputType(inputPath) {
    def inferredType = null
    def lowerName = inputPath.toString().toLowerCase()

    if (lowerName.endsWith('.fasta') || lowerName.endsWith('.fa')) {
        inferredType = 'fasta'
    }
    else if (lowerName.endsWith('.xml')) {
        def content = inputPath.text
        // Remove XML declaration and comments, then find the first root-like element
        def cleaned = content
            .replaceAll(/<\?xml[^?]*\?>/, '')
            .replaceAll(/<!--[\s\S]*?-->/, '')
            .trim()
        def matcher = cleaned =~ /<([\w-]+)/
        def rootElement = matcher ? matcher[0][1] : null

        if (rootElement == 'protein-matches') {
            inferredType = 'InterProScan'
        }
        else if (rootElement == 'results') {
            inferredType = 'InterProScan6'
        }
    }

    return inferredType
}
