package types

import Token

class TypeEnv(private val parent: TypeEnv? = null) {
    private val values = HashMap<Token, Type>()

    operator fun get(expr: Token): Type? {
        for (entry in values) {
            if (expr.lexeme == entry.key.lexeme) return entry.value
        }

        if (parent != null) return parent[expr]
        return null
    }

    operator fun set(expr: Token, type: Type) {
        values[expr] = type
    }
}
