process runIprscan6 {
    label "time_verylong"
    label "mem_high"

    input:
    val iprscanVersion
    val iprVersion
    path inputSequencePath
    path dataDir

    output:
    path "output.xml", emit: output

    when:
    task.ext.when == null || task.ext.when

    script:
    """
    mkdir results/
    nextflow run ebi-pf-team/interproscan6 \
        --applications HAMAP,PROSITE-profiles,PROSITE-patterns,Pfam,NCBIFAM,SMART,PRINTS,SFLD,CDD,CATH-Gene3D,PIRSF,PANTHER,SUPERFAMILY,CATH-FunFam \
        -r ${iprscanVersion} \
        --interpro ${iprVersion} \
        -profile ${params.iprscan6ProfileName} \
        --datadir $dataDir \
        --input ${inputSequencePath} \
        --formats xml \
        --outdir results
    mv results/*.xml output.xml
    """
}
