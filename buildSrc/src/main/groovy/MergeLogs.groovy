import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.yaml.snakeyaml.*

import java.time.*

class MergeLogs extends DefaultTask {

    @InputFile
    File praatFile

    @InputFile
    File tobiiFile

    @OutputFile
    File destFile

    @TaskAction
    void convert() {
        //for margins of praat screen
        def slurper = new groovy.json.JsonSlurper()
        def mar = slurper.parseText(project.margins)
        def marginXdiff = (mar.Xright as int) - (mar.Xleft as int)

        def data = new groovy.json.JsonSlurper().parse(tobiiFile)
        def opts = new DumperOptions()
        opts.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        def yaml = new Yaml(opts)
        def dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS"
        def sceneData = []
        new Yaml().load(praatFile.newReader()).each { scene ->
            def start = scene.start.getTime() as long
            def end = scene.end.getTime() as long
            def result = data.findAll {
                def dateInMilliseconds = Date.parse(dateFormat, it.date).getTime() as long
                dateInMilliseconds > start && dateInMilliseconds < end
            }
            def sceneMap = [
                    start      : scene.start as Date,
                    end        : scene.end as Date,
                    windowStart: scene.window.start as double,
                    windowEnd  : scene.window.end as double,
                    gaze       : []
            ]
            result.each { res ->
                sceneMap.gaze << [
                        timeStamp : Date.parse(dateFormat, res.date),
                        signalTime: findSignalTime(scene.window, res.value.xPos as int, marginXdiff),
                        gazeType  : res.value.gaze_type,
                        gazeDur   : res.value.gaze_duration as double,
                        gazeRegion: res.value.region,
                        subRegion : res.value.sub_region,
                        position  : [xPos: res.value.xPos as int,
                                     yPos: res.value.yPos as int]
                ]
            }
            sceneData << sceneMap
        }
        yaml.dump(sceneData, destFile.newWriter())
    }

    float findSignalTime(Object sceneWindow, Integer fixationXPosition, Integer marginXdiff) {
        def sceneWindowDiff = sceneWindow.end - sceneWindow.start
        return sceneWindow.start + ((fixationXPosition * sceneWindowDiff) / marginXdiff)
    }
}