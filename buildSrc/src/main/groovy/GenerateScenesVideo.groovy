import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.workers.*
import org.yaml.snakeyaml.Yaml

import javax.inject.Inject

class GenerateSceneVideoSegments extends DefaultTask {

    final WorkerExecutor workerExecutor

    @InputFile
    File scenesFile

    @InputDirectory
    File inputDir

    @OutputDirectory
    File destDir

    @Inject
    GenerateSceneVideoSegments(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void generate() {
        new Yaml().load(scenesFile.newReader()).eachWithIndex { scene, s ->
            def pngFile = project.file("$inputDir/scene_${sprintf('%04d', s + 1)}.png")
            def duration = groovy.time.TimeCategory.minus(scene.end, scene.start).seconds
            def videoFile = project.file("$destDir/scene_${sprintf('%04d', s + 1)}.mp4")
            workerExecutor.submit(SceneVideoGenerator.class) { WorkerConfiguration config ->
                config.params pngFile, duration * 25, videoFile
            }
        }
    }
}

class SceneVideoGenerator implements Runnable {

    File pngFile
    int frames
    File videoFile

    @Inject
    SceneVideoGenerator(File pngFile, int frames, File videoFile) {
        this.pngFile = pngFile
        this.frames = frames
        this.videoFile = videoFile
    }

    @Override
    void run() {
        def commandLine = ['ffmpeg', '-r', 25, '-f', 'image2', '-s', '1920x1080', '-i', pngFile, '-vframes', frames, '-vcodec', 'libx264', '-crf', 25, '-pix_fmt', 'yuv420p', videoFile, '-y']
        println commandLine.join(" ")
        commandLine.execute().waitFor()
    }
}
