package std

import Callable
import Interpreter
import types.FunctionType
import types.Type
import types.TypeSource

class Iterate : Callable {
    companion object : FunctionType {
        override fun name() = "iterate"

        override fun type(): Type.Function {
            val params = listOf(Type.Any(TypeSource.Hardcoded))
            val returnType = Type.Any(TypeSource.Hardcoded)
            return Type.Function(TypeSource.Hardcoded, params, returnType)
        }
    }

    override fun arity() = 1

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        val arg = arguments[0] as Iterable<*>
        return arg.iterator()
    }
}
