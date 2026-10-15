include { DOWNLOAD_REMOTE_FILE as DOWNLOAD_URML_TEMPLATES } from './modules/common'
include { DOWNLOAD_REMOTE_FILE as DOWNLOAD_URML } from './modules/common'
include { DOWNLOAD_REMOTE_FILE as DOWNLOAD_ARBA } from './modules/common'
include { DOWNLOAD_AND_UNTAR_REMOTE_FILE as DOWNLOAD_PIRSR } from './modules/common'
include { DOWNLOAD_REMOTE_FILE as DOWNLOAD_PIRSR_URML } from './modules/common'
include { REFRESH_TAXA_SQLITE } from './modules/taxonomy'

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

    if (uniruleEnabled) {
        if (shouldDownloadFile(urmlTemplatesFilePath, forceDownloads)) {
            def urmlTemplatesUri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/unirule-templates-${uniprotRelease}.xml"
            urmlTemplatesFilePath = DOWNLOAD_URML_TEMPLATES(urmlTemplatesUri, urmlReleasePath, "unirule-templates.xml")
        }

        if (shouldDownloadFile(uniruleUrmlFilePath, forceDownloads)) {
            def uri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/unirule-urml-${uniprotRelease}.xml"
            uniruleUrmlFilePath = DOWNLOAD_URML(uri, urmlReleasePath, "unirule-urml.xml")
        }
    }

    if (arbaEnabled) {
        if (shouldDownloadFile(arbaUrmlFilePath, forceDownloads)) {
            def uri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/arba-urml-${uniprotRelease}.xml"
            arbaUrmlFilePath = DOWNLOAD_ARBA(uri, urmlReleasePath, "arba-urml.xml")
        }
    }

    if (pirsrEnabled) {
        if (shouldDownloadFile(pirsrUrmlFilePath, forceDownloads)) {
            def urmlUri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/unirule.pirsr-urml-${uniprotRelease}.xml"
            pirsrUrmlFilePath = DOWNLOAD_PIRSR_URML(urmlUri, urmlReleasePath, "unirule.pirsr-urml.xml")
        }

        if (forceDownloads || pirsrDir.isEmpty()) {
            def uri = "https://proteininformationresource.org/pirsr/pirsr_data_${pirsrRelease}.tar.gz"
            pirsrDir = DOWNLOAD_PIRSR(uri, pirsrReleasePath)
        }
    }

    def taxaBasePath = dataPathFile.resolve("taxa")
    assert taxaBasePath.mkdirs()
    taxaFilePath = taxaBasePath.resolve("taxa.sqlite")

    if (shouldDownloadFile(taxaFilePath, forceDownloads)) {
        taxaFilePath = REFRESH_TAXA_SQLITE(taxaBasePath)
    }

    emit:
    dataPath = dataPathFile
    urmlTemplatesFilePath
    uniruleUrmlFilePath
    arbaUrmlFilePath
    pirsrUrmlFilePath
    pirsrDir
    taxaFilePath
}
