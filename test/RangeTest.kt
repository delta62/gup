import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class RangeTest {
    @Test
    fun throwsWhenMaxGreaterThanMin() {
        assertThatThrownBy { Range(5, 0) }.hasMessageContaining("Max")
    }

    @Test
    fun throwsWhenMaxEqualsMin() {
        assertThatThrownBy { Range(3, 3) }.hasMessageContaining("Max")
    }

    @Test
    fun singleStepIteratorYieldsMin() {
        val range = Range(1, 2)
        assertThat(range.next()).isEqualTo(1.0)
        assertThat(range.next()).isNull()
    }
}
