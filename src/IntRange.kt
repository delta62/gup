class IntRange(private val min: Long, private val max: Long) : Iterable<Long> {
    init {
        if (max <= min) throw RuntimeException("Max must be > min")
    }

    override fun iterator(): Iterator<Long> {
        return LongRange(min, max).iterator()
    }

    override fun toString(): String {
        return "$min..$max"
    }
}
