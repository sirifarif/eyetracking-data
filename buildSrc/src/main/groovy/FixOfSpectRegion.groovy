import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*


class FixOfSpectRegion extends DefaultTask {

    @InputFile
    File tobiiFile

    @TaskAction
    void run() {
        def topy = 400 as int
        def botty = 800 as int
        def data = new groovy.json.JsonSlurper().parse(tobiiFile)
        def fixations = data.findAll {
            it.value.yPos.toInteger() >= topy && it.value.yPos.toInteger() <= botty
        }.each { row ->
            println(row)
        }
    }
}