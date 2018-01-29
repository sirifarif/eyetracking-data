import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.yaml.snakeyaml.Yaml

import javax.naming.event.ObjectChangeListener


class FixOfSpectRegion extends DefaultTask {

    @InputFile
    File mergeLogsFile

    @TaskAction
    void run() {
        def data = new Yaml().load(mergeLogsFile.newReader())
        def fixationOfSpectrogram = []
        data.each { entry ->
            entry.gaze.findAll { it.gazeRegion == 'spectrogram' }.each {
                fixationOfSpectrogram.add(it)
            }
        }
        println(fixationOfSpectrogram.gazeDur.min())
        println(fixationOfSpectrogram.gazeDur.max())
//        fixationOfSpectrogram.each {
//            println(it)
//        }
    }
}