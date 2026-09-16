process refreshTaxaSqlite {
    label "time_short"
    label "mem_medium"

    container "${unifireImage}:${unifireVersion}"

    input:
    path taxaDataDir
    val unifireImage
    val unifireVersion

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

    container "${unifireImage}:${unifireVersion}"

    input:
    path iprscanXmlPath
    path taxaFilePath
    val unifireImage
    val unifireVersion

    output:
    path "taxonomy-lineage.xml"

    script:
    """
    python3 /opt/misc/taxonomy/updateIPRScanWithTaxonomicLineage.py -i ${iprscanXmlPath} -o taxonomy-lineage.xml -t ${taxaFilePath}
    """
}
