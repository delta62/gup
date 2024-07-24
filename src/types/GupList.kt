package types

import generated.Expr
import java.util.Collections

class GupList(private val items: ArrayList<Any>) : Iterable<Any> {
    constructor() : this(ArrayList())

    fun swap(i: Int, j: Int) {
        Collections.swap(items, i, j)
    }

    override fun iterator() = items.iterator()

    fun add(value: Expr) {
        items.add(value)
    }

    override fun toString(): String {
        return "$items"
    }
}
