import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.yaml.snakeyaml.Yaml

import java.time.*

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
            Instant offset = prevFrame.date.toInstant()
            while (frameIter.hasNext()) {
                frame = frameIter.next()
                if (prevFrame.window != frame.window) {
                    def start = formatInstantToTimestamp(prevFrame.date.toInstant(), offset)
                    def end = formatInstantToTimestamp(frame.date.toInstant(), offset)
                    vtt.println "\n$start --> $end"
                    vtt.println "{ start: $prevFrame.window.start, end: $prevFrame.window.end }"
                    prevFrame = frame
                }
            }
            def start = formatInstantToTimestamp(prevFrame.date.toInstant(), offset)
            def end = formatInstantToTimestamp(frame.date.toInstant(), offset)
            vtt.println "\n$start --> $end"
            vtt.println "{ start: $frame.window.start, end: $frame.window.end }"
        }
    }

    String formatInstantToTimestamp(Instant instant, Instant offset) {
        def duration = Duration.between(offset, instant)
        [
                duration.toHours(),
                duration.toMinutes() - duration.toHours() * 60,
                duration.seconds - duration.toMinutes() * 60
        ].collect { "$it".padLeft(2, '0') }.join(':')
    }
}
