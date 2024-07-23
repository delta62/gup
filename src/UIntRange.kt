class UIntRange(private val min: ULong, private val max: ULong) : Iterable<ULong> {
    init {
        if (max <= min) throw RuntimeException("Max must be >= min")
    }

    override fun iterator(): Iterator<ULong> {
        return ULongRange(min, max).iterator()
    }

    override fun toString(): String {
        return "$min..$max"
    }
}
