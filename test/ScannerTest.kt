import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.io.path.name

class ScannerTest {
    private val examplePath = "examples"

    @TestFactory
    @DisplayName("Scans input")
    fun scansInput(): Stream<DynamicTest> {
        val path = Paths.get(examplePath)
        val files = Files.list(path)
        return files.map { p -> makeTest(p) }
    }

    private fun makeTest(path: Path): DynamicTest {
        return DynamicTest.dynamicTest(path.name) {
            val source = Files.readString(path)
            val scanner = Scanner(source)
            scanner.scan()
        }
    }
}
