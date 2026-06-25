def printUsage() {
    log.info """
    UniFIRE - UniProt Functional-Annotation Inference Rule Engine (Nextflow)

    Usage:
      nextflow run nextflow/main.nf [options]

    Required options:
      --input FILE                Path to input file: multi-FASTA or InterProScan XML.
      --output DIR                Path to output directory for prediction files.
      --dataPath DIR              Path to a directory for downloaded rule data.

    Input options:
      --inputType TYPE            Input type: fasta, InterProScan, InterProScan6.
                                  If omitted, the type is inferred from the file extension and XML root element.

    Analysis options:
      --systems LIST              Comma-separated list of systems to run: unirule, arba, pirsr.
                                  Default: ${params.defaultSystems}
      --outputFormat FORMAT       Prediction output format: TSV or XML.
                                  Default: ${params.defaultOutputFormat}
      --chunkSize N               Number of proteins to process per chunk.
                                  Default: ${params.defaultChunkSize}

    InterProScan options (used only when input is FASTA):
      --iprscanVersion VERSION    InterProScan 6 version to run.
                                  Default: ${params.defaultIprscanVersion}
      --iprVersion VERSION        InterPro version to use with InterProScan 6.
                                  Default: ${params.defaultIprVersion}

    Data options:
      --uniprotRelease RELEASE    UniProt release used to download rule files.
                                  Default: ${params.defaultUniprotRelease}
      --skipDownloads             Skip downloading remote rule files.

    Resource options:
      --unifireMemory MB          Max heap memory for UniFIRE rule inference.
      --pirsrMemory MB            Max heap memory for PIRSR alignment.
      --maxWorkers N              Maximum number of parallel workers.

    Other options:
      -work-dir DIR               Nextflow working directory for intermediate files.
      --help                      Show this message and exit.

    Examples:
      nextflow run nextflow/main.nf --input samples/proteins.fasta --output out --dataPath data
      nextflow run nextflow/main.nf --input samples/input_ipr.xml --output out --dataPath data --skipDownloads
      nextflow run nextflow/main.nf --input samples/input_ipr6.xml --output out --dataPath data --systems unirule,arba
    """.stripIndent()
}
