import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.yaml.snakeyaml.Yaml

class GeneratePraatScenes extends DefaultTask {

    @InputFile
    File scenesFile

    @InputFile
    File audioFile

    @OutputDirectory
    File destDir

    @TaskAction
    void generate() {
        def scriptFile = project.file("$temporaryDir/script.praat")
        scriptFile.withWriter { script ->
            script.println "sound = Read from file... $audioFile"
            script.println "spectrogram = To Spectrogram... 0.005 5000 0.002 20 Gaussian"
            script.println 'Helvetica'
            new Yaml().load(scenesFile.newReader()).eachWithIndex { scene, s ->
                script.println 'Erase all'
                script.println 'Select outer viewport... 0 6 0 4'
                script.println 'select sound'
                script.println "Draw... $scene.window.start $scene.window.end 0 0 yes Curve"
                script.println 'Select outer viewport... 0 6 4 8'
                script.println 'select spectrogram'
                script.println "Paint... $scene.window.start $scene.window.end 0 0 100 yes 50 6 0 yes"
                script.println 'Select outer viewport... 0 6 0 8'
                script.println "Save as 300-dpi PNG file... $destDir/scene_${sprintf('%04d', s + 1)}.png"
            }
        }
        project.exec {
            commandLine 'praat', '--no-pref-files', '--no-plugins', '--run', scriptFile
        }
    }

}
