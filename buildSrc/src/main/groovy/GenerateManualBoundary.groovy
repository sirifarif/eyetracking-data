import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class GenerateManualBoundary extends DefaultTask {

    @InputFile
    File srcFile

    @OutputFile
    File destFile

    @TaskAction
    void generate() {
        def boundary = []
        def cols
        srcFile.eachLine { line ->
            if (line.startsWith('1')) {
                if (line.split(' ')) {
                    cols = line.split(' ')
                    boundary.add(cols[1])
                }
            }
        }
        boundary.removeAt(1)
        boundary.add(cols[2])
        destFile.withWriter { dest ->
            for (int i = 1; i <= boundary.size() - 2; i++) {
                dest.write("${boundary[i]},")
            }
            dest.write(boundary[boundary.size - 1])
        }
    }
}