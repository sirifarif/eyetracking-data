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

    @OutputFile
    File finalVideoFile

    @Inject
    GenerateSceneVideoSegments(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void generate() {
        def movieListFile = project.file("$temporaryDir/movieList.txt")
        movieListFile.text = ''
        def firstFrame = project.file("$project.rootDir/firstframe.png")
        def firstFrameVideo = project.file("$destDir/firstFrameVideo_${project.name}.mp4")
        def offsetStr = project.findProperty('offset')
        def padding = offsetStr ? Float.parseFloat(offsetStr) : 0
        if (padding > 0) {
            workerExecutor.submit(SceneVideoGenerator.class) { WorkerConfiguration config ->
                config.params firstFrame, padding / 1000, firstFrameVideo
            }
            workerExecutor.await()
            movieListFile.text = "file '$firstFrameVideo'\n"
        }
        new Yaml().load(scenesFile.newReader()).eachWithIndex { scene, s ->
            def pngFile = project.file("$inputDir/scene_${sprintf('%04d', s + 1)}.png")
            def duration = groovy.time.TimeCategory.minus(scene.end, scene.start)
            def durInSeconds = (duration.minutes * 60) + duration.seconds + (duration.millis / 1000) as double
            def videoFile = project.file("$destDir/scene_${sprintf('%04d', s + 1)}.mp4")
            movieListFile.append "file '$videoFile'\n"
            workerExecutor.submit(SceneVideoGenerator.class) { WorkerConfiguration config ->
                config.params pngFile, durInSeconds, videoFile
            }
        }
        workerExecutor.await()
        project.exec {
            commandLine 'ffmpeg', '-safe', '0', '-f', 'concat', '-i', movieListFile, '-c', 'copy', '-y', finalVideoFile
        }
    }
}

class SceneVideoGenerator implements Runnable {
    File pngFile
    double duration
    File videoFile

    @Inject
    SceneVideoGenerator(File pngFile, double duration, File videoFile) {
        this.pngFile = pngFile
        this.duration = duration
        this.videoFile = videoFile
    }

    @Override
    void run() {
        def commandLine = ['ffmpeg', '-framerate', 1 / duration, '-i', pngFile, '-s', '1920x1200', '-vcodec', 'libx264', '-crf', 25, '-pix_fmt', 'yuv420p', '-r', 30, videoFile, '-y']
        println commandLine.join(" ")
        commandLine.execute().waitFor()
    }
}
