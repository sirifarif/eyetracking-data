import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.yaml.snakeyaml.Yaml

import java.time.*

class ExtractScenesToSrt extends DefaultTask {

    @InputFile
    File srcFile

    @OutputFile
    File destFile

    @TaskAction
    void extract() {
        destFile.withWriter { srt ->
            def index = 1
            def frameIter = new Yaml().load(srcFile.newReader()).iterator()
            def prevFrame = frameIter.next()
            def frame
            Instant offset = prevFrame.date.toInstant()
            while (frameIter.hasNext()) {
                frame = frameIter.next()
                if (prevFrame.window != frame.window) {
                    def start = formatInstantToTimestamp(prevFrame.date.toInstant(), offset)
                    def end = formatInstantToTimestamp(frame.date.toInstant(), offset)
                    srt.println "${index++}"
                    srt.println "$start --> $end"
                    srt.println sprintf('{ start: %.6f, end: %.6f }\n', prevFrame.window.start, prevFrame.window.end)
                    prevFrame = frame
                }
            }
            def start = formatInstantToTimestamp(prevFrame.date.toInstant(), offset)
            def end = formatInstantToTimestamp(frame.date.toInstant(), offset)
            srt.println "$index"
            srt.println "$start --> $end"
            srt.println sprintf('{ start: %.6f, end: %.6f }', frame.window.start, frame.window.end)
        }
    }

    String formatInstantToTimestamp(Instant instant, Instant offset) {
        def duration = Duration.between(offset, instant)
        def fields = [
                duration.toHours(),
                duration.toMinutes() - duration.toHours() * 60,
                duration.seconds - duration.toMinutes() * 60,
                duration.toMillis() - duration.seconds * 1000
        ]
        sprintf '%02d:%02d:%02d,%03d', fields
    }
}
