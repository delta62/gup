package std

import Callable
import Interpreter
import types.FunctionType
import types.GupUnit
import types.Type
import types.TypeSource

class PrintLine : Callable {
    companion object : FunctionType {
        override fun name() = "println"

        override fun type(): Type.Function {
            val params = listOf(Type.Any(TypeSource.Hardcoded))
            val returnType = Type.Unit(TypeSource.Hardcoded)
            return Type.Function(TypeSource.Hardcoded, params, returnType)
        }
    }

    override fun arity(): Int {
        return 1
    }

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        println(arguments[0])
        return GupUnit()
    }
}
