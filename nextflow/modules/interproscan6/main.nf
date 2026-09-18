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
    def profileOpt = iprscan6ProfileNames ? "-profile ${iprscan6ProfileNames.toUnique().join(',')}" : ""
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
