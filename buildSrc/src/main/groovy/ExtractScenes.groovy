import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.yaml.snakeyaml.*

class ExtractScenes extends DefaultTask {

    @InputFile
    File srcFile

    @OutputFile
    File destFile

    @TaskAction
    void extract() {
        def scenes = []
        def opts = new DumperOptions()
        opts.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        def yaml = new Yaml(opts)
        def frameIter = yaml.load(srcFile.newReader()).iterator()
        def prevFrame = frameIter.next()
        def frame
        while (frameIter.hasNext()) {
            frame = frameIter.next()
            if (prevFrame.window != frame.window) {
                scenes << [
                        start : prevFrame.date,
                        end   : frame.date,
                        window: prevFrame.window
                ]
                prevFrame = frame
            }
        }
        scenes << [
                start : prevFrame.date,
                end   : frame.date,
                window: frame.window
        ]
        yaml.dump(scenes, destFile.newWriter())
    }

}
