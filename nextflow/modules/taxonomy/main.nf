process REFRESH_TAXA_SQLITE {
    label "time_short"
    label "mem_medium"
    label 'unifire_container'

    input:
    path taxaDataDir

    output:
    path "${taxaDataDir}/taxa.sqlite"

    script:
    """
    python3 /opt/scripts/bin/update-taxonomy-cache.py --sqlite-path ${taxaDataDir}/taxa.sqlite
    """

    stub:
    """
    touch ${taxaDataDir}/taxa.sqlite
    """
}

process GENERATE_TAXONOMY_LINEAGE {
    label "time_xlong"
    label "mem_high"
    label 'unifire_container'

    input:
    path iprscanXmlPath
    path taxaFilePath

    output:
    path "taxonomy-lineage.xml"

    script:
    """
    python3 /opt/misc/taxonomy/updateIPRScanWithTaxonomicLineage.py -i ${iprscanXmlPath} -o taxonomy-lineage.xml -t ${taxaFilePath}
    """

    stub:
    """
    touch taxonomy-lineage.xml
    """
}
