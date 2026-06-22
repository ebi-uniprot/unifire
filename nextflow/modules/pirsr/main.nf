process fetch_pirsr_data {
    input:
    val outputDir

    script:
    """
    PIRSR_DATA_SRC="https://proteininformationresource.org/pirsr/pirsr_data_latest.tar.gz"
    mkdir -p $outputDir
    echo "Download pirsr data files..."
    wget \${PIRSR_DATA_SRC} -O $outputDir/pirsr_data_latest.tar.gz
    echo "untarring pirsr_data..."
    tar -zxf $outputDir/pirsr_data_latest.tar.gz -C $outputDir
    rm $outputDir/pirsr_data_latest.tar.gz
    echo "Done download pirsr data files."
    """
}