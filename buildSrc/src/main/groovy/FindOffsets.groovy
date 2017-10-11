import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import com.xlson.groovycsv.CsvParser

class FindOffsets extends DefaultTask {
	@InputFile
	praatlog

	@InputFile
	tobiilog
	@TaskAction
	void findOffsets() {
		def offSet =  (praatStart(praatlog).getTime() - tobiiStart(tobiilog).getTime()) / 1000
		println "offset is $offSet Seconds"
	}

	Date praatStart(File praatlog) {
		def dateFormat = "EEE MMM dd HH:mm:ss yyyy"
		def praatStartingPoint = (praatlog.readLines().get(2) - 'Date: ').trim()
		def yy = Date.parse(dateFormat, praatStartingPoint)
		println "praat $yy"
		return Date.parse(dateFormat, praatStartingPoint)
	}

	Date tobiiStart(File tobiilog) {
		def tsvReader = new FileReader(tobiilog)
		def data = CsvParser.parseCsv(['separator': '\t'], tsvReader)
		def recDate = data[0].RecordingDate
		def localTimeStamp = data[0].LocalTimeStamp
		def xx = Date.parse("dd.MM.yyyy HH:mm:ss.SSS", "$recDate $localTimeStamp")
		println "Tobii $xx"
		return Date.parse("dd.MM.yyyy HH:mm:ss.SSS", "$recDate $localTimeStamp")
	}
}
