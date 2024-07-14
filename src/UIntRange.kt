import std.Iterable

class UIntRange(private var min: ULong, private val max: ULong) : Iterable<ULong> {
    init {
        if (max <= min) throw RuntimeException("Max must be >= min")
    }

    override fun next(): ULong? {
        if (min >= max) return null
        return min++
    }
}
