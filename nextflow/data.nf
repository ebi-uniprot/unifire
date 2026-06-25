include { downloadRemoteFile as downloadUrmlTemplates } from './modules/common'
include { downloadRemoteFile as downloadUrml } from './modules/common'
include { downloadRemoteFile as downloadArba } from './modules/common'
include { downloadAndUntarRemoteFile as downloadPirsr } from './modules/common'
include { downloadRemoteFile as downloadPirsrUrml } from './modules/common'
workflow fetchData {
    take:
    dataPath
    systems

    main:
    dataPath = file(dataPath)

    def urmlBasePath = dataPath.resolve("urml")
    assert urmlBasePath.mkdirs()
    def pirsrBasePath = dataPath.resolve("pirsr")
    assert pirsrBasePath.mkdirs()

    urmlTemplatesFilePath = urmlBasePath.resolve("unirule-templates.xml")
    uniruleUrmlFilePath = urmlBasePath.resolve("unirule-urml.xml")
    arbaUrmlFilePath = urmlBasePath.resolve("arba-urml.xml")
    pirsrUrmlFilePath = urmlBasePath.resolve("unirule.pirsr-urml.xml")
    pirsrDir = pirsrBasePath

    def uniruleEnabled = 'unirule' in systems
    def arbaEnabled = 'arba' in systems
    def pirsrEnabled = 'pirsr' in systems

    if (!params.skipDownloads) {
        if (dataPath.isFile()) {
            log.error("'--dataPath <DATA-DIR>' is required and cannot be an existing file.")
            exit(1)
        }
        else if (!dataPath.isDirectory()) {
            assert dataPath.mkdirs()
        }

        if (uniruleEnabled || arbaEnabled) {
            def urmlTemplatesUri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/unirule-templates-${params.uniprotRelease}.xml"
            urmlTemplatesFilePath = downloadUrmlTemplates(urmlTemplatesUri, urmlBasePath, "unirule-templates.xml")
        }

        if (uniruleEnabled) {
            def uri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/unirule-urml-${params.uniprotRelease}.xml"
            uniruleUrmlFilePath = downloadUrml(uri, urmlBasePath, "unirule-urml.xml")
        }

        if (arbaEnabled) {
            def uri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/arba-urml-${params.uniprotRelease}.xml"
            arbaUrmlFilePath = downloadArba(uri, urmlBasePath, "arba-urml.xml")
        }

        if (pirsrEnabled) {
            def urmlUri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/unirule.pirsr-urml-${params.uniprotRelease}.xml"
            pirsrUrmlFilePath = downloadPirsrUrml(urmlUri, urmlBasePath, "unirule.pirsr-urml.xml")

            def uri = "https://proteininformationresource.org/pirsr/pirsr_data_latest.tar.gz"
            pirsrDir = downloadPirsr(uri, pirsrBasePath)
        }
    }

    emit:
    dataPath
    urmlTemplatesFilePath
    uniruleUrmlFilePath
    arbaUrmlFilePath
    pirsrUrmlFilePath
    pirsrDir
}
