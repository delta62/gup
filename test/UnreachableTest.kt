import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class UnreachableTest {
    @Test
    fun isException() {
        assertThat(Unreachable()).isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun includesHelpfulMessage() {
        val exception = Unreachable()
        assertThat(exception).hasMessageContaining("unreachable")
    }
}
