class Function(private val declaration: Expr.Function, private val closure: Environment) : Callable {
    override fun arity(): Int {
        return declaration.params.size
    }

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        val env = Environment(closure)

        for (i in 0..<declaration.params.size) {
            env.define(declaration.params[i].lexeme, arguments[i])
        }

        try {
            return interpreter.evaluateBlock(declaration.body, env)
        } catch(returnValue: Return) {
            return returnValue.value ?: GUnit()
        }
    }

    override fun toString(): String {
        return "<function ${declaration.name}>"
    }
}
