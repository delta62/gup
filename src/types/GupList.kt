package types

import generated.Expr

class GupList(private val items: ArrayList<Expr>) : Iterable<Expr> {
    constructor() : this(ArrayList())

    override fun iterator() = items.iterator()

    fun add(value: Expr) {
        items.add(value)
    }
}
