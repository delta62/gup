class GUnit {
    override fun toString(): String {
        return "unit"
    }

    override fun equals(other: Any?): Boolean {
        return other is GUnit
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
