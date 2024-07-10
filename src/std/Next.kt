package std

import Callable
import GUnit
import Interpreter

class Next : Callable {
    override fun arity(): Int {
        return 1
    }

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        val iterable = arguments[0] as Iterable<*>
        return iterable.next() ?: GUnit()
    }
}
