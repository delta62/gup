package std

import Callable
import Interpreter
import GUnit

class AssertEqual : Callable {
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
