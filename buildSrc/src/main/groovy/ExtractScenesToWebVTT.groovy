import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.yaml.snakeyaml.Yaml

class ExtractScenesToWebVTT extends DefaultTask {

    @InputFile
    File srcFile

    @OutputFile
    File destFile

    @TaskAction
    void extract() {
        destFile.withWriter { vtt ->
            vtt.println 'WEBVTT'
            def frameIter = new Yaml().load(srcFile.newReader()).iterator()
            def prevFrame = frameIter.next()
            def frame
            while (frameIter.hasNext()) {
                frame = frameIter.next()
                if (prevFrame.window != frame.window) {
                    vtt.println "\n$prevFrame.date --> $frame.date"
                    vtt.println "{ start: $prevFrame.window.start, end: $prevFrame.window.end }"
                    prevFrame = frame
                }
            }
            vtt.println "\n$prevFrame.date --> $frame.date"
            vtt.println "{ start: $frame.window.start, end: $frame.window.end }"
        }
    }
}
