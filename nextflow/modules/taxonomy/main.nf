process generateTaxonomyLineage {
    container "unifire/unifire-taxonomy:$params.unifireVersion"

    input:
    path iprscanXmlPath

    output:
    path "taxonomy-lineage.xml"

    script:
    """
    python3 /opt/code/update-taxonomy-cache.py
    python3 /opt/code/updateIPRScanWithTaxonomicLineage.py -i ${iprscanXmlPath} -o taxonomy-lineage.xml -t /opt/taxa.sqlite
    """
}
