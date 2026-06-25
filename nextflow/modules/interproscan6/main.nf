process runIprscan6 {
    tag "iprscan6"

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
    mkdir -p iprscan6-data
    echo "Copying InterProScan 6 data from data dir to staging..."
    cp -r $dataDir/* iprscan6-data
    echo "Copy finish"
    nextflow run ebi-pf-team/interproscan6 \
        --applications HAMAP,PROSITE-profiles,PROSITE-patterns,Pfam,NCBIFAM,SMART,PRINTS,SFLD,CDD,CATH-Gene3D,PIRSF,PANTHER,SUPERFAMILY,CATH-FunFam \
        -r ${iprscanVersion} \
        --interpro ${iprVersion} \
        -profile docker \
        --datadir iprscan6-data \
        --input ${inputSequencePath} \
        --formats xml \
        --outdir results
    mv results/*.xml output.xml
    echo "Copying InterProScan 6 data from staging to data dir..."
    cp -r iprscan6-data/* $dataDir
    rm -r iprscan6-data
    echo "Copy finish"
    """
}
