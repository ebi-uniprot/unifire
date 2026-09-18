include { printUsage } from './nextflow/modules/help'
include { getDefaultParams } from './nextflow/defaults.nf'
include { getDefaultVersions } from './nextflow/versions.nf'
include { UNIFIRE } from './nextflow/unifire.nf'

workflow {
    if (params.help) {
        def defParams = getDefaultParams()
        printUsage(
            run: defParams.run,
            data: defParams.data,
            engine: defParams.engine,
            versions: getDefaultVersions()
        )
        exit(0)
    }

    // Resolve versioned defaults.
    // Precedence: explicit CLI arguments > --version mapping > defaultKey mapping.
    def versionTree = getDefaultVersions()
    def defaultConfig = versionTree.versions[versionTree.defaultKey]
    if (!defaultConfig) {
        log.error("Version '${versionTree.defaultKey}' not found in default versions.")
        exit(1)
    }
    def versionConfig = params.version ? versionTree.versions[params.version.toString()] : defaultConfig
    if (params.version && !versionConfig) {
        log.error("Version '${params.version}' not found. Available versions: ${versionTree.versions.keySet().join(', ')}")
        exit(1)
    }

    UNIFIRE(
        [
            input: params.input,
            outputDir: params.output,
            inputType: params.inputType,
            systems: params.systems,
            outputFormat: params.outputFormat,
            chunkSize: params.chunkSize
        ],
        [
            dataPath: params.dataPath ? file(params.dataPath) : null,
            forceDownloads: params.forceDownloads,
            uniprotRelease: params.uniprotRelease ?: versionConfig.uniprotRelease,
            pirsrRelease: params.pirsrRelease ?: versionConfig.pirsrRelease,
            iprscanVersion: params.iprscanVersion ?: versionConfig.iprscanVersion,
            iprVersion: params.iprVersion ?: versionConfig.iprVersion,
            iprscanApplications: params.iprscanApplications
        ],
        [
            unifireImage: params.unifireImage,
            unifireVersion: params.unifireVersion,
            unifireMemory: params.unifireMemory ?: '',
            pirsrMemory: params.pirsrMemory ?: '',
            iprscan6ProfileNames: params.iprscan6ProfileNames
        ]
    )
}
