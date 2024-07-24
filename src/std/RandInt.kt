package std

import Callable
import Interpreter
import types.FunctionType
import types.Type
import types.TypeSource
import Math
import kotlin.random.Random

class RandInt : Callable {
    companion object: FunctionType {
        override fun name() = "randInt"

        override fun type(): Type.Function {
            val returnType = Type.Long(TypeSource.Hardcoded)
            val params = listOf(Type.Long(TypeSource.Hardcoded), Type.Long(TypeSource.Hardcoded))
            return Type.Function(TypeSource.Hardcoded, params, returnType)
        }
    }

    override fun arity(): Int {
        return 2
    }

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        val min = Math.toLong(arguments[0])
        val max = Math.toLong(arguments[1])
        return Random.Default.nextLong(min, max)
    }
}
