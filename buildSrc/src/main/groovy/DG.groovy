import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.workers.*
import org.yaml.snakeyaml.Yaml
import javax.inject.Inject
import java.time.*

class DG extends DefaultTask {

    final WorkerExecutor workerExecutor

    @InputFile
    File fixSymbol

    @InputFile
    File inputGazeFile

    @InputDirectory
    File sceneVideos

    @InputDirectory
            fixatedVideoSegments

    @OutputDirectory
    File scriptsDir

    @OutputFile
    File destVideoFile

    @Inject
    DG(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void overlay() {
        def inputFileList = []
        def outputFileList = []

        sceneVideos.eachFile { sceneVideo ->
            def name = [fixatedVideoSegments, sceneVideo.name].join('/')
            outputFileList.add(name)
            inputFileList.add(sceneVideo.getAbsolutePath())
        }

        def videoList = project.file("$temporaryDir/fixatedMovies.txt")
        videoList.text = ''
        new Yaml().load(inputGazeFile.newReader()).eachWithIndex { sdata, index ->
            def scriptFile = writeOverlayScript(sdata, index)
            workerExecutor.submit(drawFixationOnVideo.class) { WorkerConfiguration config ->
                config.params inputFileList[index], fixSymbol, scriptFile, outputFileList[index]
            }
            workerExecutor.await()
        }
        fixatedVideoSegments.eachFile { f ->
            videoList.append("file '${[fixatedVideoSegments, f.name].join(' ')}'\n")
        }

        project.exec {
            commandLine 'ffmpeg', '-safe', '0', '-f', 'concat', '-i', videoList, '-c', 'copy', '-y', destVideoFile
        }

    }

    File writeOverlayScript(Object sdata, int index) {
        def offset
        def ts = []
        def xpos = []
        def ypos = []
        def name = [sdata.windowStart.toString(), '_', sdata.windowEnd.toString(), '.txt'].join()
        def scriptFile = project.file("$scriptsDir/${name}_$index")
        scriptFile.text = ''
        sdata.gaze.each { g ->
            offset = offset ?: g.timeStamp.toInstant()
            ts.add(formatInstantToTimestamp(g.timeStamp.toInstant(), offset))
            xpos.add(g.position.xPos)
            ypos.add(g.position.yPos)
        }
        if (ts.size() > 2) {
            scriptFile.append(/[0:v][1:v] overlay=x=${xpos[0]}:y=${ypos[0]}/ +
                    /:enable='between(t,${ts[0]},${ts[1]})' [tmp];/ + '\n')
            for (int ind = 1; ind < (ts.size() - 2); ind++) {
                scriptFile.append(/[tmp][1:v] overlay=x=${xpos[ind]}:y=${ypos[ind]}/ +
                        /:enable='between(t,${ts[ind]},${ts[ind + 1]})' [tmp];/ + '\n')
            }
            scriptFile.append(/[tmp][1:v] overlay=x=${xpos[xpos.size() - 2]}:y=${ypos[ypos.size() - 2]}/ +
                    /:enable='between(t,${ts[ts.size() - 2]},${ts[ts.size() - 1]})'/)
        }
        return scriptFile
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

class drawFixationOnVideo implements Runnable {
    String srcFile
    File fixSymbol
    File scriptFile
    String destFile

    @Inject
    drawFixationOnVideo(String srcFile, File fixSymbol, File scriptFile, String destFile) {
        this.srcFile = srcFile
        this.fixSymbol = fixSymbol
        this.scriptFile = scriptFile
        this.destFile = destFile
    }

    @Override
    void run() {
        def commandLine = ['ffmpeg', '-i', srcFile, '-i', fixSymbol, '-filter_complex_script', scriptFile, '-y', destFile]
        println commandLine.join(' ')
        commandLine.execute().waitFor()
    }
}