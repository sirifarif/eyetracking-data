import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.yaml.snakeyaml.*

class ConvertPraatLogToYaml extends DefaultTask {

    @InputFile
    File srcFile

    @OutputFile
    File destFile

    @TaskAction
    void convert() {
        def opts = new DumperOptions()
        opts.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        def yaml = new Yaml(opts)
        def frames = []
        def frameStr = ''
        srcFile.eachLine { line ->
            switch (line) {
                case ~/^Editor type:.+/:
                    if (frameStr) {
                        def map = yaml.load(frameStr)
                        frames << [window: [start: (map.'Window start' - 'seconds') as double,
                                            end  : (map.'Window end' - 'seconds') as double],
                                   date  : Date.parse('EEE MMM dd HH:mm:ss yyyy', map.Date, TimeZone.getTimeZone('Europe/Berlin'))]
                    }
                    frameStr = "$line\n"
                    break
                default:
                    frameStr += "$line\n"
                    break
            }
        }
        yaml.dump(frames, destFile.newWriter())
    }
}
