def printUsage(opts) {
    def run = opts.run
    def data = opts.data
    def engine = opts.engine
    log.info """
    UniFIRE - UniProt Functional-Annotation Inference Rule Engine (Nextflow)

    Usage:
      nextflow run . [options]

    Required options:
      --input FILE                Path to input file: multi-FASTA or InterProScan XML.
      --output DIR                Path to output directory for prediction files.
      --dataPath DIR              Path to a directory for downloaded rule data.
                                  Default: ${data.dataPath}

    Input options:
      --inputType TYPE            Input type: fasta, InterProScan, InterProScan6.
                                  If omitted, the type is inferred from the file extension and XML root element.

    Analysis options:
      --systems LIST              Comma-separated list of systems to run: unirule, arba, pirsr.
                                  Default: ${run.systems}
      --outputFormat FORMAT       Prediction output format: TSV or XML.
                                  Default: ${run.outputFormat}
      --chunkSize N               Number of proteins to process per chunk.
                                  Default: ${run.chunkSize}

    Version options:
      --version VERSION           Predefined data version to use, e.g. 2026.4.
                                  Resolved from versions.json: GitHub master merged
                                  with the bundled nextflow/versions.json.
      --dataVersions FILE         Use the given versions.json file instead of resolving
                                  remotely (exclusive override; also used offline).

    InterProScan options (used only when input is FASTA):
      --iprscanVersion VERSION    InterProScan 6 version to run.
      --iprVersion VERSION        InterPro version to use with InterProScan 6.
      --iprscanApplications LIST  Comma-separated list of InterProScan 6 analyses to run,
                                  e.g. HAMAP,Pfam. If empty, InterProScan 6 runs all its analyses.
                                  Default: ${data.iprscanApplications}

    Data options:
      --uniprotRelease RELEASE    UniProt release used to download rule files.
      --pirsrRelease RELEASE      PIRSR data release used to download PIRSR data files.
      --forceDownloads            Re-download remote rule and taxonomy data files even if they already exist locally.

    Container options:
      --unifireImage IMAGE        Docker image used for UniFIRE rule inference.
                                  Default: ${engine.unifireImage}
      --unifireVersion VERSION    Tag of the UniFIRE Docker image.
                                  Default: ${engine.unifireVersion}

    Resource options:
      --unifireMemory MB          Max heap memory for UniFIRE rule inference.
      --pirsrMemory MB            Max heap memory for PIRSR alignment.
      --maxWorkers N              Maximum number of parallel workers.

    Other options:
      -work-dir DIR               Nextflow working directory for intermediate files.
      --help                      Show this message and exit.

    Examples:
      nextflow run . --input samples/proteins.fasta --output out --dataPath data
      nextflow run . --input samples/input_ipr.xml --output out --dataPath data
      nextflow run . --input samples/input_ipr6.xml --output out --dataPath data --systems unirule,arba
    """.stripIndent()
}
