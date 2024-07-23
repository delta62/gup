package types

class GupUnit {
    override fun toString(): String {
        return "unit"
    }

    override fun equals(other: Any?): Boolean {
        return other is GupUnit
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
