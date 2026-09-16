process refreshTaxaSqlite {
    label "time_short"
    label "mem_medium"

    container "${params.unifireImage}:${params.unifireVersion}"

    input:
    path taxaDataDir

    output:
    path "${taxaDataDir}/taxa.sqlite"

    script:
    """
    /opt/scripts/bin/update-taxonomy-cache.sh
    mv /opt/ete4/taxa.sqlite ${taxaDataDir}/taxa.sqlite
    """
}

process generateTaxonomyLineage {
    label "time_verylong"
    label "mem_high"

    container "${params.unifireImage}:${params.unifireVersion}"

    input:
    path iprscanXmlPath
    path taxaFilePath

    output:
    path "taxonomy-lineage.xml"

    script:
    """
    python3 /opt/misc/taxonomy/updateIPRScanWithTaxonomicLineage.py -i ${iprscanXmlPath} -o taxonomy-lineage.xml -t ${taxaFilePath}
    """
}
