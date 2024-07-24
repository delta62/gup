package std

import Callable
import Interpreter
import Math
import types.*

class Swap : Callable {
    companion object : FunctionType {
        override fun name() = "swap"

        override fun type(): Type.Function {
            val params = listOf(Type.Long(TypeSource.Hardcoded), Type.Long(TypeSource.Hardcoded), Type.Any(TypeSource.Hardcoded))
            val returnType = Type.Unit(TypeSource.Hardcoded)
            return Type.Function(TypeSource.Hardcoded, params, returnType)
        }
    }

    override fun arity() = 3

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        val i = Math.toLong(arguments[0]).toInt()
        val j = Math.toLong(arguments[1]).toInt()
        val list = arguments[2] as GupList

        list.swap(i, j)

        return GupUnit()
    }
}
