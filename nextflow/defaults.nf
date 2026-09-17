def getDefaultParams() {
    return [
        run: [
            input: null,
            outputDir: null,
            inputType: null,
            systems: 'unirule,arba,pirsr',
            outputFormat: 'TSV',
            chunkSize: 500
        ],
        data: [
            dataPath: '.unifire/data',
            forceDownloads: false,
            uniprotRelease: null,
            pirsrRelease: null,
            iprscanVersion: null,
            iprVersion: null
        ],
        engine: [
            unifireImage: 'ghcr.io/ebi-uniprot/unifire/nextflow',
            unifireVersion: '3.1.0-SNAPSHOT',
            unifireMemory: '',
            pirsrMemory: '',
            iprscan6ProfileNames: []
        ]
    ]
}
