package std

import Callable
import Interpreter
import types.FunctionType
import types.GupUnit
import types.Type
import types.TypeSource

class AssertEqual : Callable {
    companion object : FunctionType {
        override fun name() = "assertEqual"

        override fun type(): Type.Function {
            val returnType = Type.Unit(TypeSource.Hardcoded)
            val params = listOf(Type.Any(TypeSource.Hardcoded), Type.Any(TypeSource.Hardcoded))
            return Type.Function(TypeSource.Hardcoded, params, returnType)
        }
    }

    override fun arity(): Int {
        return 2
    }

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        var expected = arguments[0]
        var actual = arguments[1]

        // TODO: Hack until there is a first-class equality system
        if (expected is Long && actual is ULong) {
            expected = expected.toLong()
            actual = actual.toLong()
        }
        if (expected is ULong && actual is Long) {
            expected = expected.toLong()
            actual = actual.toLong()
        }

        if (expected != actual) {
            val message = "Expected $actual to equal $expected"
            throw RuntimeException(message)
        }

        return GupUnit()
    }
}
