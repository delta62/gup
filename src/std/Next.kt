package std

import Callable
import GUnit
import Interpreter
import types.Type
import types.TypeSource

class Next : Callable {
    companion object {
        fun type(): Type {
            val returnType = Type.Any(TypeSource.Hardcoded)
            val params = listOf(Type.Any(TypeSource.Hardcoded))
            return Type.Function(TypeSource.Hardcoded, params, returnType)
        }
    }

    override fun arity(): Int {
        return 1
    }

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        val iterable = arguments[0] as Iterable<*>
        return iterable.next() ?: GUnit()
    }
}
