import error.RuntimeError

class LexicalScope(private val parent: LexicalScope?, private val name: String, private var value: Any?) {
    companion object {
        fun fromEntries(values: Map<String, Any?>): LexicalScope {
            val seed: LexicalScope? = null
            val ret = values.entries.fold(seed) { acc, (k, v) ->  LexicalScope(acc, k, v) }
            if (ret == null) throw RuntimeException("Cannot initialize a lexical scope with nothing")
            return ret
        }
    }

    fun define(name: String, value: Any? = null): LexicalScope {
        return LexicalScope(this, name, value)
    }

    fun get(name: Token): Any? {
        if (name.lexeme == this.name) return value
        if (parent != null) return parent.get(name)
        undefined(name)
    }

    fun assign(name: Token, value: Any) {
        if (name.lexeme == this.name) this.value = value
        else if (parent != null) parent.assign(name, value)
        else undefined(name)
    }

    private fun undefined(name: Token): Nothing {
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'")
    }

    override fun toString(): String {
        return if (parent == null) name else "$name, $parent"
    }
}
