import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.yaml.snakeyaml.Yaml

import java.time.*

class DrawGazes extends DefaultTask {

    @InputFile
    File fixSymbol

    @InputFile
    File srcFile

    @InputFile
    File gazeFile

    @OutputFile
    File destFile

    @TaskAction
    void overlay() {
        def scriptFile = project.file("$temporaryDir/script.txt")
        scriptFile.text = ""
        def offset

        //def points = new gazePoints() as List
        def ts = []
        def xpos = []
        def ypos = []
        new Yaml().load(gazeFile.newReader()).each { sdate ->
            def row = [:]
            sdate.gaze.each { g ->
                offset = offset ?: g.timeStamp.toInstant()
                def start = formatInstantToTimestamp(g.timeStamp.toInstant(), offset)
                ts.add(start)
                xpos.add(g.position.xPos)
                ypos.add(g.position.yPos)
            }
        }

        scriptFile.append(/[0:v][1:v] overlay=x=${xpos[0]}:y=${ypos[0]}:enable='between(t,${ts[0]},${ts[1]})' [tmp];/)
        scriptFile.append('\n')
        for (int ind = 1; ind < (ts.size() - 3); ind++) {
            scriptFile.append(/[tmp][1:v] overlay=x=${xpos[ind]}:y=${ypos[ind]}:enable='between(t,${ts[ind]},${
                ts[ind + 1]
            })' [tmp];/)
            scriptFile.append('\n')
        }
        scriptFile.append(/[tmp][1:v] overlay=x=${xpos[xpos.size() - 2]}:y=${ypos[ypos.size() - 2]}:enable='between(t,${
            ts[ts.size() - 2]
        },${ts[ts.size() - 1]})'/)

        project.exec {
            commandLine 'ffmpeg', '-i', srcFile, '-i', fixSymbol, '-filter_complex_script', scriptFile, '-y', destFile
        }
    }

    String formatInstantToTimestamp(Instant instant, Instant offset) {
        def duration = Duration.between(offset, instant)
        def fields = [
                duration.seconds,
                duration.toMillis() - duration.seconds * 1000
        ]
        sprintf '%d.%03d', fields
    }
}
