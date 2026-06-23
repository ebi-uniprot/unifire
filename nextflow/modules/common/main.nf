process downloadRemoteFile {
    input:
    val remoteUri
    path outputDir
    val fileName

    script:
    """
    wget "${remoteUri}" -O ${outputDir}/${fileName}
    echo "${outputDir}/${fileName}"
    """
}

process downloadAndUntarRemoteFile {
    input:
    val remoteUri
    path outputDir

    script:
    """
    wget "${remoteUri}" -O tmp.tar.gz
    tar -zxf tmp.tar.gz -C ${outputDir}
    rm tmp.tar.gz
    echo "$outputDir"
    """
}
