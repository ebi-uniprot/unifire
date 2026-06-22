process fetch_unirule_urml {
    input:
    val uniruleVersion
    val outputDir

    script:
    """
    URML_OUTPUT_DIRECTORY="${outputDir}"
    mkdir -p \$URML_OUTPUT_DIRECTORY
    FTP_SRC="ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules"

    echo "Downloading rule URML files..."
    for file_prefix in arba-urml unirule-urml unirule-templates unirule.pirsr-urml;
    do
        echo "Downloading \$file_prefix..."
        wget "\${FTP_SRC}/\${file_prefix}-${uniruleVersion}.xml" -O \$URML_OUTPUT_DIRECTORY/\${file_prefix}-latest.xml
    done
    echo "Done downloading rule URML files."
    """
}
