import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class RangeTest {
    @Test
    fun throwsWhenMaxGreaterThanMin() {
        assertThatThrownBy { IntRange(5, 0) }.hasMessageContaining("Max")
    }

    @Test
    fun throwsWhenMaxEqualsMin() {
        assertThatThrownBy { IntRange(3, 3) }.hasMessageContaining("Max")
    }

    @Test
    fun singleStepIteratorYieldsMin() {
        val range = IntRange(1, 2)
        assertThat(range.next()).isEqualTo(1.0)
        assertThat(range.next()).isNull()
    }
}
