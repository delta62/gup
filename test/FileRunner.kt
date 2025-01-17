import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class FileRunner(private val examplePath: Path) {
    fun runExample(name: String) {
        val path = Paths.get(examplePath.toString(), name)
        val bytes = Files.readAllBytes(path)
        val contents = String(bytes, Charset.defaultCharset())

        Gup.runSource(contents)
    }
}
