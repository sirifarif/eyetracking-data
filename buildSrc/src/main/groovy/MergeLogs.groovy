import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.yaml.snakeyaml.*
import groovy.json.JsonBuilder

class MergeLogs extends DefaultTask {

	@InputFile
	File praatFile

	@InputFile
	File tobiiFile

	@OutputFile
	File destFile

	@TaskAction
	void convert() {
		def data = new groovy.json.JsonSlurper().parse( tobiiFile )
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
					start : scene.start as Date,
					end   : scene.end as Date,
					windowStart: scene.window.start as double,
					windowEnd: scene.window.end as double,
					gaze: []
			]
			result.each {
				sceneMap.gaze << [
						timeStamp: Date.parse(dateFormat, it.date),
						gazeType: it.value.gaze_type,
						gazeDur: it.value.gaze_duration as double,
						xPos: it.value.xPos as int,
						yPos: it.value.yPos as int
				]
			}
			sceneData << sceneMap
		}
		yaml.dump(sceneData, destFile.newWriter())
	}
}
