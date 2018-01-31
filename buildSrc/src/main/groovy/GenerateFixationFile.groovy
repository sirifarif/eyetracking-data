import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.yaml.snakeyaml.Yaml
import groovy.json.JsonBuilder

class GenerateFixationFile extends DefaultTask {

    @InputFile
    File mergeLogsFile

    @OutputFile
    File gazeFile

    @TaskAction
    void run() {
        def data = new Yaml().load(mergeLogsFile.newReader())
        def fixationsData = []
        data.each { entry ->
            entry.gaze.findAll { it.gazeRegion != 'other' }.each {
                fixationsData.add(it)
            }
        }
//        fixationsData.each {
//            println(it)
//        }
        gazeFile.text = new JsonBuilder(fixationsData).toPrettyString()
    }
}