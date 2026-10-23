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
            dataVersions: null,
            forceDownloads: false,
            uniprotRelease: null,
            pirsrRelease: null,
            iprscanVersion: null,
            iprVersion: null,
            iprscanApplications: 'HAMAP,PROSITE-profiles,PROSITE-patterns,Pfam,NCBIFAM,SMART,PRINTS,SFLD,CDD,CATH-Gene3D,PIRSF,PANTHER,SUPERFAMILY,CATH-FunFam'
        ],
        engine: [
            iprscan6ProfileNames: []
        ]
    ]
}
