include {fetch_unirule_urml} from './modules/unirule'
include {fetch_pirsr_data} from './modules/pirsr'
include {build_unifire} from './build_unifire'

workflow {
    main:
    println params.uniprotRelease

    build_unifire()

    def urmlDir = "$workDir/data/urml"
    def pirsrDir = "$workDir/data/pirsr"
//    fetch_unirule_urml(params.uniprotRelease, urmlDir)
//    fetch_pirsr_data(pirsrDir)
}