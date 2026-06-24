include { downloadRemoteFile as downloadUrmlTemplates } from './modules/common'
include { downloadRemoteFile as downloadUrml } from './modules/common'
include { downloadRemoteFile as downloadArba } from './modules/common'
include { downloadAndUntarRemoteFile as downloadPirsr } from './modules/common'
include { downloadRemoteFile as downloadPirsrUrml } from './modules/common'
workflow fetchData {
    take:
    dataPath

    main:
    dataPath = file(dataPath)

    def urmlBasePath = dataPath.resolve("urml")
    urmlBasePath.mkdirs()
    def pirsrBasePath = dataPath.resolve("pirsr")
    pirsrBasePath.mkdirs()

    urmlTemplatesFilePath = urmlBasePath.resolve("unirule-templates.xml")
    uniruleUrmlFilePath = urmlBasePath.resolve("unirule-urml.xml")
    arbaUrmlFilePath = urmlBasePath.resolve("arba-urml.xml")
    pirsrUrmlFilePath = urmlBasePath.resolve("unirule.pirsr-urml.xml")
    pirsrDir = pirsrBasePath

    if (!params.skipDownloads) {
        if (dataPath.isFile()) {
            log.error("'--dataPath <DATA-DIR>' is required and cannot be an existing file.")
            exit(1)
        }
        else if (!dataPath.isDirectory()) {
            dataPath.mkdirs()
        }

        if (params.useUrml || params.useArba) {
            def urmlTemplatesUri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/unirule-templates-${params.uniprotRelease}.xml"
            urmlTemplatesFilePath = downloadUrmlTemplates(urmlTemplatesUri, urmlBasePath, "unirule-templates.xml")
        }

        if (params.useUrml) {
            def urmlUri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/unirule-urml-${params.uniprotRelease}.xml"
            uniruleUrmlFilePath = downloadUrml(urmlUri, urmlBasePath, "unirule-urml.xml")
        }

        if (params.useArba) {
            def urmlUri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/arba-urml-${params.uniprotRelease}.xml"
            arbaUrmlFilePath = downloadArba(urmlUri, urmlBasePath, "arba-urml.xml")
        }

        if (params.usePirsr) {
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
