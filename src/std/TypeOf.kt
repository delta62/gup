package std

import Callable
import types.GupUnit
import Interpreter
import types.FunctionType
import types.Type
import types.TypeSource

class TypeOf : Callable {
    companion object : FunctionType {
        override fun name() = "typeof"

        override fun type(): Type.Function {
            val returnType = Type.String(TypeSource.Hardcoded)
            val params = listOf(Type.Any(TypeSource.Hardcoded))
            return Type.Function(TypeSource.Hardcoded, params, returnType)
        }
    }

    override fun arity(): Int {
        return 1
    }

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        return when (arguments[0]) {
            is Double -> "double"
            is Callable -> "function"
            is GupUnit -> "unit"
            is Boolean -> "bool"
            is String -> "string"
            is Long -> "int"
            is ULong -> "uint"
            else -> throw RuntimeException("Unexpected type $arguments[0]")
        }
    }
}
