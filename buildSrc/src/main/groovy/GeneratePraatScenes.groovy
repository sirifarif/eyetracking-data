import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.process.JavaForkOptions
import org.gradle.workers.*
import org.yaml.snakeyaml.Yaml

import javax.inject.Inject

class GeneratePraatScenes extends DefaultTask {

    final WorkerExecutor workerExecutor

    @InputFile
    File scenesFile

    @InputFile
    File audioFile

    @OutputDirectory
    File destDir

    @Inject
    GeneratePraatScenes(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void generate() {
        def scriptFile = project.file("$temporaryDir/script.praat")
        def spectrogramFile = project.file("$temporaryDir/sound.Spectrogram")
        scriptFile.withWriter { script ->
            script.println "Read from file... $audioFile"
            script.println "To Spectrogram... 0.005 5000 0.002 20 Gaussian"
            script.println "Write to binary file... $spectrogramFile"
        }
        project.exec {
            commandLine 'praat', '--no-pref-files', '--no-plugins', '--run', scriptFile
        }
        new Yaml().load(scenesFile.newReader()).eachWithIndex { scene, s ->
            workerExecutor.submit(PraatSceneGenerator.class) { WorkerConfiguration config ->
                config.isolationMode = IsolationMode.PROCESS
                config.forkOptions { JavaForkOptions options ->
                    options.maxHeapSize = "512m"
                }
                config.params audioFile, spectrogramFile, scene.window.start, scene.window.end, project.file("$destDir/scene_${sprintf('%04d', s + 1)}.png")
            }
        }
        workerExecutor.await()
    }

}

class PraatSceneGenerator implements Runnable {

    File soundFile
    File spectrogramFile
    double start
    double end
    File pngFile

    @Inject
    PraatSceneGenerator(File soundFile, File spectrogramFile, double start, double end, File pngFile) {
        this.soundFile = soundFile
        this.spectrogramFile = spectrogramFile
        this.start = start
        this.end = end
        this.pngFile = pngFile
    }

    @Override
    void run() {
        def scriptFile = File.createTempFile('script', '.praat')
        scriptFile.withWriter { script ->
            script.println 'Helvetica'
            script.println 'Erase all'
            script.println 'Select inner viewport... 1.5 22.5 1.5 5.5'
            script.println "Read from file... $soundFile"
            script.println "Draw... $start $end 0 0 no Curve"
            script.println "Draw inner box"
            script.println "One mark left... 0.0 yes yes yes"
            script.println 'Select inner viewport... 1.5 22.5 5.5 9.5'
            script.println "Read from file... $spectrogramFile"
            script.println "Paint... $start $end 0 0 100 yes 50 6 0 yes"
            script.println 'Select outer viewport... 0 24 0 15'
            script.println "Save as 300-dpi PNG file... $pngFile"
        }
        ['/usr/local/bin/praat', '--no-pref-files', '--no-plugins', '--run', scriptFile].execute().waitFor()
    }
}
