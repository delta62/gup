package std

import Callable
import Interpreter
import types.FunctionType
import types.Type
import types.TypeSource

class ReadLine : Callable {
    companion object : FunctionType {
        override fun name() = "readln"

        override fun type(): Type.Function {
            val returnType = Type.String(TypeSource.Hardcoded)
            return Type.Function(TypeSource.Hardcoded, emptyList(), returnType)
        }
    }

    override fun arity() = 0

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        return readln()
    }
}
