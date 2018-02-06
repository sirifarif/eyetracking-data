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
        def marXDiff = (mar.Xright as int) - (mar.Xleft as int)

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
                def ts = ''
                def fixationPosition = res.value.xPos as int
                if ((fixationPosition >= (mar.Xleft as int)) && (fixationPosition <= (mar.Xright as int))) {
                    fixationPosition = (fixationPosition as int) - (mar.Xleft as int)
                    ts = findSignalTime(scene.window, fixationPosition, marXDiff)
                }
                def gazeMap = [
                        vp        : project.name - 'vp',
                        timeStamp : Date.parse(dateFormat, res.date),
                        signalTime: ts,
                        gazeType  : res.value.gaze_type,
                        gazeDur   : res.value.gaze_duration as double,
                        gazeRegion: res.value.region,
                        position  : [xPos: res.value.xPos as int,
                                     yPos: res.value.yPos as int]
                ]

                if (res.value.region == 'spectrogram') {
                    gazeMap.subRegion = res.value.sub_region
                }
                sceneMap.gaze << gazeMap
            }
            sceneData << sceneMap
        }
        yaml.dump(sceneData, destFile.newWriter())
    }

    float findSignalTime(Object sceneWindow, Integer fixXPos, Integer marXDiff) {
        def sceneWindowDiff = sceneWindow.end - sceneWindow.start
        def res = sceneWindow.start + ((fixXPos * sceneWindowDiff) / marXDiff)
        assert ((res >= 0.0) && (res <= 46.663042))  //46.663042 maximum signal length
        return res
    }
}