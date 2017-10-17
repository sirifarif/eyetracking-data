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
		//def dur = groovy.time.TimeCategory.minus(praatStart(praatlog), tobiiStart(tobiilog))
		//println "$dur.minutes $dur.seconds $dur.millis"
		//def durInSeconds = (dur.minutes*60) + dur.seconds + (dur.millis/1000)
		//println durInSeconds

		def offSet =  praatStart(praatlog).getTime() - tobiiStart(tobiilog).getTime()
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
