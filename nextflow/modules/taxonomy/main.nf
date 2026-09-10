process generateTaxonomyLineage {
    label "time_verylong"
    label "mem_high"

    container "${params.unifireImage}:${params.unifireVersion}"

    input:
    path iprscanXmlPath

    output:
    path "taxonomy-lineage.xml"

    script:
    """
    python3 /opt/misc/taxonomy/updateIPRScanWithTaxonomicLineage.py -i ${iprscanXmlPath} -o taxonomy-lineage.xml -t /opt/ete4/taxa.sqlite
    """
}
