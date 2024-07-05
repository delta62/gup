class Function(private val declaration: Expr.Function) : Callable {
    override fun arity(): Int {
        return declaration.params.size
    }

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        val env = Environment(interpreter.globals)

        for (i in 0..<declaration.params.size) {
            env.define(declaration.params[i].lexeme, arguments[i])
        }

        try {
            interpreter.executeBlock(declaration.body, env)
        } catch(returnValue: Return) {
            return returnValue.value ?: SamUnit()
        }

        return SamUnit()
    }

    override fun toString(): String {
        return "<function>"
    }
}
