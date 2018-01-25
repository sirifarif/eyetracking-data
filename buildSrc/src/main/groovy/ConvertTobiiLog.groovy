import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import com.xlson.groovycsv.CsvParser
import groovy.json.JsonBuilder

class ConvertTobiiLog extends DefaultTask {

    @InputFile
    File srcFile

    @OutputFile
    File destFile

    @TaskAction
    void convert() {
        def slurper = new groovy.json.JsonSlurper()
        def mar = slurper.parseText(project.margins)
        def tsvReader = new FileReader(srcFile)
        def data = CsvParser.parseCsv(['separator': '\t'], tsvReader)
        def fixationWithData = []
        def prevXPosition = ""
        def prevYPosition = ""
        def dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS"
        data.each { row ->
            def xPosition = row.'FixationPointX (MCSpx)'
            def yPosition = row.'FixationPointY (MCSpx)'
            if (xPosition != prevXPosition || yPosition != prevYPosition) {
                if (xPosition) {
                    def subRegion = ''
                    def fixationRegion = findRegion(mar, xPosition as int, yPosition as int)
                    if (fixationRegion == 'spectrogram') {
                        subRegion = getSpectrogramRegion(mar, yPosition as int)
                    }
                    def date = Date.parse("dd.MM.yyyy HH:mm:ss.SSS", "$row.RecordingDate $row.LocalTimeStamp")
                    def gazeEvent = row.'GazeEventType'
                    def gazeDuration = row.'GazeEventDuration'
                    fixationWithData << [
                            date : date.format(dateFormat),
                            value: [
                                    gaze_type    : gazeEvent,
                                    gaze_duration: gazeDuration,
                                    xPos         : xPosition as int,
                                    yPos         : yPosition as int,
                                    region       : fixationRegion,
                                    sub_region : subRegion
                            ]]
                    prevXPosition = xPosition
                    prevYPosition = yPosition
                }
            }
        }
        destFile.text = new JsonBuilder(fixationWithData).toPrettyString()
    }

    String findRegion(mar, xPosition, yPosition) {
        def Xleft = mar.Xleft as int
        def Xright = mar.Xright as int
        def Ytop = mar.Ytop as int
        def YmiddleTop = mar.YmiddleTop as int
        def YmiddleBottom = mar.YmiddleBottom as int
        def Ybottom = mar.Ybottom as int

        if ((xPosition < Xleft || xPosition > Xright) || (yPosition < Ytop || yPosition > Ybottom)) {
            return 'other'
        } else {
            if ((yPosition >= Ytop) && (yPosition <= YmiddleTop)) {
                return 'waveform'
            }
            if ((yPosition > YmiddleTop) && (yPosition <= YmiddleBottom)) {
                return 'spectrogram'
            }
            if ((yPosition > YmiddleBottom) && (yPosition <= Ybottom)) {
                return 'Annotation'
            }
        }
    }

    String getSpectrogramRegion(mar, yPosition) {
        def top = mar.YmiddleTop as int
        def bottom = mar.YmiddleBottom as int
        def hightOfSpectrogram = bottom - top
        def lower = bottom - (hightOfSpectrogram * 10) / 100.0 as int
        def middle = lower - (hightOfSpectrogram * 60) / 100.0 as int

        if ((yPosition > top) && (yPosition <= middle)) {
            return 'top-sub-band'
        }
        if ((yPosition > middle) && (yPosition <= lower)) {
            return 'middle-sub-band'
        }
        if ((yPosition > lower) && (yPosition <= bottom)) {
            return 'lower-sub-band'
        }
    }
}
