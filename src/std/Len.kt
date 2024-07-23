package std

import Callable
import Interpreter
import types.FunctionType
import types.Type
import types.TypeSource

class Len : Callable {
    companion object : FunctionType{
        override fun name() = "len"

        override fun type(): Type.Function {
            val returnType = Type.Long(TypeSource.Hardcoded)
            val params = listOf(Type.Any(TypeSource.Hardcoded))
            return Type.Function(TypeSource.Hardcoded, params, returnType)
        }
    }

    override fun arity() = 1

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        val iterable: Iterable<*> = arguments[0] as Iterable<*>
        return iterable.count().toULong()
    }
}
