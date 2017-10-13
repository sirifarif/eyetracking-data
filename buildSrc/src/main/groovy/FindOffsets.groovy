import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import com.xlson.groovycsv.CsvParser

class FindOffsets extends DefaultTask {
    @InputFile
    File praatlog

    @InputFile
    File tobiilog

    @TaskAction
    void findOffsets() {
        def offSet = praatStart(praatlog).time - tobiiStart(tobiilog).time
        project.file("gradle.properties").text = "offset=$offSet"
        println offSet
    }

    Date praatStart(File praatlog) {
        def dateFormat = "EEE MMM dd HH:mm:ss yyyy"
        def praatStartingPoint = (praatlog.readLines().get(2) - 'Date: ').trim()
        return Date.parse(dateFormat, praatStartingPoint)
    }

    Date tobiiStart(File tobiilog) {
        def tsvReader = new FileReader(tobiilog)
        def data = CsvParser.parseCsv(['separator': '\t'], tsvReader)
        def recDate = data[0].RecordingDate
        def localTimeStamp = data[0].LocalTimeStamp
        return Date.parse("dd.MM.yyyy HH:mm:ss.SSS", "$recDate $localTimeStamp")
    }
}
