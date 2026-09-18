include { printUsage } from './nextflow/modules/help'
include { getDefaultParams } from './nextflow/defaults.nf'
include { resolveDataVersions } from './nextflow/versions.nf'
include { UNIFIRE } from './nextflow/unifire.nf'

workflow {
    if (params.help) {
        def defParams = getDefaultParams()
        printUsage(
            run: defParams.run,
            data: defParams.data,
            engine: defParams.engine
        )
        exit(0)
    }

    // Resolve the data versions. Source precedence: explicit --dataVersions file,
    // else GitHub master merged with the bundled nextflow/versions.json, else the
    // bundled file alone. Individual releases can still be overridden via
    // explicit CLI arguments, which always win over the resolved values.
    def versionConfig = resolveDataVersions(params.version?.toString(), params.dataVersions)

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
