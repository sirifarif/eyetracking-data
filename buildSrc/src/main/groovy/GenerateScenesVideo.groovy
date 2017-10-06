import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.yaml.snakeyaml.Yaml

class GenerateVideoFile extends DefaultTask {

    @InputFile
    File scenesFile

    @InputDirectory
    File inputDir

    @OutputFile
    File videoFile

    @TaskAction
    void generate() {

        def sceneId = []
        new File("$inputDir").eachFileMatch(~/.*.png/) { file ->
            sceneId.add(project.relativePath(file))
        }
        def scriptFile = project.file("$project.projectDir/concat.txt")
        scriptFile.withWriter { script ->
            new Yaml().load(scenesFile.newReader()).eachWithIndex { scene, s ->
                def duration = groovy.time.TimeCategory.minus(scene.end, scene.start).seconds
                script.println "file '${sceneId[s]}'"
                script.println "duration $duration"
            }
        }
        project.exec {
            workingDir project.projectDir
            commandLine 'ffmpeg', '-f', 'concat', '-i', scriptFile, '-s', '720x480', '-c:v', 'libx264', '-crf', '18', '-vf', 'fps=25', '-pix_fmt', 'yuv420p', '-y', videoFile
        }
    }
}
