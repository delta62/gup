import generated.Expr
import types.GupUnit

class Function(private val declaration: Expr.Function, private val closure: LexicalScope) : Callable {
    override fun arity(): Int {
        return declaration.params.size
    }

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        var env = closure

        for (i in declaration.params.indices) {
            env = env.define(declaration.params[i].identifier.lexeme, arguments[i])
        }

        return try {
            val body = Expr.Block(declaration.body)
            interpreter.evaluateWithEnv(body, env)
        } catch(returnValue: Return) {
            returnValue.value ?: GupUnit()
        }
    }

    override fun toString(): String {
        return "<fn ${declaration.name}(...${arity()} args)>"
    }
}
