process downloadRemoteFile {
    label "time_short"
    label "mem_low"

    input:
    val remoteUri
    path outputDir
    val fileName

    output:
    path "${outputDir}/${fileName}"

    script:
    """
    wget --timeout=30 --tries=3 "${remoteUri}" -O ${outputDir}/${fileName}
    if [[ ! -f "${outputDir}/${fileName}" ]] || [[ ! -s "${outputDir}/${fileName}" ]]; then
        echo "ERROR: Download failed or produced empty file: ${remoteUri}" >&2
        exit 1
    fi
    echo "${outputDir}/${fileName}"
    """
}

process downloadAndUntarRemoteFile {
    label "time_short"
    label "mem_low"

    input:
    val remoteUri
    path outputDir

    output:
    path outputDir

    script:
    """
    wget "${remoteUri}" -O tmp.tar.gz
    tar -zxf tmp.tar.gz -C ${outputDir}
    rm tmp.tar.gz
    echo "${outputDir}"
    """
}
