data class TypedIdentifier(val identifier: Token, val type: Token?) {
    override fun toString(): String {
        return if (type == null) {
            "$identifier"
        } else {
            "$identifier: $type"
        }
    }
}
