process runIprscan6 {
    label "time_verylong"
    label "mem_high"

    input:
    val iprscanVersion
    val iprVersion
    path inputSequencePath
    path dataDir
    val iprscan6ProfileName

    output:
    path "output.xml", emit: output

    when:
    task.ext.when == null || task.ext.when

    exec:
    // Persist the child run's session cache inside the parent work dir so the
    // child can `-resume` across task re-executions without a persistent dir.
    // This is computed here rather than passed as an input so the task hash
    // stays stable between a first run and a parent `-resume`.
    def cacheDir = file("${workflow.workDir}/iprscan6")
    assert cacheDir.mkdirs()

    def childWorkDir = "${task.workDir}/work"
    def childCacheDir = "${cacheDir}/.nextflow-cache"
    def childRunName = "${workflow.runName}-interproscan6"

    // Detect parent launch options. Done in exec (not as inputs) so that
    // parent `-resume` does not change the task hash: the wrapper task is
    // simply cached and the child is not re-invoked. If the task genuinely
    // re-executes (e.g. changed inputs or a previous attempt failed), the
    // options below let the child resume its own cached state.
    def parentCommand = null
    try {
        parentCommand = workflow.commandLine
    } catch (Exception ignored) {}
    if (!parentCommand) {
        parentCommand = System.getProperty('sun.java.command') ?: ''
    }
    def parentTokens = parentCommand.tokenize()
    def childOpts = ''
    if (parentTokens.contains('-resume')) {
        childOpts = "${childOpts} -resume".trim()
    }
    if (parentTokens.contains('-with-tower')) {
        // TOWER_* environment vars are inherited from the parent env
        // (or forwarded via `process.env` whitelisting in nextflow.config).
        childOpts = "${childOpts} -with-tower".trim()
    }

    def childCmd = """
        nextflow -log ${task.workDir}/nextflow.log run ebi-pf-team/interproscan6 \\
            --applications HAMAP,PROSITE-profiles,PROSITE-patterns,Pfam,NCBIFAM,SMART,PRINTS,SFLD,CDD,CATH-Gene3D,PIRSF,PANTHER,SUPERFAMILY,CATH-FunFam \\
            -r ${iprscanVersion} \\
            --interpro ${iprVersion} \\
            -profile ${iprscan6ProfileName} \\
            ${childOpts} \\
            -name ${childRunName} \\
            -w ${childWorkDir} \\
            --datadir ${dataDir} \\
            --input ${inputSequencePath} \\
            --formats xml \\
            --outdir ${task.workDir}/results
    """.stripIndent().trim()

    // Run command via a shell script so it can be inspected for debugging.
    def runScript = file("$task.workDir/run-iprscan6.sh")
    runScript.text = """#!/usr/bin/env bash
set -euo pipefail

# Isolate the child's session cache so its `-resume` never collides with the
# parent's session lock, while keeping NXF_HOME shared to avoid re-downloading
# plugins.
export NXF_CACHE_DIR="${childCacheDir}"
export NXF_ANSI_LOG=false
mkdir -p "\${NXF_CACHE_DIR}"

${childCmd}
"""

    // Execute the child nextflow run from the task work dir so the staged input
    // and data paths (relative to this task) resolve correctly, just as they do
    // for regular process scripts. The reduce/resume session state lives in
    // NXF_CACHE_DIR (under the parent work dir), not in the launch cwd, so
    // resumability is unaffected by choosing the task dir as the launch dir.
    def stdout = new StringBuilder()
    def stderr = new StringBuilder()
    def proc = ["bash", runScript.toString()].execute(null, task.workDir.toFile())
    proc.waitForProcessOutput(stdout, stderr)
    System.out.print(stdout.toString())
    System.err.print(stderr.toString())
    assert proc.exitValue() == 0: "InterProScan 6 child run failed.\nstdout:\n${stdout}\nstderr:\n${stderr}"

    def resultsDir = file("${task.workDir}/results")
    def xmlFiles = resultsDir.list().findAll { it.endsWith('.xml') }
    assert xmlFiles.size() == 1: "Expected exactly one XML in results, found: ${xmlFiles}"
    file("${resultsDir}/${xmlFiles[0]}").copyTo(file("${task.workDir}/output.xml"))
}

