package std

import Callable
import types.GupUnit
import Interpreter
import types.FunctionType
import types.Type
import types.TypeSource

class Next : Callable {
    companion object : FunctionType {
        override fun name() = "next"

        override fun type(): Type.Function {
            val returnType = Type.Any(TypeSource.Hardcoded)
            val params = listOf(Type.Any(TypeSource.Hardcoded))
            return Type.Function(TypeSource.Hardcoded, params, returnType)
        }
    }

    override fun arity() = 1

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        val iterator = arguments[0] as Iterator<*>
        return if (iterator.hasNext()) iterator.next()!! else GupUnit()
    }
}
