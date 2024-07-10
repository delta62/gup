import error.RuntimeError

class Environment(private val enclosing: Environment? = null) {
    private val values = HashMap<String, Any?>()
    private val expressionCache = HashMap<Expr, Any>()

    fun define(name: String, value: Any? = null) {
        values[name] = value
    }

    fun defineExpr(expr: Expr, value: Any) {
        expressionCache[expr] = value
    }

    fun recallExpr(expr: Expr): Any? {
        if (expressionCache.containsKey(expr)) {
            return expressionCache[expr]
        }

        if (enclosing != null) {
            return enclosing.recallExpr(expr)
        }

        return null
    }

    private fun ancestor(distance: Int): Environment {
        var env = this
        for (i in 0..<distance) {
            env = env.enclosing!!
        }

        return env
    }

    fun getAt(distance: Int, name: String): Any? {
        return ancestor(distance).values[name]
    }

    fun assignAt(distance: Int, name: Token, value: Any) {
        ancestor(distance).values[name.lexeme] = value
    }

    fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) {
            return values[name.lexeme]
        }

        if (enclosing != null) {
            return enclosing.get(name)
        }

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'")
    }

    fun assign(name: Token, value: Any) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }

        if (enclosing != null) {
            enclosing.assign(name, value)
            return
        }

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'")
    }

    private fun depth(): Int {
        var depth = 0
        var ptr = enclosing
        while (ptr != null) {
            depth += 1
            ptr = ptr.enclosing
        }

        return depth
    }

    override fun toString(): String {
        return "[Environment depth=${depth()}] $values "
    }
}
