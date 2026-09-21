process runIprscan6 {
    label "time_xlong"
    label "mem_high"

    input:
    val iprscanVersion
    val iprVersion
    val iprscanApplications
    path inputSequencePath
    path dataDir
    val iprscan6ProfileNames

    output:
    path "output.xml", emit: output

    when:
    task.ext.when == null || task.ext.when

    script:
    // The profile names are a List when extended from the configuration, but a
    // plain String when set via CLI (--iprscan6ProfileNames foo); normalise
    // before joining, since String has no toUnique().
    def profileNameInput = (iprscan6ProfileNames instanceof Collection)
        ? iprscan6ProfileNames
        : (iprscan6ProfileNames ? [iprscan6ProfileNames.toString()] : [])
    def profileNames = profileNameInput.findAll { it }
    def profileOpt = profileNames ? "-profile ${profileNames.toUnique().join(',')}" : ""
    def applicationsOpt = iprscanApplications ? "--applications ${iprscanApplications}" : ""
    """
    mkdir results/
    nextflow run ebi-pf-team/interproscan6 \
        ${profileOpt} \
        ${applicationsOpt} \
        -r ${iprscanVersion} \
        --interpro ${iprVersion} \
        --datadir $dataDir \
        --input ${inputSequencePath} \
        --formats xml \
        --no-matches-api \
        --outdir results
    mv results/*.xml output.xml
    """

    stub:
    """
    touch output.xml
    """
}
