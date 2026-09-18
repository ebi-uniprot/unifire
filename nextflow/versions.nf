def fetchRemoteVersions() {
    def remoteVersionsUri = 'https://raw.githubusercontent.com/ebi-uniprot/unifire/refs/heads/master/versions.json'
    def maxAttempts = 2
    def timeoutMillis = 10_000
    return (1..maxAttempts).findResult { attempt ->
        try {
            def connection = new URL(remoteVersionsUri).openConnection()
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            if (connection.responseCode == 200) {
                return connection.inputStream.text
            }
        }
        catch (Exception e) {
            if (attempt == maxAttempts) {
                log.warn("Fetching versions.json from GitHub master failed: ${e.message}")
            }
        }
        return null
    }
}

def parseVersionsJson(text, source) {
    def requiredDataKeys = ['uniprotRelease', 'iprVersion', 'iprscanVersion', 'pirsrRelease']
    def tree = new groovy.json.JsonSlurper().parseText(text)
    if (!(tree.default instanceof CharSequence) || !(tree.versions instanceof Map) || tree.versions.isEmpty()) {
        throw new IllegalArgumentException("Invalid versions.json (${source}): expected 'default' and a non-empty 'versions' mapping.")
    }
    tree.versions.each { key, value ->
        def missing = requiredDataKeys.findAll { value[it] == null }
        if (missing) {
            throw new IllegalArgumentException("Invalid versions.json (${source}): version '${key}' is missing ${missing.join(', ')}.")
        }
    }
    return tree
}

def selectDataVersions(tree, versionKey) {
    def selected = versionKey ?: tree.default
    def configured = tree.versions[selected.toString()]
    if (configured == null) {
        log.error("Version '${selected}' not found. Available versions: ${tree.versions.keySet().sort().join(', ')}")
        exit(1)
    }
    return [
        version: selected,
        uniprotRelease: configured.uniprotRelease.toString(),
        iprVersion: configured.iprVersion.toString(),
        iprscanVersion: configured.iprscanVersion.toString(),
        pirsrRelease: configured.pirsrRelease.toString()
    ]
}

// Resolves the data version configuration. Source precedence:
//   1. dataVersionsPath, if given ('--dataVersions'): exclusive override, never fetched remotely.
//   2. GitHub master's versions.json merged with the bundled projectDir/versions.json
//      (local entries win on conflicts, so in-development versions keep working).
//   3. Bundled projectDir/versions.json only, when the remote fetch failed.
// The default key when no explicit version is requested: master's default when the
// fetch succeeded, else the bundled default.
def resolveDataVersions(versionKey = null, dataVersionsPath = null) {
    if (dataVersionsPath) {
        def overrideFile = file(dataVersionsPath.toString())
        if (!overrideFile.exists()) {
            log.error("'--dataVersions' file does not exist: ${dataVersionsPath}")
            exit(1)
        }
        try {
            def tree = parseVersionsJson(overrideFile.text, "--dataVersions ${dataVersionsPath}")
            return selectDataVersions(tree, versionKey)
        }
        catch (Exception e) {
            log.error(e.message)
            exit(1)
        }
    }

    def localFile = new File("${projectDir}/nextflow/versions.json")
    def localTree
    try {
        localTree = parseVersionsJson(localFile.text, 'bundled versions.json')
    }
    catch (Exception e) {
        log.error("Cannot read bundled nextflow/versions.json (${localFile}): ${e.message}")
        exit(1)
    }

    try {
        def remoteText = fetchRemoteVersions()
        if (remoteText != null) {
            def remoteTree = parseVersionsJson(remoteText, 'GitHub master')
            def tree = ['default': remoteTree.default, 'versions': new LinkedHashMap(remoteTree.versions)]
            localTree.versions.each { key, value -> tree.versions[key] = value }
            return selectDataVersions(tree, versionKey)
        }
    }
    catch (Exception e) {
        log.warn("Falling back to bundled nextflow/versions.json: ${e.message}")
    }

    return selectDataVersions(localTree, versionKey)
}
