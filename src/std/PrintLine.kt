package std

import Callable
import Interpreter
import SamUnit

class PrintLine : Callable {
    override fun arity(): Int {
        return 1
    }

    override fun call(interpreter: Interpreter, arguments: List<Any>): Any {
        println(arguments[0])
        return SamUnit()
    }
}
