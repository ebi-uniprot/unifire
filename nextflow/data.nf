include { downloadRemoteFile as downloadUrmlTemplates } from './modules/common'
include { downloadRemoteFile as downloadUrml } from './modules/common'
include { downloadRemoteFile as downloadArba } from './modules/common'
include { downloadAndUntarRemoteFile as downloadPirsr } from './modules/common'
include { downloadRemoteFile as downloadPirsrUrml } from './modules/common'
workflow fetchData {
    take:
    dataPath

    main:
    // Use Channel.empty() with capital C
    urmlTemplatesFilePath = Channel.empty()
    urmlFilePath = Channel.empty()
    arbaFilePath = Channel.empty()
    pirsrUrmlFilePath = Channel.empty()
    pirsrDir = Channel.empty()

    if (!params.skipDownloads) {
        dataPath = file(dataPath)
        if (dataPath.isFile()) {
            log.error("'--dataPath <DATA-DIR>' is required and cannot be an existing file.")
            exit(1)
        }
        else if (!dataPath.isDirectory()) {
            dataPath.mkdirs()
        }

        if (params.useUrml || params.useArba) {
            def urmlTemplatesPath = dataPath.resolve("urml")
            urmlTemplatesPath.mkdirs()
            def urmlTemplatesUri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/unirule-templates-${params.uniprotRelease}.xml"
            urmlTemplatesFilePath = downloadUrmlTemplates(urmlTemplatesUri, urmlTemplatesPath, "unirule-templates.xml")
        }

        if (params.useUrml) {
            def urmlPath = dataPath.resolve("urml")
            urmlPath.mkdirs()
            def urmlUri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/unirule-urml-${params.uniprotRelease}.xml"
            urmlFilePath = downloadUrml(urmlUri, urmlPath, "unirule-urml.xml")
        }

        if (params.useArba) {
            def urmlPath = dataPath.resolve("urml")
            urmlPath.mkdirs()
            def urmlUri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/arba-urml-${params.uniprotRelease}.xml"
            arbaFilePath = downloadArba(urmlUri, urmlPath, "arba-urml.xml")
        }

        if (params.usePirsr) {
            def urmlPath = dataPath.resolve("urml")
            urmlPath.mkdirs()
            def urmlUri = "ftp://ftp.ebi.ac.uk/pub/contrib/UniProt/UniFIRE/rules/unirule.pirsr-urml-${params.uniprotRelease}.xml"
            pirsrUrmlFilePath = downloadPirsrUrml(urmlUri, urmlPath, "unirule.pirsr-urml.xml")

            def path = dataPath.resolve("pirsr")
            path.mkdirs()
            def uri = "https://proteininformationresource.org/pirsr/pirsr_data_latest.tar.gz"
            pirsrDir = downloadPirsr(uri, path)
        }
    }

    emit:
    urmlTemplatesFilePath
    urmlFilePath
    arbaFilePath
    pirsrUrmlFilePath
    pirsrDir
}
