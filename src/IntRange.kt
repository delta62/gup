import std.Iterable

class IntRange(private var min: Long, private val max: Long) : Iterable<Long> {
    init {
        if (max <= min) throw RuntimeException("Max must be >= min")
    }

    override fun next(): Long? {
        val ret = min
        if (min >= max) return null
        min += 1
        return ret
    }
}
