import std.Iterable

class Range(private var min: Int, private val max: Int) : Iterable<Double> {
    init {
        if (max <= min) throw RuntimeException("Max must be >= min")
    }

    override fun next(): Double? {
        if (min >= max) return null
        return min++.toDouble()
    }
}
