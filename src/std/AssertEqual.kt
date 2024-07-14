package std

import Callable
import Interpreter
import GUnit
import types.Type
import types.TypeSource

class AssertEqual : Callable {
    companion object {
        fun type(): Type {
            val returnType = Type.Unit(TypeSource.Hardcoded)
            val params = listOf(Type.Any(TypeSource.Hardcoded), Type.Any(TypeSource.Hardcoded))
            return Type.Function(TypeSource.Hardcoded, params, returnType)
        }
    }

    override fun arity(): Int {
        return 2
    }

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        val expected = arguments[0]
        val actual = arguments[1]

        if (expected != actual) {
            val message = "Expected $actual to equal $expected"
            throw RuntimeException(message)
        }

        return GUnit()
    }
}
