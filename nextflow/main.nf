include { buildMaven } from './modules/maven'
include { fetchData } from './data.nf'

workflow {
    println("Using UniProt release: $params.uniprotRelease")

    fetchData(params.dataPath)
    def outArtifacts = buildMaven(file("${projectDir}/../"))
    println(outArtifacts.path)
}
