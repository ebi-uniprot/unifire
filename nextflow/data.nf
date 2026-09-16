include { downloadRemoteFile as downloadUrmlTemplates } from './modules/common'
include { downloadRemoteFile as downloadUrml } from './modules/common'
include { downloadRemoteFile as downloadArba } from './modules/common'
include { downloadAndUntarRemoteFile as downloadPirsr } from './modules/common'
include { downloadRemoteFile as downloadPirsrUrml } from './modules/common'

def shouldDownloadFile(path: Path, forceDownloads: boolean) {
    def pathExists = path.isFile()
    return !pathExists || forceDownloads
}

workflow fetchData {
    take:
    dataPath
    systems
    uniprotRelease
    pirsrRelease
    forceDownloads

    main:
    def dataPathFile = file(dataPath)
    if (dataPathFile.isFile()) {
        log.error("'--dataPath <DATA-DIR>' is required and cannot be an existing file.")
        exit(1)
    }

    if (!dataPathFile.isDirectory()) {
        assert dataPathFile.mkdirs()
    }

    def urmlBasePath = dataPathFile.resolve("urml")
    assert urmlBasePath.mkdirs()
    def urmlReleasePath = urmlBasePath.resolve(uniprotRelease)
    assert urmlReleasePath.mkdirs()

    def pirsrBasePath = dataPathFile.resolve("pirsr")
    assert pirsrBasePath.mkdirs()
    def pirsrReleasePath = pirsrBasePath.resolve(pirsrRelease)
    assert pirsrReleasePath.mkdirs()

    urmlTemplatesFilePath = urmlReleasePath.resolve("unirule-templates.xml")
    uniruleUrmlFilePath = urmlReleasePath.resolve("unirule-urml.xml")
    arbaUrmlFilePath = urmlReleasePath.resolve("arba-urml.xml")
    pirsrUrmlFilePath = urmlReleasePath.resolve("unirule.pirsr-urml.xml")
    pirsrDir = pirsrReleasePath

    def uniruleEnabled = 'unirule' in systems
    def arbaEnabled = 'arba' in systems
    def pirsrEnabled = 'pirsr' in systems

    if (dataPathFile.isFile()) {
        log.error("'--dataPath <DATA-DIR>' is required and cannot be an existing file.")
        exit(1)
    }
    else if (!dataPathFile.isDirectory()) {
        assert dataPathFile.mkdirs()
    }

    if (uniruleEnabled || arbaEnabled) {
        if (shouldDownloadFile(urmlTemplatesFilePath, forceDownloads)) {
            def urmlTemplatesUri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/unirule-templates-${uniprotRelease}.xml"
            urmlTemplatesFilePath = downloadUrmlTemplates(urmlTemplatesUri, urmlReleasePath, "unirule-templates.xml")
        }
    }

    if (uniruleEnabled) {
        if (shouldDownloadFile(uniruleUrmlFilePath, forceDownloads)) {
            def uri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/unirule-urml-${uniprotRelease}.xml"
            uniruleUrmlFilePath = downloadUrml(uri, urmlReleasePath, "unirule-urml.xml")
        }
    }

    if (arbaEnabled) {
        if (shouldDownloadFile(arbaUrmlFilePath, forceDownloads)) {
            def uri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/arba-urml-${uniprotRelease}.xml"
            arbaUrmlFilePath = downloadArba(uri, urmlReleasePath, "arba-urml.xml")
        }
    }

    if (pirsrEnabled) {
        if (shouldDownloadFile(pirsrUrmlFilePath, forceDownloads)) {
            def urmlUri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/unirule.pirsr-urml-${uniprotRelease}.xml"
            pirsrUrmlFilePath = downloadPirsrUrml(urmlUri, urmlReleasePath, "unirule.pirsr-urml.xml")
        }

        if (forceDownloads || pirsrDir.isEmpty()) {
            def uri = "https://proteininformationresource.org/pirsr/pirsr_data_${pirsrRelease}.tar.gz"
            pirsrDir = downloadPirsr(uri, pirsrReleasePath)
        }
    }

    emit:
    dataPath = dataPathFile
    urmlTemplatesFilePath
    uniruleUrmlFilePath
    arbaUrmlFilePath
    pirsrUrmlFilePath
    pirsrDir
}
